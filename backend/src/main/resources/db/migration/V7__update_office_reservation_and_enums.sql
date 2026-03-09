-- 1. office 테이블 owner_user_id -> manager_id로 변경
ALTER TABLE office RENAME COLUMN owner_user_id TO manager_id;

-- 2. reservation 테이블 customer_id -> user_id로 변경
ALTER TABLE reservation RENAME COLUMN customer_id TO user_id;

-- 3. 신고 유형(Issue Type) 확장: 오작동, 소모품부족, 청결불량 항목 추가
ALTER TYPE report_issue_type ADD VALUE 'MALFUNCTION';
ALTER TYPE report_issue_type ADD VALUE 'NEEDS_SUPPLIES';
ALTER TYPE report_issue_type ADD VALUE 'DIRTY';

-- 4. 신고 상태(Status) 확장: 사용자 철회 항목 추가
ALTER TYPE report_status ADD VALUE 'CANCELED';
