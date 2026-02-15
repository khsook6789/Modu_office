import React from 'react';
import { useOfficeContext } from '../contexts/OfficeContext';

export const OfficeSelectorDropdown: React.FC = () => {
    const { offices, selectedOfficeId, selectOffice, isLoading } = useOfficeContext();

    if (isLoading) {
        return <div className="office-selector">Loading offices...</div>;
    }

    if (offices.length === 0) {
        return (
            <div className="office-selector no-offices">
                <p>⚠️ You don't have any offices yet. Please create one first.</p>
            </div>
        );
    }

    return (
        <div className="office-selector">
            <label htmlFor="office-select">
                <strong>Select Office:</strong>
            </label>
            <select
                id="office-select"
                value={selectedOfficeId || ''}
                onChange={(e) => selectOffice(Number(e.target.value))}
                style={{
                    marginLeft: '10px',
                    padding: '8px 12px',
                    fontSize: '14px',
                    borderRadius: '4px',
                    border: '1px solid #ccc',
                }}
            >
                <option value="">-- Select an Office --</option>
                {offices.map((office) => (
                    <option key={office.id} value={office.id}>
                        {office.name} ({office.location})
                    </option>
                ))}
            </select>
        </div>
    );
};
