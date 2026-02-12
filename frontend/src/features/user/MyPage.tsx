import { useState, useEffect } from 'react';
import { useAuth } from '../../contexts/AuthContext';
import { useNavigate, Link } from 'react-router-dom';
import { wishlistApi } from '../wishlist/api/wishlist.api';
import { roomApi } from '../rooms/api/room.api';
import { officeApi } from '../rooms/api/office.api';
import RoomCard, { type Room } from '../rooms/RoomCard';

export default function MyPage() {
    const { user, logout } = useAuth();
    const navigate = useNavigate();
    const [activeTab, setActiveTab] = useState<'SETTINGS' | 'WISHLIST'>('SETTINGS');
    const [wishlistRooms, setWishlistRooms] = useState<Room[]>([]);
    const [loading, setLoading] = useState(false);

    useEffect(() => {
        if (activeTab === 'WISHLIST' && user) {
            loadWishlist();
        }
    }, [activeTab, user]);

    const loadWishlist = async () => {
        if (!user) return;
        setLoading(true);
        try {
            const wishlistIds = await wishlistApi.getWishlist(user.id);
            if (wishlistIds.length > 0) {
                // Fetch all data
                const [allRooms, allOffices] = await Promise.all([
                    roomApi.getAllRooms(),
                    officeApi.getAllOffices()
                ]);

                const filtered = allRooms.filter(r => wishlistIds.includes(Number(r.id)));

                const mappedRooms: Room[] = filtered.map(r => {
                    const office = allOffices.find(o => o.id === r.officeId);
                    return {
                        id: String(r.id),
                        name: r.name,
                        location: office ? `${office.name} ${r.floor} F` : `${r.floor} F`,
                        capacity: r.capacity,
                        equipment: [],
                        imageUrl: r.imageUrl,
                        isAvailable: r.status === 'AVAILABLE'
                    };
                });
                setWishlistRooms(mappedRooms);
            } else {
                setWishlistRooms([]);
            }
        } catch (error) {
            console.error("Failed to load wishlist", error);
        } finally {
            setLoading(false);
        }
    };

    const handleWithdrawal = () => {
        if (window.confirm('정말로 탈퇴하시겠습니까? 이 작업은 되돌릴 수 없습니다.')) {
            // Call API to delete user
            alert('회원 탈퇴가 완료되었습니다.');
            logout();
            navigate('/login');
        }
    };

    if (!user) return <div>로그인이 필요합니다.</div>;

    return (
        <div className="container mx-auto p-md max-w-4xl">
            <h1 className="text-3xl font-bold mb-xl">마이페이지</h1>

            <div className="flex gap-md mb-lg border-b">
                <button
                    className={`pb-sm px-md ${activeTab === 'SETTINGS' ? 'border-b-2 border-primary font-bold text-primary' : 'text-muted'}`}
                    onClick={() => setActiveTab('SETTINGS')}
                >
                    내 프로필
                </button>
                <button
                    className={`pb-sm px-md ${activeTab === 'WISHLIST' ? 'border-b-2 border-primary font-bold text-primary' : 'text-muted'}`}
                    onClick={() => setActiveTab('WISHLIST')}
                >
                    관심 목록
                </button>
            </div>

            {activeTab === 'SETTINGS' && (
                <div className="max-w-2xl">
                    <div className="card bg-white shadow-sm p-xl mb-lg">
                        <div className="flex items-center mb-lg">
                            <div className="w-16 h-16 bg-gray-200 rounded-full flex items-center justify-center text-2xl mr-md">
                                {user.name[0]}
                            </div>
                            <div>
                                <h2 className="text-xl font-bold">{user.name}</h2>
                                <p className="text-muted">{user.email}</p>
                                <span className="inline-block mt-xs px-2 py-0.5 bg-gray-100 text-xs rounded text-gray-600">
                                    {user.role}
                                </span>
                            </div>
                        </div>

                        <hr className="my-lg border-gray-100" />

                        <h3 className="font-bold mb-md">계정 설정</h3>
                        <div className="space-y-sm">
                            <button className="w-full text-left p-sm hover:bg-gray-50 rounded flex justify-between">
                                <span>비밀번호 변경</span>
                                <span className="text-muted">Currently disabled</span>
                            </button>
                            <button className="w-full text-left p-sm hover:bg-gray-50 rounded flex justify-between">
                                <span>알림 설정</span>
                                <span className="text-muted">ON</span>
                            </button>
                        </div>
                    </div>

                    <div className="text-right">
                        <button
                            onClick={handleWithdrawal}
                            className="text-red-500 text-sm hover:underline"
                        >
                            회원 탈퇴하기
                        </button>
                    </div>
                </div>
            )}

            {activeTab === 'WISHLIST' && (
                <div>
                    {loading ? (
                        <div className="text-center py-xl text-muted">Loading...</div>
                    ) : wishlistRooms.length > 0 ? (
                        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-md">
                            {wishlistRooms.map(room => (
                                <RoomCard key={room.id} room={room} />
                            ))}
                        </div>
                    ) : (
                        <div className="text-center py-xl bg-gray-50 rounded-lg">
                            <p className="text-muted mb-md">관심 등록한 회의실이 없습니다.</p>
                            <Link to="/rooms" className="btn btn-primary">회의실 둘러보기</Link>
                        </div>
                    )}
                </div>
            )}
        </div>
    );
}
