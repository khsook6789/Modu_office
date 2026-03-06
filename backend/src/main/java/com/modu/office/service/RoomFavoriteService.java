package com.modu.office.service;

import com.modu.office.dto.response.RoomFavoriteResponse;
import com.modu.office.entity.AppUser;
import com.modu.office.entity.Room;
import com.modu.office.entity.RoomFavorite;
import com.modu.office.repository.AppUserRepository;
import com.modu.office.repository.RoomRepository;
import com.modu.office.repository.RoomFavoriteRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * RoomFavorite 비즈니스 로직 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoomFavoriteService {

    private final RoomFavoriteRepository favoriteRepository;
    private final RoomRepository roomRepository;
    private final AppUserRepository userRepository;

    /**
     * 즐겨찾기 추가
     */
    @Transactional
    public RoomFavoriteResponse addFavorite(Long userId, Long roomId) {
        if (userId == null)
            throw new IllegalArgumentException("userId must not be null");
        if (roomId == null)
            throw new IllegalArgumentException("roomId must not be null");

        // 1. 중복 체크
        if (favoriteRepository.existsByUserIdAndRoomId(userId, roomId)) {
            throw new IllegalStateException("이미 즐겨찾기에 추가된 회의실입니다.");
        }

        // 2. 사용자 존재 확인
        AppUser user = userRepository.findById(java.util.Objects.requireNonNull(userId))
                .orElseThrow(() -> new EntityNotFoundException("사용자를 찾을 수 없습니다. ID: " + userId));

        // 3. 회의실 존재 확인
        Room room = roomRepository.findById(java.util.Objects.requireNonNull(roomId))
                .orElseThrow(() -> new EntityNotFoundException("회의실을 찾을 수 없습니다. ID: " + roomId));

        // 4. 즐겨찾기 생성
        RoomFavorite favorite = RoomFavorite.builder()
                .user(user)
                .room(room)
                .build();

        RoomFavorite saved = favoriteRepository.save(java.util.Objects.requireNonNull(favorite));

        log.info("즐겨찾기 추가됨: userId={}, roomId={}", userId, roomId);

        return RoomFavoriteResponse.fromEntity(saved);
    }

    /**
     * 즐겨찾기 삭제
     */
    @Transactional
    public void removeFavorite(Long userId, Long roomId) {
        RoomFavorite favorite = favoriteRepository
                .findByUserIdAndRoomId(userId, roomId)
                .orElseThrow(() -> new EntityNotFoundException("즐겨찾기를 찾을 수 없습니다."));

        if (favorite == null)
            throw new EntityNotFoundException("즐겨찾기를 찾을 수 없습니다.");

        favoriteRepository.delete(favorite);

        log.info("즐겨찾기 삭제됨: userId={}, roomId={}", userId, roomId);
    }

    /**
     * 특정 사용자의 즐겨찾기 목록 조회
     */
    public List<RoomFavoriteResponse> getUserFavorites(Long userId) {
        return favoriteRepository.findByUserId(userId).stream()
                .map(RoomFavoriteResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * 즐겨찾기 여부 확인
     */
    public boolean isFavorite(Long userId, Long roomId) {
        return favoriteRepository.existsByUserIdAndRoomId(userId, roomId);
    }

    /**
     * 특정 회의실의 즐겨찾기 개수 조회
     */
    public Long getFavoriteCount(Long roomId) {
        return favoriteRepository.countByRoomId(roomId);
    }
}
