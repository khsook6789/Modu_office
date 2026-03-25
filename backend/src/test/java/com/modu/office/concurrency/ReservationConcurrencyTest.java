package com.modu.office.concurrency;

import com.modu.office.dto.request.ReservationRequest;
import com.modu.office.entity.*;
import com.modu.office.entity.enums.*;
import com.modu.office.repository.*;
import com.modu.office.service.ReservationService;
import com.modu.office.support.IntegrationTestSupport;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 예약 동시성 방어 검증 테스트
 *
 * <p>시스템의 2중 방어 구조를 검증합니다:
 * <ul>
 *   <li>1층: 애플리케이션 검증 (findConflictingReservationsWithOptimisticLock)</li>
 *   <li>2층: PostgreSQL EXCLUDE 제약조건 (ex_reservation_no_overlap) - 최종 방어선</li>
 *   <li>3층: @Lock(LockModeType.OPTIMISTIC) - UPDATE 충돌 시 낙관적 잠금 (CONFIRM 액션)</li>
 * </ul>
 *
 * <p>스레드 수는 HikariCP 커넥션 풀(기본 10~16) 이하로 유지하여
 * 자원 부족에 의한 가짜 실패(Flaky Test)를 방지합니다.
 */
@SuppressWarnings("null")
class ReservationConcurrencyTest extends IntegrationTestSupport {

    @Autowired
    private ReservationService reservationService;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private OfficeRepository officeRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private UpdateLogRepository updateLogRepository;

    private AppUser user;
    private AppUser manager;
    private Office office;
    private Room room;

    @BeforeEach
    void setUp() {
        updateLogRepository.deleteAll();
        reservationRepository.deleteAll();
        roomRepository.deleteAll();
        officeRepository.deleteAll();
        appUserRepository.deleteAll();
        accountRepository.deleteAll();

        Account userAccount = Account.builder()
                .email("concurrency-user@test.com")
                .passwordHash("hashed-password")
                .loginType(LoginType.LOCAL)
                .build();
        accountRepository.save(userAccount);

        user = AppUser.builder()
                .account(userAccount)
                .name("Concurrency User")
                .role(UserRole.USER)
                .build();
        appUserRepository.save(user);

        Account managerAccount = Account.builder()
                .email("concurrency-manager@test.com")
                .passwordHash("hashed-password")
                .loginType(LoginType.LOCAL)
                .build();
        accountRepository.save(managerAccount);

        manager = AppUser.builder()
                .account(managerAccount)
                .name("Concurrency Manager")
                .role(UserRole.MANAGER)
                .approvalStatus(ManagerApprovalStatus.APPROVED)
                .build();
        appUserRepository.save(manager);

        office = Office.builder()
                .manager(manager)
                .name("Concurrency Test Office")
                .location("Seoul, Korea")
                .latitude(37.5665)
                .longitude(126.9780)
                .openTime(LocalTime.of(9, 0))
                .closeTime(LocalTime.of(18, 0))
                .openDays(new Short[]{1, 2, 3, 4, 5})
                .build();
        officeRepository.save(office);

        room = Room.builder()
                .office(office)
                .name("Concurrency Room")
                .roomCode("CONC-01")
                .capacity(10)
                .floor(1)
                .build();
        roomRepository.save(room);
    }

    @AfterEach
    void tearDown() {
        updateLogRepository.deleteAll();
        reservationRepository.deleteAll();
        roomRepository.deleteAll();
        officeRepository.deleteAll();
        appUserRepository.deleteAll();
        accountRepository.deleteAll();
    }

