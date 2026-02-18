-- =========================================
-- V1: 초기 스키마 (PostgreSQL)
-- 업데이트 날짜 2026/02/18
-- space rental platform MVP
-- =========================================


-- =========================================
-- 0) Extensions
-- =========================================
CREATE EXTENSION IF NOT EXISTS btree_gist;


-- =========================================
-- 1) ENUM types
-- =========================================
DO $$
BEGIN
	IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'operator_approval_status') THEN
    CREATE TYPE operator_approval_status AS ENUM ('PENDING', 'APPROVED');
  END IF;

  IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'account_status') THEN
    CREATE TYPE account_status AS ENUM ('ACTIVE', 'SUSPENDED', 'DELETED');
  END IF;

  IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'user_role') THEN
    CREATE TYPE user_role AS ENUM ('CUSTOMER', 'OPERATOR', 'PLATFORM_ADMIN');
  END IF;

  IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'room_status') THEN
    CREATE TYPE room_status AS ENUM ('AVAILABLE', 'INACTIVE');
  END IF;

  IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'reservation_status') THEN
    CREATE TYPE reservation_status AS ENUM ('PENDING', 'CONFIRMED', 'CANCELED');
  END IF;

  IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'log_action') THEN
    CREATE TYPE log_action AS ENUM ('CREATE', 'UPDATE', 'CANCEL');
  END IF;

  IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'login_type') THEN
    CREATE TYPE login_type AS ENUM ('LOCAL', 'NAVER');
  END IF;
END$$;


-- =========================================
-- 2) account
-- =========================================
CREATE TABLE IF NOT EXISTS account (
  id            BIGSERIAL PRIMARY KEY,
  email         VARCHAR(255) NOT NULL,
  password_hash TEXT,
  login_type    login_type NOT NULL DEFAULT 'LOCAL',
  oauth_id      VARCHAR(100),
  status        account_status NOT NULL DEFAULT 'ACTIVE',
  created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT uq_account_email UNIQUE (email)
);


-- =========================================
-- 3) app_user
-- =========================================
CREATE TABLE IF NOT EXISTS app_user (
  id          BIGSERIAL PRIMARY KEY,
  account_id  BIGINT NOT NULL,
  name        VARCHAR(100) NOT NULL,
  role        user_role NOT NULL DEFAULT 'CUSTOMER',
  created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),

  CONSTRAINT uq_user_account UNIQUE (account_id),
  CONSTRAINT fk_user_account
    FOREIGN KEY (account_id)
    REFERENCES account(id)
    ON DELETE CASCADE
);


-- =========================================
-- 4) office
-- =========================================
CREATE TABLE IF NOT EXISTS office (
  id            BIGSERIAL PRIMARY KEY,
  owner_user_id BIGINT NOT NULL,
  name          VARCHAR(150) NOT NULL,
  location      VARCHAR(255) NOT NULL,
  latitude      DOUBLE PRECISION,
  longitude     DOUBLE PRECISION,
  open_time     TIME NOT NULL,
  close_time    TIME NOT NULL,
  open_days     SMALLINT[] NOT NULL DEFAULT ARRAY[1,2,3,4,5,6,7]::SMALLINT[],
  created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at    TIMESTAMPTZ NOT NULL DEFAULT now(),

  CONSTRAINT fk_office_owner_user
    FOREIGN KEY (owner_user_id)
    REFERENCES app_user(id)
    ON DELETE RESTRICT,

  CONSTRAINT ck_office_open_days_range CHECK (open_days <@ ARRAY[1,2,3,4,5,6,7]::SMALLINT[])
);


-- =========================================
-- 5) office_room
-- =========================================
CREATE TABLE IF NOT EXISTS office_room (
  id          BIGSERIAL PRIMARY KEY,
  office_id   BIGINT NOT NULL,
  room_code   VARCHAR(50) NOT NULL,
  name        VARCHAR(100) NOT NULL,
  floor       INTEGER,
  status      room_status NOT NULL DEFAULT 'AVAILABLE',
  capacity    INTEGER NOT NULL,
  category    VARCHAR(100),
  price       NUMERIC(10,2) NOT NULL DEFAULT 0,
  version     BIGINT NOT NULL DEFAULT 0,
  created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),

  CONSTRAINT fk_room_office
    FOREIGN KEY (office_id)
    REFERENCES office(id)
    ON DELETE RESTRICT,

  CONSTRAINT uq_room_office_roomcode UNIQUE (office_id, room_code),
  CONSTRAINT uq_office_room_id_office UNIQUE (id, office_id),
  CONSTRAINT ck_room_capacity_positive CHECK (capacity > 0),
  CONSTRAINT ck_room_price_non_negative CHECK (price >= 0)
);


