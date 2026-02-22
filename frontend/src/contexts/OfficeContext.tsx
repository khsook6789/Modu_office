import React, { createContext, useContext, useState, useEffect, type ReactNode } from 'react';
import { officeApi, type Office } from '../features/rooms/api/office.api';
import { useAuth } from './AuthContext';

interface OfficeContextType {
    offices: Office[];
    selectedOfficeId: number | null;
    selectedOffice: Office | null;
    isLoading: boolean;
    error: string | null;
    selectOffice: (id: number) => void;
    refreshOffices: () => Promise<void>;
    createOffice: (data: Omit<Office, 'id'>) => Promise<Office>;
    deleteOffice: (id: number) => Promise<void>;
}

const OfficeContext = createContext<OfficeContextType | undefined>(undefined);

export const useOfficeContext = () => {
    const context = useContext(OfficeContext);
    if (!context) {
        throw new Error('useOfficeContext must be used within OfficeProvider');
    }
    return context;
};

interface OfficeProviderProps {
    children: ReactNode;
}

export const OfficeProvider: React.FC<OfficeProviderProps> = ({ children }) => {
    const { user } = useAuth();
    const [offices, setOffices] = useState<Office[]>([]);
    const [selectedOfficeId, setSelectedOfficeId] = useState<number | null>(null);
    const [isLoading, setIsLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);

    // Load user's offices on mount
    useEffect(() => {
        loadOffices();
    }, []);

    const loadOffices = async () => {
        try {
            setIsLoading(true);
            setError(null);
            // OPERATOR/ADMIN은 자신의 오피스만, CUSTOMER는 전체 오피스 조회
            const isOperator = user?.role === 'OPERATOR' || user?.role === 'PLATFORM_ADMIN';
            const data = isOperator
                ? await officeApi.getMyOffices()
                : await officeApi.getAllOffices();
            setOffices(data);
            
            // Auto-select first office if available
            if (data.length > 0 && !selectedOfficeId) {
                setSelectedOfficeId(data[0].id);
            }
        } catch (err: any) {
            console.error('Failed to load offices:', err);
            setError(err.message || 'Failed to load offices');
        } finally {
            setIsLoading(false);
        }
    };

    const selectOffice = (id: number) => {
        setSelectedOfficeId(id);
    };

    const refreshOffices = async () => {
        await loadOffices();
    };

    const createOffice = async (data: Omit<Office, 'id'>): Promise<Office> => {
        try {
            const newOffice = await officeApi.createOffice(data);
            await refreshOffices();
            setSelectedOfficeId(newOffice.id); // Auto-select newly created office
            return newOffice;
        } catch (err: any) {
            console.error('Failed to create office:', err);
            throw err;
        }
    };

    const deleteOffice = async (id: number): Promise<void> => {
        try {
            await officeApi.deleteOffice(id);
            setOffices(prev => prev.filter(o => o.id !== id));
            if (selectedOfficeId === id) {
                setSelectedOfficeId(null);
            }
        } catch (err: any) {
            console.error('Failed to delete office:', err);
            throw err;
        }
    };

    const selectedOffice = offices.find(o => o.id === selectedOfficeId) || null;

    const value: OfficeContextType = {
        offices,
        selectedOfficeId,
        selectedOffice,
        isLoading,
        error,
        selectOffice,
        refreshOffices,
        createOffice,
        deleteOffice,
    };

    return (
        <OfficeContext.Provider value={value}>
            {children}
        </OfficeContext.Provider>
    );
};
