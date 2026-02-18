-- =========================================
-- V3: room_favorite 테이블에 updated_at 컬럼 추가
-- RoomFavorite 엔티티가 BaseEntity를 상속하므로 updated_at 필요
-- 업데이트 날짜 2026/02/18
-- =========================================

ALTER TABLE room_favorite
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ NOT NULL DEFAULT now();

-- updated_at 자동 갱신 트리거 추가
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_trigger WHERE tgname = 'tr_room_favorite_updated_at') THEN
    CREATE TRIGGER tr_room_favorite_updated_at
    BEFORE UPDATE ON room_favorite
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();
  END IF;
END$$;