-- =========================================
-- 6) facility
-- =========================================
CREATE TABLE IF NOT EXISTS facility (
  id         BIGSERIAL PRIMARY KEY,
  name       VARCHAR(50) NOT NULL UNIQUE,
  label      VARCHAR(100) NOT NULL,
  is_active  BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);


-- =========================================
-- 7) office_room_facility
-- =========================================
CREATE TABLE IF NOT EXISTS office_room_facility (
  room_id     BIGINT NOT NULL,
  facility_id BIGINT NOT NULL,
  created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),

  PRIMARY KEY (room_id, facility_id),

  CONSTRAINT fk_orf_room
    FOREIGN KEY (room_id)
    REFERENCES office_room(id)
    ON DELETE CASCADE,

  CONSTRAINT fk_orf_facility
    FOREIGN KEY (facility_id)
    REFERENCES facility(id)
    ON DELETE RESTRICT
);


-- =========================================
-- 8) reservation
-- =========================================
CREATE TABLE IF NOT EXISTS reservation (
  id           BIGSERIAL PRIMARY KEY,
  title        VARCHAR(200),
  office_id    BIGINT NOT NULL,
  room_id      BIGINT NOT NULL,
  customer_id  BIGINT NOT NULL,
  start_at     TIMESTAMPTZ NOT NULL,
  end_at       TIMESTAMPTZ NOT NULL,
  version      BIGINT NOT NULL DEFAULT 0,
  status       reservation_status NOT NULL DEFAULT 'PENDING',
  created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at   TIMESTAMPTZ NOT NULL DEFAULT now(),

  CONSTRAINT fk_reservation_office
    FOREIGN KEY (office_id)
    REFERENCES office(id)
    ON DELETE RESTRICT,

  CONSTRAINT fk_reservation_room_office_pair
    FOREIGN KEY (room_id, office_id)
    REFERENCES office_room(id, office_id)
    ON DELETE RESTRICT,

  CONSTRAINT fk_reservation_customer
    FOREIGN KEY (customer_id)
    REFERENCES app_user(id)
    ON DELETE RESTRICT,

  CONSTRAINT ck_reservation_time CHECK (start_at < end_at)
);


-- 중복 예약 방지 (EXCLUDE)
DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_constraint WHERE conname = 'ex_reservation_no_overlap'
  ) THEN
    ALTER TABLE reservation
    ADD CONSTRAINT ex_reservation_no_overlap
    EXCLUDE USING gist (
      room_id WITH =,
      tstzrange(start_at, end_at, '[)') WITH &&
    )
    WHERE (status IN ('PENDING', 'CONFIRMED'));
  END IF;
END$$;


-- =========================================
-- 9) review
-- =========================================
CREATE TABLE IF NOT EXISTS review (
  id             BIGSERIAL PRIMARY KEY,
  reservation_id BIGINT NOT NULL,
  author_user_id BIGINT NOT NULL,
  rating         SMALLINT NOT NULL,
  content        TEXT NOT NULL,
  created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at     TIMESTAMPTZ NOT NULL DEFAULT now(),

  CONSTRAINT uq_review_reservation UNIQUE (reservation_id),

  CONSTRAINT fk_review_reservation
    FOREIGN KEY (reservation_id)
    REFERENCES reservation(id)
    ON DELETE RESTRICT,

  CONSTRAINT fk_review_author_user
    FOREIGN KEY (author_user_id)
    REFERENCES app_user(id)
    ON DELETE RESTRICT,

  CONSTRAINT ck_review_rating_range CHECK (rating BETWEEN 1 AND 5)
);


-- =========================================
-- 10) update_log
-- =========================================
CREATE TABLE IF NOT EXISTS update_log (
  id             BIGSERIAL PRIMARY KEY,
  reservation_id BIGINT NOT NULL,
  action         log_action NOT NULL,
  actor_user_id  BIGINT NOT NULL,
  occurred_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
  before_data    JSONB,
  after_data     JSONB,

  CONSTRAINT fk_log_reservation
    FOREIGN KEY (reservation_id)
    REFERENCES reservation(id)
    ON DELETE RESTRICT,

  CONSTRAINT fk_log_actor_user
    FOREIGN KEY (actor_user_id)
    REFERENCES app_user(id)
    ON DELETE RESTRICT
);


-- =========================================
-- 11) refresh_token
-- =========================================
CREATE TABLE IF NOT EXISTS refresh_token (
  id          BIGSERIAL PRIMARY KEY,
  token       TEXT NOT NULL UNIQUE,
  account_id  BIGINT NOT NULL UNIQUE,
  expiry_date TIMESTAMPTZ NOT NULL,
  created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT fk_refresh_token_account
    FOREIGN KEY (account_id)
    REFERENCES account(id)
    ON DELETE CASCADE
);


