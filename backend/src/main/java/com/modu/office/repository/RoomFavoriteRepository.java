package com.modu.office.repository;

import com.modu.office.entity.RoomFavorite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * RoomFavorite 엔티티에 대한 데이터 액세스 레포지토리
 */
@Repository
public interface RoomFavoriteRepository extends JpaRepository<RoomFavorite, Long> {

    /**
     * 특정 사용자의 모든 즐겨찾기 조회
     */
    List<RoomFavorite> findByUserId(Long userId);

    /**
     * 특정 회의실의 즐겨찾기 개수 조회 (인기도 측정)
     */
    Long countByRoomId(Long roomId);

    /**
     * 중복 체크: 이미 즐겨찾기에 추가했는지 확인
     */
    boolean existsByUserIdAndRoomId(Long userId, Long roomId);

    /**
     * 즐겨찾기 삭제를 위한 조회
     */
    Optional<RoomFavorite> findByUserIdAndRoomId(Long userId, Long roomId);

    /**
     * 특정 회의실이 삭제될 때 관련 즐겨찾기 삭제 (Cascade)
     */
    void deleteAllByRoomId(Long roomId);

    /**
     * 특정 사용자의 모든 즐겨찾기 삭제 (회원탈퇴 시 사용)
     */
    void deleteAllByUserId(Long userId);
}
