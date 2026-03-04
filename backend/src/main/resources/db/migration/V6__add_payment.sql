-- =========================================
-- V6: payment 테이블 추가
-- 업데이트 날짜 2026/03/04
-- Toss Payments 연동용 결제 내역 관리
-- =========================================

-- ENUM type removed for H2 compatibility; using VARCHAR(20) with CHECK or application-level ENUM mapped to VARCHAR instead.

CREATE TABLE IF NOT EXISTS payment (
  id             BIGSERIAL PRIMARY KEY,
  reservation_id BIGINT NOT NULL UNIQUE,
  order_id       VARCHAR(64) NOT NULL UNIQUE,
  payment_key    VARCHAR(200),
  amount         BIGINT NOT NULL,
  status         VARCHAR(20) NOT NULL DEFAULT 'READY',
  method         VARCHAR(50),
  approved_at    TIMESTAMPTZ,
  canceled_at    TIMESTAMPTZ,
  fail_reason    TEXT,
  created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at     TIMESTAMPTZ NOT NULL DEFAULT now(),

  CONSTRAINT fk_payment_reservation
    FOREIGN KEY (reservation_id)
    REFERENCES reservation(id)
    ON DELETE RESTRICT
);

CREATE INDEX IF NOT EXISTS ix_payment_reservation_id ON payment(reservation_id);
CREATE INDEX IF NOT EXISTS ix_payment_order_id ON payment(order_id);

DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_trigger WHERE tgname = 'tr_payment_updated_at') THEN
    CREATE TRIGGER tr_payment_updated_at
    BEFORE UPDATE ON payment
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();
  END IF;
END$$;
