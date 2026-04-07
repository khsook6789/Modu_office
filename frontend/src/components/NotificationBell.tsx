import { useState, useEffect, useRef, useCallback } from 'react';
import { useAuth } from '../contexts/AuthContext';
import { notificationApi, type NotificationResponse } from '../features/notifications/api/notification.api';
import './NotificationBell.css';

export default function NotificationBell() {
    const { user } = useAuth();
    const [unreadCount, setUnreadCount] = useState(0);
    const [notifications, setNotifications] = useState<NotificationResponse[]>([]);
    const [isOpen, setIsOpen] = useState(false);
    const [isLoading, setIsLoading] = useState(false);
    const dropdownRef = useRef<HTMLDivElement>(null);
    const esRef = useRef<EventSource | null>(null);

    // 읽지 않은 개수 폴링
    const fetchUnreadCount = useCallback(async () => {
        try {
            const count = await notificationApi.getUnreadCount();
            setUnreadCount(count);
        } catch {}
    }, []);

    // SSE 구독 (로그인 시)
    useEffect(() => {
        if (!user) return;
        fetchUnreadCount();

        // SSE 구독이 토큰을 쿼리파라미터로 지원하지 않을 경우 대비: 폴링 fallback
        try {
            const es = notificationApi.subscribe((notification) => {
                setNotifications(prev => [notification, ...prev]);
                setUnreadCount(prev => prev + 1);
            });
            // onerror 시 close() 하지 않음 — EventSource는 연결 오류 시 자동 재연결함
            // close()를 호출하면 내장 재연결 동작이 중단되어 알림이 영구적으로 끊김
            esRef.current = es;
        } catch {}


        // 30초마다 폴링 fallback
        const interval = setInterval(fetchUnreadCount, 30000);
        return () => {
            clearInterval(interval);
            esRef.current?.close();
        };
    }, [user?.id]);

    // 드롭다운 외부 클릭 닫기
    useEffect(() => {
        const handleClickOutside = (e: MouseEvent) => {
            if (dropdownRef.current && !dropdownRef.current.contains(e.target as Node)) {
                setIsOpen(false);
            }
        };
        document.addEventListener('mousedown', handleClickOutside);
        return () => document.removeEventListener('mousedown', handleClickOutside);
    }, []);

    const handleToggle = async () => {
        const opening = !isOpen;
        setIsOpen(opening);
        if (opening) {
            setIsLoading(true);
            try {
                const result = await notificationApi.getMyNotifications();
                setNotifications(result.content);
            } catch {
            } finally {
                setIsLoading(false);
            }
        }
    };

    const handleMarkAsRead = async (id: number) => {
        try {
            await notificationApi.markAsRead(id);
            setNotifications(prev => prev.map(n => n.id === id ? { ...n, isRead: true } : n));
            setUnreadCount(prev => Math.max(0, prev - 1));
        } catch {}
    };

    const handleMarkAllAsRead = async () => {
        try {
            await notificationApi.markAllAsRead();
            setNotifications(prev => prev.map(n => ({ ...n, isRead: true })));
            setUnreadCount(0);
        } catch {}
    };

    if (!user) return null;

    return (
        <div className="notif-wrapper" ref={dropdownRef}>
            <button
                className="notif-bell-btn"
                onClick={handleToggle}
                aria-label="알림"
                title="알림"
            >
                🔔
                {unreadCount > 0 && (
                    <span className="notif-badge">{unreadCount > 99 ? '99+' : unreadCount}</span>
                )}
            </button>

            {isOpen && (
                <div className="notif-dropdown">
                    <div className="notif-header">
                        <span className="notif-title">알림</span>
                        {unreadCount > 0 && (
                            <button className="notif-read-all" onClick={handleMarkAllAsRead}>
                                모두 읽음
                            </button>
                        )}
                    </div>

                    <div className="notif-list">
                        {isLoading ? (
                            <div className="notif-empty">불러오는 중...</div>
                        ) : notifications.length === 0 ? (
                            <div className="notif-empty">
                                <span className="notif-empty-icon">🔕</span>
                                <p>새로운 알림이 없습니다</p>
                            </div>
                        ) : (
                            notifications.map(n => (
                                <div
                                    key={n.id}
                                    className={`notif-item ${n.isRead ? 'read' : 'unread'}`}
                                    onClick={() => !n.isRead && handleMarkAsRead(n.id)}
                                >
                                    <div className="notif-item-content">
                                        <p className="notif-message">{n.message}</p>
                                        <span className="notif-time">
                                            {new Date(n.createdAt).toLocaleString('ko-KR', {
                                                month: 'short', day: 'numeric',
                                                hour: '2-digit', minute: '2-digit'
                                            })}
                                        </span>
                                    </div>
                                    {!n.isRead && <span className="notif-dot" />}
                                </div>
                            ))
                        )}
                    </div>
                </div>
            )}
        </div>
    );
}
