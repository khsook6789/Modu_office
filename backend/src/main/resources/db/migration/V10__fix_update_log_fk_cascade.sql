-- V10: update_log.fk_log_reservation FK를 ON DELETE RESTRICT → ON DELETE CASCADE로 변경
-- reservation 삭제 시 연결된 update_log 레코드가 자동으로 함께 삭제됩니다.

ALTER TABLE update_log
    DROP CONSTRAINT fk_log_reservation;

ALTER TABLE update_log
    ADD CONSTRAINT fk_log_reservation
        FOREIGN KEY (reservation_id)
        REFERENCES reservation(id)
        ON DELETE CASCADE;
