-- =========================================
-- V2: app_user 테이블에 approval_status 컬럼 추가, 기존에 approved면 그대로 유지
-- 업데이트 날짜 2026/02/18
-- =========================================

ALTER TABLE app_user
    ADD COLUMN IF NOT EXISTS approval_status operator_approval_status;

UPDATE app_user
    SET approval_status = 'APPROVED'
    WHERE role = 'OPERATOR' AND approval_status IS NULL;