-- =========================================
-- 12) room_favorite
-- =========================================
CREATE TABLE IF NOT EXISTS room_favorite (
  id         BIGSERIAL PRIMARY KEY,
  user_id    BIGINT NOT NULL,
  room_id    BIGINT NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),

  CONSTRAINT uq_room_favorite_user_room UNIQUE (user_id, room_id),

  CONSTRAINT fk_room_favorite_user
    FOREIGN KEY (user_id)
    REFERENCES app_user(id)
    ON DELETE CASCADE,

  CONSTRAINT fk_room_favorite_room
    FOREIGN KEY (room_id)
    REFERENCES office_room(id)
    ON DELETE CASCADE
);


-- =========================================
-- Indexes
-- =========================================
CREATE INDEX IF NOT EXISTS ix_office_owner_user_id
  ON office(owner_user_id);

CREATE INDEX IF NOT EXISTS ix_office_open_days
  ON office USING gin (open_days);

CREATE INDEX IF NOT EXISTS ix_office_room_office_id
  ON office_room(office_id);

CREATE INDEX IF NOT EXISTS ix_office_room_price
  ON office_room(price);

CREATE INDEX IF NOT EXISTS ix_office_room_office_price
  ON office_room(office_id, price);

CREATE INDEX IF NOT EXISTS ix_orf_facility_room
  ON office_room_facility(facility_id, room_id);

CREATE INDEX IF NOT EXISTS ix_reservation_room_start
  ON reservation(room_id, start_at);

CREATE INDEX IF NOT EXISTS ix_reservation_customer_start
  ON reservation(customer_id, start_at);

CREATE INDEX IF NOT EXISTS ix_reservation_office_start
  ON reservation(office_id, start_at);

CREATE INDEX IF NOT EXISTS ix_reservation_room_office
  ON reservation(room_id, office_id);

CREATE INDEX IF NOT EXISTS ix_reservation_room_timerange_active
  ON reservation USING gist (
    room_id,
    tstzrange(start_at, end_at, '[)')
  )
  WHERE status IN ('PENDING', 'CONFIRMED');

CREATE INDEX IF NOT EXISTS ix_review_author_time
  ON review(author_user_id, created_at DESC);

CREATE INDEX IF NOT EXISTS ix_update_log_reservation_time
  ON update_log(reservation_id, occurred_at DESC);

CREATE INDEX IF NOT EXISTS ix_update_log_actor_time
  ON update_log(actor_user_id, occurred_at DESC);

CREATE INDEX IF NOT EXISTS ix_refresh_token_account
  ON refresh_token(account_id);

CREATE INDEX IF NOT EXISTS ix_room_favorite_user_time
  ON room_favorite(user_id, created_at DESC);

CREATE INDEX IF NOT EXISTS ix_room_favorite_room
  ON room_favorite(room_id);


-- =========================================
-- updated_at trigger
-- =========================================
CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
  NEW.updated_at = now();
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;


DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_trigger WHERE tgname = 'tr_account_updated_at') THEN
    CREATE TRIGGER tr_account_updated_at
    BEFORE UPDATE ON account
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();
  END IF;

  IF NOT EXISTS (SELECT 1 FROM pg_trigger WHERE tgname = 'tr_app_user_updated_at') THEN
    CREATE TRIGGER tr_app_user_updated_at
    BEFORE UPDATE ON app_user
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();
  END IF;

  IF NOT EXISTS (SELECT 1 FROM pg_trigger WHERE tgname = 'tr_office_updated_at') THEN
    CREATE TRIGGER tr_office_updated_at
    BEFORE UPDATE ON office
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();
  END IF;

  IF NOT EXISTS (SELECT 1 FROM pg_trigger WHERE tgname = 'tr_office_room_updated_at') THEN
    CREATE TRIGGER tr_office_room_updated_at
    BEFORE UPDATE ON office_room
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();
  END IF;

  IF NOT EXISTS (SELECT 1 FROM pg_trigger WHERE tgname = 'tr_facility_updated_at') THEN
    CREATE TRIGGER tr_facility_updated_at
    BEFORE UPDATE ON facility
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();
  END IF;

  IF NOT EXISTS (SELECT 1 FROM pg_trigger WHERE tgname = 'tr_reservation_updated_at') THEN
    CREATE TRIGGER tr_reservation_updated_at
    BEFORE UPDATE ON reservation
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();
  END IF;

  IF NOT EXISTS (SELECT 1 FROM pg_trigger WHERE tgname = 'tr_review_updated_at') THEN
    CREATE TRIGGER tr_review_updated_at
    BEFORE UPDATE ON review
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();
  END IF;
END$$;
