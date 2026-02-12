import { client } from '../../../api/client';

export interface Office {
    id: number;
    name: string;
    location: string;
    latitude: number;
    longitude: number;
    openTime: string;
    closeTime: string;
}

// LocalStorage Keys
const OFFICE_STORAGE_KEY = 'modu_offices';

// Initial Seed Data
const INITIAL_OFFICES: Office[] = [
    { id: 1, name: '강남 공유오피스', location: '서울시 강남구 테헤란로 123', latitude: 37.4979, longitude: 127.0276, openTime: '09:00', closeTime: '22:00' },
    { id: 2, name: '판교 스타트업 허브', location: '성남시 분당구 판교역로 456', latitude: 37.3947, longitude: 127.1111, openTime: '08:00', closeTime: '20:00' },
];

// Helper to get offices from storage
const getStoredOffices = (): Office[] => {
    const stored = localStorage.getItem(OFFICE_STORAGE_KEY);
    if (!stored) {
        localStorage.setItem(OFFICE_STORAGE_KEY, JSON.stringify(INITIAL_OFFICES));
        return INITIAL_OFFICES;
    }
    return JSON.parse(stored);
};

export const officeApi = {
    getAllOffices: () => {
        // Simulate network delay
        return new Promise<Office[]>((resolve) => {
            setTimeout(() => {
                resolve(getStoredOffices());
            }, 300);
        });
    },
    getOfficeById: (id: string | number) => {
        return new Promise<Office>((resolve, reject) => {
            setTimeout(() => {
                const offices = getStoredOffices();
                const office = offices.find(o => o.id === Number(id));
                if (office) resolve(office);
                else reject(new Error('Office not found'));
            }, 300);
        });
    },
    createOffice: (data: Omit<Office, 'id'>) => {
        return new Promise<Office>((resolve) => {
            setTimeout(() => {
                const offices = getStoredOffices();
                const newId = offices.length > 0 ? Math.max(...offices.map(o => o.id)) + 1 : 1;
                const newOffice = { id: newId, ...data };
                localStorage.setItem(OFFICE_STORAGE_KEY, JSON.stringify([...offices, newOffice]));
                resolve(newOffice);
            }, 500);
        });
    },
    updateOffice: (id: string | number, data: Partial<Office>) => {
        return new Promise<Office>((resolve, reject) => {
            setTimeout(() => {
                const offices = getStoredOffices();
                const index = offices.findIndex(o => o.id === Number(id));
                if (index !== -1) {
                    const updatedOffice = { ...offices[index], ...data };
                    offices[index] = updatedOffice;
                    localStorage.setItem(OFFICE_STORAGE_KEY, JSON.stringify(offices));
                    resolve(updatedOffice);
                } else {
                    reject(new Error('Office not found'));
                }
            }, 500);
        });
    }
};
