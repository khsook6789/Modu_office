-- 결제 대기 상태(PENDING_PAYMENT)인 예약들의 빠른 조회를 위한 복합 인덱스 생성
CREATE INDEX IF NOT EXISTS ix_reservation_status_created_at
ON reservation (status, created_at);