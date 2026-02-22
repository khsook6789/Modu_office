package com.modu.office.service;

import com.modu.office.dto.request.ReviewRequest;
import com.modu.office.dto.request.ReviewUpdateRequest;
import com.modu.office.dto.response.ReviewResponse;
import com.modu.office.entity.AppUser;
import com.modu.office.entity.Reservation;
import com.modu.office.entity.Review;
import com.modu.office.entity.enums.ReservationStatus;
import com.modu.office.repository.ReservationRepository;
import com.modu.office.repository.ReviewRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @InjectMocks
    private ReviewService reviewService;

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private ReservationRepository reservationRepository;

    @Test
    @DisplayName("리뷰 작성 성공")
    void createReview_success() {
        // given
        AppUser user = mock(AppUser.class);
        given(user.getId()).willReturn(1L);
        given(user.getName()).willReturn("Tester");

        Reservation reservation = mock(Reservation.class);
        given(reservation.getId()).willReturn(10L);
        given(reservation.getCustomer()).willReturn(user);
        given(reservation.getStatus()).willReturn(ReservationStatus.CONFIRMED);
        given(reservation.getEndAt()).willReturn(LocalDateTime.now().minusHours(1)); // 이용 시간 경과 필수

        ReviewRequest request = ReviewRequest.builder()
                .reservationId(10L)
                .rating((short) 5)
                .content("완벽한 회의실입니다.")
                .build();

        given(reservationRepository.findById(10L)).willReturn(Optional.of(reservation));
        given(reviewRepository.findByReservationId(10L)).willReturn(Optional.empty());

        Review savedReview = mock(Review.class);
        given(savedReview.getId()).willReturn(100L);
        given(savedReview.getReservation()).willReturn(reservation);
        given(savedReview.getAuthorUser()).willReturn(user);
        given(savedReview.getRating()).willReturn((short) 5);
        given(savedReview.getContent()).willReturn("완벽한 회의실입니다.");
        given(savedReview.getCreatedAt()).willReturn(LocalDateTime.now());
        given(savedReview.getUpdatedAt()).willReturn(LocalDateTime.now());

        given(reviewRepository.save(any(Review.class))).willReturn(savedReview);

        // when
        ReviewResponse response = reviewService.createReview(user, request);

        // then
        assertThat(response.getRating()).isEqualTo((short) 5);
        assertThat(response.getContent()).isEqualTo("완벽한 회의실입니다.");
        verify(reviewRepository, times(1)).save(any(Review.class));
    }

    @Test
    @DisplayName("리뷰 작성 실패 - 확정(이용완료)되지 않은 예약")
    void createReview_fail_notConfirmed() {
        // given
        AppUser user = mock(AppUser.class);
        given(user.getId()).willReturn(1L);

        Reservation reservation = mock(Reservation.class);
        given(reservation.getCustomer()).willReturn(user);
        given(reservation.getStatus()).willReturn(ReservationStatus.PENDING); // 에러 원인

        ReviewRequest request = ReviewRequest.builder().reservationId(10L).rating((short) 5).content("Test").build();

        given(reservationRepository.findById(10L)).willReturn(Optional.of(reservation));

        // when & then
        assertThatThrownBy(() -> reviewService.createReview(user, request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("확정된");
    }

    @Test
    @DisplayName("리뷰 작성 실패 - 이용 시간 종료 전")
    void createReview_fail_beforeEndAt() {
        // given
        AppUser user = mock(AppUser.class);
        given(user.getId()).willReturn(1L);

        Reservation reservation = mock(Reservation.class);
        given(reservation.getCustomer()).willReturn(user);
        given(reservation.getStatus()).willReturn(ReservationStatus.CONFIRMED);
        given(reservation.getEndAt()).willReturn(LocalDateTime.now().plusHours(1)); // 미래 시간

        ReviewRequest request = ReviewRequest.builder().reservationId(10L).rating((short) 5).content("Test").build();

        given(reservationRepository.findById(10L)).willReturn(Optional.of(reservation));

        // when & then
        assertThatThrownBy(() -> reviewService.createReview(user, request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("종료된 후");
    }

    @Test
    @DisplayName("리뷰 삭제 실패 - IDOR (작성자가 아닌 사용자)")
    void deleteReview_fail_idor() {
        // given
        AppUser author = mock(AppUser.class);
        given(author.getId()).willReturn(1L);

        AppUser anotherUser = mock(AppUser.class);
        given(anotherUser.getId()).willReturn(2L);

        Review review = mock(Review.class);
        given(review.getAuthorUser()).willReturn(author);

        given(reviewRepository.findById(100L)).willReturn(Optional.of(review));

        // when & then
        assertThatThrownBy(() -> reviewService.deleteReview(100L, anotherUser))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("본인이 작성한 후기만");
    }

    @Test
    @DisplayName("리뷰 수정 성공")
    void updateReview_success() {
        // given
        AppUser author = mock(AppUser.class);
        given(author.getId()).willReturn(1L);
        given(author.getName()).willReturn("Tester");

        Reservation reservation = mock(Reservation.class);
        given(reservation.getId()).willReturn(10L);

        Review review = spy(Review.builder()
                .reservation(reservation)
                .authorUser(author)
                .rating((short) 4)
                .content("Good")
                .build());
        lenient().when(review.getId()).thenReturn(100L); // Optional for safety if called

        ReviewUpdateRequest request = ReviewUpdateRequest.builder()
                .rating((short) 5)
                .content("Excellent")
                .build();

        given(reviewRepository.findById(100L)).willReturn(Optional.of(review));

        // when
        ReviewResponse response = reviewService.updateReview(100L, author, request);

        // then
        assertThat(review.getRating()).isEqualTo((short) 5);
        assertThat(review.getContent()).isEqualTo("Excellent");
        assertThat(response.getRating()).isEqualTo((short) 5);
    }
}
