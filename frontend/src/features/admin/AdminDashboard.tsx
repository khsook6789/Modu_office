import { useState, useEffect } from 'react';

export default function AdminDashboard() {
    const [users, setUsers] = useState<any[]>([]);
    const [activeTab, setActiveTab] = useState<'OVERVIEW' | 'USERS'>('OVERVIEW');

    useEffect(() => {
        loadUsers();
    }, []);

    const loadUsers = () => {
        const storedUsers = JSON.parse(localStorage.getItem('users') || '[]');
        setUsers(storedUsers);
    };

    const handleDeleteUser = (email: string) => {
        if (window.confirm('정말로 이 사용자를 삭제하시겠습니까?')) {
            const newUsers = users.filter(u => u.email !== email);
            localStorage.setItem('users', JSON.stringify(newUsers));
            setUsers(newUsers);
            alert('사용자가 삭제되었습니다.');
        }
    };

    const handleChangeRole = (email: string, newRole: string) => {
        if (window.confirm(`사용자의 권한을 ${newRole}로 변경하시겠습니까?`)) {
            const newUsers = users.map(u => {
                if (u.email === email) {
                    return { ...u, role: newRole };
                }
                return u;
            });
            localStorage.setItem('users', JSON.stringify(newUsers));
            setUsers(newUsers);
            alert('권한이 변경되었습니다.');
        }
    };

    const stats = {
        totalUsers: users.length,
        activeBookings: 0, // Mock for now
        monthlyRevenue: 0 // Mock for now
    };

    return (
        <div className="container mx-auto p-md">
            <h1 className="text-3xl font-bold mb-lg">관리자 대시보드</h1>

            <div className="flex gap-md mb-lg border-b">
                <button
                    className={`pb-sm px-md ${activeTab === 'OVERVIEW' ? 'border-b-2 border-primary font-bold text-primary' : 'text-muted'}`}
                    onClick={() => setActiveTab('OVERVIEW')}
                >
                    개요
                </button>
                <button
                    className={`pb-sm px-md ${activeTab === 'USERS' ? 'border-b-2 border-primary font-bold text-primary' : 'text-muted'}`}
                    onClick={() => setActiveTab('USERS')}
                >
                    사용자 관리
                </button>
            </div>

            {activeTab === 'OVERVIEW' && (
                <div className="grid grid-cols-1 md:grid-cols-3 gap-md">
                    <div className="card p-lg bg-white shadow-sm">
                        <h3 className="text-muted text-sm font-bold uppercase mb-xs">총 사용자</h3>
                        <p className="text-3xl font-bold">{stats.totalUsers}명</p>
                    </div>
                    {/* Placeholder stats */}
                </div>
            )}

            {activeTab === 'USERS' && (
                <div className="bg-white rounded-lg shadow-sm overflow-hidden">
                    <div className="p-md border-b flex justify-between items-center">
                        <h2 className="text-xl font-bold">전체 사용자 목록</h2>
                    </div>

                    <div className="overflow-x-auto">
                        <table className="w-full text-left border-collapse">
                            <thead>
                                <tr className="bg-gray-50 border-b text-sm text-muted uppercase">
                                    <th className="p-md font-medium">이름</th>
                                    <th className="p-md font-medium">이메일</th>
                                    <th className="p-md font-medium">권한</th>
                                    <th className="p-md font-medium text-center">관리</th>
                                </tr>
                            </thead>
                            <tbody>
                                {users.map((user) => (
                                    <tr key={user.email} className="border-b hover:bg-gray-50">
                                        <td className="p-md">{user.name}</td>
                                        <td className="p-md">{user.email}</td>
                                        <td className="p-md">
                                            <span className={`px-2 py-1 rounded text-xs font-bold ${user.role === 'ADMIN' ? 'bg-purple-100 text-purple-700' :
                                                    user.role === 'OPERATOR' ? 'bg-blue-100 text-blue-700' :
                                                        'bg-gray-100 text-gray-700'
                                                }`}>
                                                {user.role || 'USER'}
                                            </span>
                                        </td>
                                        <td className="p-md text-center">
                                            <div className="flex justify-center gap-2">
                                                {user.role !== 'ADMIN' && (
                                                    <>
                                                        <button
                                                            onClick={() => handleChangeRole(user.email, 'OPERATOR')}
                                                            className="btn btn-sm btn-outline px-2 text-xs"
                                                            title="운영자로 변경"
                                                        >
                                                            운영자
                                                        </button>
                                                        <button
                                                            onClick={() => handleDeleteUser(user.email)}
                                                            className="btn btn-sm text-red-500 hover:bg-red-50 px-2 text-xs"
                                                        >
                                                            삭제
                                                        </button>
                                                    </>
                                                )}
                                            </div>
                                        </td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </div>
                </div>
            )}
        </div>
    );
}