    /**
     * 동일 시간대 동시 예약 생성 시 1건만 성공하는지 검증합니다.
     *
     * <p>검증 대상: PostgreSQL EXCLUDE 제약조건 (ex_reservation_no_overlap)
     * <p>시나리오: 3개 스레드가 동일 회의실, 동일 시간대에 동시 예약 시도
     * <p>기대 결과: 정확히 1건만 INSERT 성공, 나머지는 예외 발생
     */
    @RepeatedTest(3)
    @DisplayName("동일 시간대 동시 예약 생성 시 1건만 성공한다")
    void should_allowOnlyOneReservation_whenConcurrentInsert() throws InterruptedException {
        int threadCount = 3;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch readyLatch = new CountDownLatch(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);
        List<Exception> exceptions = Collections.synchronizedList(new ArrayList<>());

        LocalDateTime startAt = LocalDateTime.now()
                .plusDays(1).withHour(10).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime endAt = LocalDateTime.now()
                .plusDays(1).withHour(11).withMinute(0).withSecond(0).withNano(0);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    readyLatch.countDown();
                    startLatch.await();

                    ReservationRequest request = ReservationRequest.builder()
                            .title("동시성 테스트 예약")
                            .officeId(office.getId())
                            .roomId(room.getId())
                            .userId(user.getId())
                            .startAt(startAt)
                            .endAt(endAt)
                            .build();
                    reservationService.createReservation(request);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                    exceptions.add(e);
                }
            });
        }

        readyLatch.await();
        startLatch.countDown();
        executor.shutdown();
        assertThat(executor.awaitTermination(10, TimeUnit.SECONDS)).isTrue();

        assertThat(successCount.get()).isEqualTo(1);
        assertThat(failCount.get()).isEqualTo(threadCount - 1);

        long activeReservations = reservationRepository.findAll().stream()
                .filter(r -> r.getStatus() != ReservationStatus.CANCELED)
                .count();
        assertThat(activeReservations).isEqualTo(1);
    }

    /**
     * 동일 예약을 동시에 확정할 때 @Version 낙관적 잠금이 동작하는지 검증합니다.
     *
     * <p>검증 대상: Hibernate @Version 필드의 implicit 낙관적 잠금
     * <p>시나리오: PENDING_APPROVAL 상태의 예약을 3개 스레드가 동시 확정 시도
     * <p>기대 결과: 최종 상태는 CONFIRMED, version은 정확히 1회만 증가
     *
     * <p>참고: confirmReservation()에는 Service 레벨 try-catch가 없지만,
     * 트랜잭션 커밋 시점에 Hibernate가 version 검사를 수행하여
     * OptimisticLockingFailureException을 발생시킵니다.
     */
    @RepeatedTest(3)
    @DisplayName("동일 예약 동시 확정 시 낙관적 잠금이 충돌을 방어한다")
    void should_preventConflict_whenConcurrentConfirmation() throws InterruptedException {
        Reservation reservation = transactionTemplate.execute(status -> {
            Reservation res = Reservation.builder()
                    .user(user)
                    .office(office)
                    .room(room)
                    .title("확정 동시성 테스트")
                    .startAt(LocalDateTime.now().plusDays(2)
                            .withHour(10).withMinute(0).withSecond(0).withNano(0))
                    .endAt(LocalDateTime.now().plusDays(2)
                            .withHour(11).withMinute(0).withSecond(0).withNano(0))
                    .endAtIncludeBufferTime(LocalDateTime.now().plusDays(2)
                            .withHour(11).withMinute(0).withSecond(0).withNano(0))
                    .status(ReservationStatus.PENDING_APPROVAL)
                    .build();
            return reservationRepository.save(res);
        });

        int threadCount = 3;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch readyLatch = new CountDownLatch(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);
        List<Exception> exceptions = Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    readyLatch.countDown();
                    startLatch.await();
                    reservationService.confirmReservation(reservation.getId(), manager);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                    exceptions.add(e);
                }
            });
        }

        readyLatch.await();
        startLatch.countDown();
        executor.shutdown();
        assertThat(executor.awaitTermination(10, TimeUnit.SECONDS)).isTrue();

        Reservation result = reservationRepository.findById(reservation.getId()).orElseThrow();
        assertThat(result.getStatus()).isEqualTo(ReservationStatus.CONFIRMED);
        assertThat(result.getVersion()).isEqualTo(1L);
        assertThat(successCount.get()).isGreaterThanOrEqualTo(1);
    }

    /**
     * 스레드 수를 늘려도 이중 예약이 절대 발생하지 않는지 검증합니다.
     *
     * <p>HikariCP 기본 풀(기본 10~16) 이하 범위에서 부하를 높여 검증합니다.
     * <p>동시 INSERT 경합 시 데드락(40P01)으로 전원 실패할 수 있으며,
     * 이는 이중 예약 방지 관점에서 안전한 결과입니다 (0건 = 재시도 필요, 2건 이상 = 버그).
     */
    @RepeatedTest(3)
    @DisplayName("스레드 수 증가 시에도 이중 예약은 절대 발생하지 않는다")
    void should_neverAllowDoubleBooking_whenModerateThreadCount() throws InterruptedException {
        int threadCount = 5;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch readyLatch = new CountDownLatch(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        LocalDateTime startAt = LocalDateTime.now()
                .plusDays(3).withHour(14).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime endAt = LocalDateTime.now()
                .plusDays(3).withHour(15).withMinute(30).withSecond(0).withNano(0);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    readyLatch.countDown();
                    startLatch.await();

                    ReservationRequest request = ReservationRequest.builder()
                            .title("안정성 테스트 예약")
                            .officeId(office.getId())
                            .roomId(room.getId())
                            .userId(user.getId())
                            .startAt(startAt)
                            .endAt(endAt)
                            .build();
                    reservationService.createReservation(request);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                }
            });
        }

        readyLatch.await();
        startLatch.countDown();
        executor.shutdown();
        assertThat(executor.awaitTermination(10, TimeUnit.SECONDS)).isTrue();

        // 핵심 검증: 이중 예약 절대 불가 (0건 또는 1건만 허용)
        long activeReservations = reservationRepository.findAll().stream()
                .filter(r -> r.getStatus() != ReservationStatus.CANCELED)
                .count();
        assertThat(activeReservations).isLessThanOrEqualTo(1);
        assertThat(successCount.get()).isLessThanOrEqualTo(1);
    }
}
