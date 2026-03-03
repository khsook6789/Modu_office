import { client } from "../../../api/client";

export interface Favorite {
  id: number;
  roomId: number;
  roomName: string;
  officeName: string;
  floor: number;
  capacity: number;
  imageUrl: string | null;
}

export const favoriteApi = {
  /** 즐겨찾기 목록 조회 */
  getMyFavorites: async (): Promise<Favorite[]> => {
    const res = await client.get<any>("/favorites");
    const raw = res.data ?? res;
    return raw?.data ?? raw ?? [];
  },

  /** 즐겨찾기 추가 */
  addFavorite: async (roomId: number): Promise<Favorite> => {
    const res = await client.post<any>("/favorites", { roomId });
    const raw = res.data ?? res;
    return raw?.data ?? raw;
  },

  /** 즐겨찾기 삭제 */
  removeFavorite: async (roomId: number): Promise<void> => {
    await client.delete(`/favorites/${roomId}`);
  },

  /** 즐겨찾기 여부 확인 */
  checkFavorite: async (roomId: number): Promise<boolean> => {
    const res = await client.get<any>(`/favorites/check/${roomId}`);
    const raw = res.data ?? res;
    return raw?.data ?? raw ?? false;
  },
};
