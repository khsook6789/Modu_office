-- =========================================
-- V5: DB 컬럼, 테이블명 리팩토링
-- =========================================

-- 1) user_role Enum 값 변경
-- 참고: PostgreSQL에서 enum value를 변경하려면 10.0 이상부터 RENAME VALUE 지원
ALTER TYPE user_role RENAME VALUE 'CUSTOMER' TO 'USER';
ALTER TYPE user_role RENAME VALUE 'OPERATOR' TO 'MANAGER';
ALTER TYPE user_role RENAME VALUE 'PLATFORM_ADMIN' TO 'ADMIN';

-- 2) facility 테이블 컬럼명 변경
ALTER TABLE facility RENAME COLUMN name TO facility_code;
ALTER TABLE facility RENAME COLUMN label TO facility_name;

-- 3) office_room 관련 테이블 및 제약조건/인덱스 명 변경
-- 3-1) office_room -> room 테이블명 변경
ALTER TABLE office_room RENAME TO room;

-- 기존 제약조건 이름 변경
ALTER TABLE room RENAME CONSTRAINT fk_room_office TO fk_room_office_id;
-- uq_room_office_roomcode -> uq_room_office_roomcode 유지해도 무방하나, 깔끔하게
ALTER TABLE room RENAME CONSTRAINT uq_room_office_roomcode TO uq_room_office_room_code;
ALTER TABLE room RENAME CONSTRAINT uq_office_room_id_office TO uq_room_id_office;

-- 기존 인덱스 이름 변경
ALTER INDEX ix_office_room_office_id RENAME TO ix_room_office_id;
ALTER INDEX ix_office_room_price RENAME TO ix_room_price;
ALTER INDEX ix_office_room_office_price RENAME TO ix_room_office_price;

-- 3-2) office_room_facility -> room_facility 테이블명 변경
ALTER TABLE office_room_facility RENAME TO room_facility;

-- 제약조건 이름 변경 (fk_orf_...)
ALTER TABLE room_facility RENAME CONSTRAINT fk_orf_room TO fk_room_facility_room;
ALTER TABLE room_facility RENAME CONSTRAINT fk_orf_facility TO fk_room_facility_facility;
ALTER INDEX ix_orf_facility_room RENAME TO ix_room_facility_id_room;

-- 트리거 함수 이름 변경 대응 (V1에서 생성한 것)
ALTER TRIGGER tr_office_room_updated_at ON room RENAME TO tr_room_updated_at;
