import { useOfficeContext } from '../contexts/OfficeContext';

export const OfficeSelectorDropdown: React.FC = () => {
    const { offices, selectedOfficeId, selectOffice, isLoading } = useOfficeContext();

    if (isLoading) {
        return <div className="office-selector" style={{ color: 'var(--color-text-muted)' }}>오피스 목록 불러오는 중...</div>;
    }

    if (offices.length === 0) {
        return (
            <div className="office-selector no-offices" style={{ padding: '10px', color: 'var(--color-text-muted)' }}>
                <p>등록된 오피스가 없습니다. 관리자에게 문의해주세요.</p>
            </div>
        );
    }

    return (
        <div className="office-selector" style={{ width: '100%', marginBottom: '1rem' }}>
            <div 
                className="office-scroll-container"
                style={{ 
                    display: 'flex', 
                    gap: '1.5rem', 
                    overflowX: 'auto', 
                    padding: '0.5rem 0.5rem 1rem 0.5rem',
                    margin: '-0.5rem', /* Componsate for padding to keep alignment */
                    scrollBehavior: 'smooth',
                    WebkitOverflowScrolling: 'touch',
                    alignItems: 'flex-start'
                }}
            >
                {/* "All Offices" Button */}
                <button
                    onClick={() => selectOffice(null as any)}
                    className="office-circle-btn"
                    style={{
                        display: 'flex',
                        flexDirection: 'column',
                        alignItems: 'center',
                        gap: '0.5rem',
                        cursor: 'pointer',
                        background: 'none',
                        border: 'none',
                        padding: 0,
                        minWidth: '60px',
                        outline: 'none'
                    }}
                >
                    <div style={{
                        width: '56px',
                        height: '56px',
                        borderRadius: '50%',
                        backgroundColor: !selectedOfficeId ? 'var(--color-primary)' : '#f1f5f9',
                        color: !selectedOfficeId ? '#ffffff' : '#64748b',
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                        fontSize: '1.25rem',
                        transition: 'all 0.2s ease',
                        boxShadow: !selectedOfficeId ? '0 4px 12px rgba(27, 89, 248, 0.2)' : '0 2px 4px rgba(0,0,0,0.05)',
                        border: !selectedOfficeId ? 'none' : '1px solid #e2e8f0'
                    }}
                    onMouseEnter={(e) => {
                        if (selectedOfficeId) e.currentTarget.style.transform = 'translateY(-2px)'
                    }}
                    onMouseLeave={(e) => {
                        if (selectedOfficeId) e.currentTarget.style.transform = 'none'
                    }}>
                        전체
                    </div>
                    <span style={{ 
                        fontSize: '0.85rem', 
                        fontWeight: !selectedOfficeId ? '700' : '500',
                        color: !selectedOfficeId ? 'var(--color-primary)' : '#475569',
                        whiteSpace: 'nowrap'
                    }}>
                        모든 지점
                    </span>
                </button>

                {/* Individual Office Buttons */}
                {offices.map((office) => {
                    const isSelected = selectedOfficeId === office.id;
                    return (
                        <button
                            key={office.id}
                            onClick={() => selectOffice(office.id)}
                            className="office-circle-btn"
                            title={office.location}
                            style={{
                                display: 'flex',
                                flexDirection: 'column',
                                alignItems: 'center',
                                gap: '0.5rem',
                                cursor: 'pointer',
                                background: 'none',
                                border: 'none',
                                padding: 0,
                                minWidth: '60px',
                                outline: 'none'
                            }}
                        >
                            <div style={{
                                width: '56px',
                                height: '56px',
                                borderRadius: '50%',
                                backgroundColor: isSelected ? 'var(--color-primary)' : '#ffffff',
                                color: isSelected ? '#ffffff' : '#475569',
                                display: 'flex',
                                alignItems: 'center',
                                justifyContent: 'center',
                                fontSize: '1.25rem',
                                transition: 'all 0.2s ease',
                                boxShadow: isSelected ? '0 4px 12px rgba(27, 89, 248, 0.2)' : '0 2px 5px rgba(0,0,0,0.06)',
                                border: isSelected ? 'none' : '1px solid #e2e8f0',
                                position: 'relative'
                            }}
                            onMouseEnter={(e) => {
                                if (!isSelected) e.currentTarget.style.transform = 'translateY(-2px)'
                            }}
                            onMouseLeave={(e) => {
                                if (!isSelected) e.currentTarget.style.transform = 'none'
                            }}>
                                🏢
                            </div>
                            <span style={{ 
                                fontSize: '0.85rem', 
                                fontWeight: isSelected ? '700' : '500',
                                color: isSelected ? 'var(--color-primary)' : '#475569',
                                whiteSpace: 'nowrap'
                            }}>
                                {office.name}
                            </span>
                        </button>
                    );
                })}
            </div>
        </div>
    );
};

