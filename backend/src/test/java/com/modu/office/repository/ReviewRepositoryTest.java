package com.modu.office.repository;

import com.modu.office.entity.*;
import com.modu.office.entity.enums.AccountStatus;
import com.modu.office.entity.enums.LoginType;
import com.modu.office.entity.enums.ReservationStatus;
import com.modu.office.entity.enums.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

import org.springframework.test.context.ActiveProfiles;
import org.springframework.context.annotation.Import;
import com.modu.office.config.QueryDslConfig;
import com.modu.office.config.JpaConfig;

@DataJpaTest
@ActiveProfiles("test")
@Import({ QueryDslConfig.class, JpaConfig.class })
class ReviewRepositoryTest {

        @Autowired
        private ReviewRepository reviewRepository;
        @Autowired
        private ReservationRepository reservationRepository;
        @Autowired
        private AppUserRepository appUserRepository;
        @Autowired
        private AccountRepository accountRepository;
        @Autowired
        private RoomRepository roomRepository;
        @Autowired
        private OfficeRepository officeRepository;

        private AppUser user;
        private Room room;
        private Reservation reservation;

        @BeforeEach
        void setUp() {
                Account account = accountRepository.save(Account.builder()
                                .email("test@test.com")
                                .passwordHash("hash")
                                .status(AccountStatus.ACTIVE)
                                .loginType(LoginType.LOCAL)
                                .build());

                user = appUserRepository.save(AppUser.builder()
                                .account(account)
                                .name("Tester")
                                .role(UserRole.USER)
                                .build());

                Office office = officeRepository.save(Office.builder()
                                .name("Test Office")
                                .location("Seoul")
                                .latitude(37.5)
                                .longitude(127.0)
                                .openTime(LocalTime.of(9, 0))
                                .closeTime(LocalTime.of(18, 0))
                                .manager(user)
                                .build());

                room = roomRepository.save(Room.builder()
                                .office(office)
                                .name("Test Room")
                                .roomCode("R001")
                                .capacity(4)
                                .price(BigDecimal.valueOf(10000))
                                .build());

                reservation = reservationRepository.save(Reservation.builder()
                                .user(user)
                                .room(room)
                                .startAt(LocalDateTime.now().minusHours(2))
                                .endAt(LocalDateTime.now().minusHours(1))
                                .endAtIncludeBufferTime(LocalDateTime.now().minusHours(1))
                                .status(ReservationStatus.CONFIRMED)
                                .build());
        }

        @Test
        @DisplayName("예약 ID로 후기 조회")
        void findByReservationId() {
                // given
                Review review = Review.builder().reservation(reservation).author(user).rating((short) 5)
                                .content("Great!")
                                .build();
                reviewRepository.save(review);

                // when
                Optional<Review> found = reviewRepository.findByReservationId(reservation.getId());

                // then
                assertThat(found).isPresent();
                assertThat(found.get().getContent()).isEqualTo("Great!");
        }

        @Test
        @DisplayName("특정 회의실의 후기 목록 페이징 및 평점 통계 테스트")
        void roomReviewStatistics() {
                // given
                Review review1 = Review.builder().reservation(reservation).author(user).rating((short) 5)
                                .content("Good")
                                .build();
                reviewRepository.save(review1);

                Reservation res2 = reservationRepository.save(Reservation.builder()
                                .user(user).room(room)
                                .startAt(LocalDateTime.now().minusDays(1))
                                .endAt(LocalDateTime.now().minusDays(1).plusHours(1))
                                .endAtIncludeBufferTime(LocalDateTime.now().minusDays(1).plusHours(1))
                                .status(ReservationStatus.CONFIRMED).build());
                Review review2 = Review.builder().reservation(res2).author(user).rating((short) 4).content("Nice")
                                .build();
                reviewRepository.save(review2);

                // when
                Page<Review> roomReviews = reviewRepository.findByReservationRoomId(room.getId(),
                                PageRequest.of(0, 10));
                Optional<Double> avgRating = reviewRepository.findAverageRatingByRoomId(room.getId());
                Long count = reviewRepository.countByRoomId(room.getId());

                // then
                assertThat(roomReviews.getTotalElements()).isEqualTo(2);
                assertThat(avgRating).isPresent();
                assertThat(avgRating.get()).isEqualTo(4.5);
                assertThat(count).isEqualTo(2);
        }
}
