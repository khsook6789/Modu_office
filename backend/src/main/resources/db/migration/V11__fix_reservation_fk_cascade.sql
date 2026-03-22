-- V11: payment, review, facility_report의 reservation FK를 ON DELETE CASCADE로 변경
-- reservation 삭제 시 연결된 레코드들이 자동으로 함께 삭제됩니다.

-- payment
ALTER TABLE payment
    DROP CONSTRAINT fk_payment_reservation;

ALTER TABLE payment
    ADD CONSTRAINT fk_payment_reservation
        FOREIGN KEY (reservation_id)
        REFERENCES reservation(id)
        ON DELETE CASCADE;

-- review
ALTER TABLE review
    DROP CONSTRAINT fk_review_reservation;

ALTER TABLE review
    ADD CONSTRAINT fk_review_reservation
        FOREIGN KEY (reservation_id)
        REFERENCES reservation(id)
        ON DELETE CASCADE;

-- facility_report
ALTER TABLE facility_report
    DROP CONSTRAINT fk_report_reservation;

ALTER TABLE facility_report
    ADD CONSTRAINT fk_report_reservation
        FOREIGN KEY (reservation_id)
        REFERENCES reservation(id)
        ON DELETE CASCADE;
