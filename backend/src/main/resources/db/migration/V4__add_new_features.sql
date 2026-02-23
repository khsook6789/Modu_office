-- =========================================
-- V4: 지점/회의실 상세 설명, 이미지, 정비시간, 리포트, 알림, 취소정책 추가
-- =========================================

-- 1) office 테이블 확장
ALTER TABLE office
ADD COLUMN description TEXT;

-- 2) office_room 테이블 확장
ALTER TABLE office_room
ADD COLUMN description TEXT,
ADD COLUMN banner_image_url VARCHAR(255),
ADD COLUMN buffer_time INTEGER NOT NULL DEFAULT 0;

-- 3) room_image 테이블 생성
CREATE TABLE IF NOT EXISTS room_image (
  id            BIGSERIAL PRIMARY KEY,
  room_id       BIGINT NOT NULL,
  image_url     VARCHAR(255) NOT NULL,
  display_order INTEGER NOT NULL DEFAULT 0,
  created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),

  CONSTRAINT fk_room_image_room
    FOREIGN KEY (room_id)
    REFERENCES office_room(id)
    ON DELETE CASCADE
);

CREATE INDEX ix_room_image_room_id ON room_image(room_id);

-- 4) reservation 테이블 제약조건 수정 및 컬럼 추가
-- end_at_include_buffer_time 추가
ALTER TABLE reservation
ADD COLUMN end_at_include_buffer_time TIMESTAMPTZ;

-- 기존 데이터 마이그레이션: 기존 예약들은 buffer_time이 0이므로 end_at과 동일하게 세팅
UPDATE reservation SET end_at_include_buffer_time = end_at WHERE end_at_include_buffer_time IS NULL;

-- NOT NULL 제약조건 추가
ALTER TABLE reservation ALTER COLUMN end_at_include_buffer_time SET NOT NULL;

-- 중복 예약 방지 제약조건 수정
ALTER TABLE reservation DROP CONSTRAINT IF EXISTS ex_reservation_no_overlap;

ALTER TABLE reservation
ADD CONSTRAINT ex_reservation_no_overlap
EXCLUDE USING gist (
  room_id WITH =,
  tstzrange(start_at, end_at_include_buffer_time, '[)') WITH &&
)
WHERE (status IN ('PENDING', 'CONFIRMED'));


-- 5) facility_report 테이블 및 Enum 생성
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'report_issue_type') THEN
    CREATE TYPE report_issue_type AS ENUM ('BROKEN', 'MISSING', 'OTHER');
  END IF;
  
  IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'report_status') THEN
    CREATE TYPE report_status AS ENUM ('REPORTED', 'IN_PROGRESS', 'RESOLVED');
  END IF;
END$$;

CREATE TABLE IF NOT EXISTS facility_report (
  id             BIGSERIAL PRIMARY KEY,
  reservation_id BIGINT NOT NULL,
  room_id        BIGINT NOT NULL,
  facility_id    BIGINT NOT NULL,
  issue_type     report_issue_type NOT NULL,
  status         report_status NOT NULL DEFAULT 'REPORTED',
  created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at     TIMESTAMPTZ NOT NULL DEFAULT now(),

  CONSTRAINT fk_report_reservation
    FOREIGN KEY (reservation_id)
    REFERENCES reservation(id)
    ON DELETE RESTRICT,
    
  CONSTRAINT fk_report_room
    FOREIGN KEY (room_id)
    REFERENCES office_room(id)
    ON DELETE RESTRICT,
    
  CONSTRAINT fk_report_facility
    FOREIGN KEY (facility_id)
    REFERENCES facility(id)
    ON DELETE RESTRICT
);

CREATE INDEX ix_report_reservation_id ON facility_report(reservation_id);
CREATE INDEX ix_report_room_id ON facility_report(room_id);

DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_trigger WHERE tgname = 'tr_facility_report_updated_at') THEN
    CREATE TRIGGER tr_facility_report_updated_at
    BEFORE UPDATE ON facility_report
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();
  END IF;
END$$;


-- 6) notification 테이블 생성
CREATE TABLE IF NOT EXISTS notification (
  id         BIGSERIAL PRIMARY KEY,
  user_id    BIGINT NOT NULL,
  content    TEXT NOT NULL,
  is_read    BOOLEAN NOT NULL DEFAULT FALSE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),

  CONSTRAINT fk_notification_user
    FOREIGN KEY (user_id)
    REFERENCES app_user(id)
    ON DELETE CASCADE
);

CREATE INDEX ix_notification_user_id ON notification(user_id);


-- 7) cancellation_policy 테이블 생성
CREATE TABLE IF NOT EXISTS cancellation_policy (
  id          BIGSERIAL PRIMARY KEY,
  office_id   BIGINT NOT NULL,
  days_before INTEGER NOT NULL,
  refund_rate INTEGER NOT NULL,
  created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),

  CONSTRAINT fk_cancellation_policy_office
    FOREIGN KEY (office_id)
    REFERENCES office(id)
    ON DELETE CASCADE,
    
  CONSTRAINT ck_cancellation_policy_refund_rate CHECK (refund_rate BETWEEN 0 AND 100),
  CONSTRAINT ck_cancellation_policy_days_before CHECK (days_before >= 0),
  CONSTRAINT uq_cancellation_policy_office_days UNIQUE (office_id, days_before)
);

DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_trigger WHERE tgname = 'tr_cancellation_policy_updated_at') THEN
    CREATE TRIGGER tr_cancellation_policy_updated_at
    BEFORE UPDATE ON cancellation_policy
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();
  END IF;
END$$;
