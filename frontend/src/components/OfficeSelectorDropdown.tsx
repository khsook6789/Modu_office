import { useOfficeContext } from '../contexts/OfficeContext';

export const OfficeSelectorDropdown: React.FC = () => {
    const { offices, selectedOfficeId, selectOffice, isLoading } = useOfficeContext();

    if (isLoading) {
        return <div className="office-selector" style={{ color: 'var(--color-text-muted)' }}>Loading offices...</div>;
    }

    if (offices.length === 0) {
        return (
            <div className="office-selector no-offices" style={{ padding: '10px', color: 'var(--color-warning)' }}>
                <p>⚠️ You don't have any offices yet. Please create one first.</p>
            </div>
        );
    }

    return (
        <div className="office-selector">
            <label htmlFor="office-select" style={{ color: 'var(--color-text-main)', fontWeight: 'bold' }}>
                Select Office:
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
                <option value="" style={{ background: 'var(--color-bg-dark)' }}>-- Select an Office --</option>
                {offices.map((office) => (
                    <option key={office.id} value={office.id} style={{ background: 'var(--color-bg-dark)' }}>
                        {office.name} ({office.location})
                    </option>
                ))}
            </select>
        </div>
    );
};
