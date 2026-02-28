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
        <div className="office-selector">
            <label htmlFor="office-select" style={{ color: 'var(--color-text-main)', fontWeight: 'bold' }}>
                오피스 선택:
            </label>
            <select
                id="office-select"
                value={selectedOfficeId || ''}
                onChange={(e) => selectOffice(Number(e.target.value))}
                style={{
                    marginLeft: '10px',
                    padding: '8px 12px',
                    fontSize: '14px',
                    borderRadius: 'var(--radius-md)',
                    border: 'var(--glass-border)',
                    background: 'var(--color-bg-card)',
                    color: 'var(--color-text-main)',
                    outline: 'none'
                }}
            >
                <option value="" style={{ background: '#ffffff', color: '#0f172a' }}>-- 오피스를 선택하세요 --</option>
                {offices.map((office) => (
                    <option key={office.id} value={office.id} style={{ background: '#ffffff', color: '#0f172a' }}>
                        {office.name} ({office.location})
                    </option>
                ))}
            </select>
        </div>
    );
};

