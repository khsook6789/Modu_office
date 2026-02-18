import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { type OfficeRoomResponse, roomApi } from '../rooms/api/room.api';
import Input from '../../components/Input';

export default function BookingPage() {
    const { roomId } = useParams();
    const navigate = useNavigate();
    const [room, setRoom] = useState<OfficeRoomResponse | null>(null);

    // Booking Form State
    const [date, setDate] = useState('');
    const [startTime, setStartTime] = useState('09:00');
    const [duration, setDuration] = useState('1'); // hours
    const [guestCount, setGuestCount] = useState('1');
    const [totalPrice, setTotalPrice] = useState(0);

    useEffect(() => {
        if (roomId) {
            roomApi.getRoomById(Number(roomId))
                .then(data => {
                    setRoom(data);
                    // Set default date to today
                    const today = new Date().toISOString().split('T')[0];
                    setDate(today);
                })
                .catch(err => {
                    console.error("Failed to fetch room details", err);
                    alert("존재하지 않는 회의실입니다.");
                    navigate('/rooms');
                });
        }
    }, [roomId, navigate]);

    // Calculate Price
    useEffect(() => {
        if (room) {
            // Mock price: 10000 KRW per hour
            const pricePerHour = 10000;
            setTotalPrice(Number(duration) * pricePerHour);
        }
    }, [duration, room]);

    const handleBooking = () => {
        // Validation
        if (!date || !startTime) {
            alert('날짜와 시간을 선택해주세요.');
            return;
        }

        // Navigate to payment with state
        const bookingData = {
            roomId,
            roomName: room?.name,
            date,
            start_at: startTime,
            end_at: calculateEndTime(startTime, Number(duration)),
            guestCount,
            totalPrice
        };

        // In a real app we might create a "Pending" booking ID here.
        // For simplicity, we pass data to PaymentPage via state.
        navigate('/payment', { state: bookingData });
    };

    const calculateEndTime = (start: string, durationHours: number) => {
        const [hours, minutes] = start.split(':').map(Number);
        const endHours = hours + durationHours;
        return `${String(endHours).padStart(2, '0')}:${String(minutes).padStart(2, '0')}`;
    };

    if (!room) return <div className="p-xl text-center">Loading room details...</div>;

    return (
        <div className="container mx-auto p-md max-w-lg">
            <h1 className="text-2xl font-bold mb-lg">예약하기</h1>

            <div className="card bg-white shadow-sm p-lg mb-md">
                <div className="flex gap-md mb-md border-b pb-md">
                    <img
                        src={room.imageUrl}
                        alt={room.name}
                        className="w-24 h-24 object-cover rounded-md"
                    />
                    <div>
                        <h2 className="text-xl font-bold">{room.name}</h2>
                        <p className="text-muted text-sm">최대 {room.capacity}명 수용</p>
                        <p className="text-primary font-bold mt-xs">10,000원 / 시간</p>
                    </div>
                </div>

                <div className="space-y-md">
                    <Input
                        label="날짜 선택"
                        type="date"
                        value={date}
                        onChange={(e) => setDate(e.target.value)}
                        required
                        fullWidth
                        min={new Date().toISOString().split('T')[0]}
                    />

                    <div className="grid grid-cols-2 gap-md">
                        <Input
                            label="시작 시간"
                            type="time"
                            value={startTime}
                            onChange={(e) => setStartTime(e.target.value)}
                            required
                            fullWidth
                            min="09:00"
                            max="18:00"
                        />
                        <div className="form-group">
                            <label className="block text-sm font-medium mb-xs">이용 시간</label>
                            <select
                                className="input-field w-full"
                                value={duration}
                                onChange={(e) => setDuration(e.target.value)}
                            >
                                <option value="1">1시간</option>
                                <option value="2">2시간</option>
                                <option value="3">3시간</option>
                                <option value="4">4시간</option>
                            </select>
                        </div>
                    </div>

                    {/* Guest Count Stepper */}
                    <div className="form-group input-wrapper">
                        <label className="input-label text-sm font-bold mb-xs">
                            인원 (최대 {room.capacity}명)
                        </label>
                        <div className="flex items-center gap-xs">
                            <button
                                className="btn btn-secondary h-12 w-12 flex items-center justify-center p-0 text-xl font-bold"
                                onClick={() => {
                                    const current = parseInt(guestCount);
                                    if (current > 1) setGuestCount((current - 1).toString());
                                }}
                                disabled={parseInt(guestCount) <= 1}
                            >
                                -
                            </button>
                            <div className="flex-1 input-container">
                                <input
                                    type="number"
                                    className="input-field text-center font-bold text-lg"
                                    value={guestCount}
                                    readOnly
                                />
                            </div>
                            <button
                                className="btn btn-secondary h-12 w-12 flex items-center justify-center p-0 text-xl font-bold"
                                onClick={() => {
                                    const current = parseInt(guestCount);
                                    if (current < room.capacity) setGuestCount((current + 1).toString());
                                }}
                                disabled={parseInt(guestCount) >= room.capacity}
                            >
                                +
                            </button>
                        </div>
                    </div>
                </div>
            </div>

            <div className="card bg-gray-50 p-md flex justify-between items-center mb-lg">
                <span className="text-lg font-bold">총 결제 금액</span>
                <span className="text-2xl font-bold text-primary">{totalPrice.toLocaleString()}원</span>
            </div>

            <div className="flex gap-md">
                <button
                    onClick={() => navigate(-1)}
                    className="btn btn-outline flex-1"
                >
                    취소
                </button>
                <button
                    onClick={handleBooking}
                    className="btn btn-primary flex-1 py-3 text-lg"
                >
                    다음 (결제)
                </button>
            </div>
        </div>
    );
}
