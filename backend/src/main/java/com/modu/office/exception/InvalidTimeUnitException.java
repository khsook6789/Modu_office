package com.modu.office.exception;

/**
 * 예약 시간 단위가 30분 규격에 맞지 않을 때 발생하는 예외
 * Why: 자투리 시간(10분, 15분 단위) 예약을 차단하여 회의실 운영 효율(Utilization)을 최대화.
 */
public class InvalidTimeUnitException extends RuntimeException {

    public InvalidTimeUnitException(String message) {
        super(message);
    }
}
