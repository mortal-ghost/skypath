'use client';

import { useState, useEffect } from 'react';
import { Airport } from '@/lib/types';
import { getAirports } from '@/lib/api';
import AirportInput from './AirportInput';
import styles from './SearchForm.module.css';

interface SearchFormProps {
    onSearch: (origin: string, destination: string, date: string) => void;
    isLoading: boolean;
}

export default function SearchForm({ onSearch, isLoading }: SearchFormProps) {
    const [airports, setAirports] = useState<Airport[]>([]);
    const [origin, setOrigin] = useState('');
    const [destination, setDestination] = useState('');
    const [date, setDate] = useState('2024-03-15');
    const [error, setError] = useState('');

    useEffect(() => {
        getAirports()
            .then(setAirports)
            .catch((err) => console.error('Failed to load airports:', err));
    }, []);

    const handleSwap = () => {
        setOrigin(destination);
        setDestination(origin);
    };

    const handleSubmit = (e: React.FormEvent) => {
        e.preventDefault();
        setError('');

        if (!origin) {
            setError('Please select an origin airport.');
            return;
        }
        if (!destination) {
            setError('Please select a destination airport.');
            return;
        }
        if (!date) {
            setError('Please select a travel date.');
            return;
        }
        if (origin === destination) {
            setError('Origin and destination cannot be the same.');
            return;
        }

        onSearch(origin, destination, date);
    };

    return (
        <form onSubmit={handleSubmit} className={styles.form}>
            <div className={styles.header}>
                <div className={styles.iconWrapper}>
                    <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5">
                        <path d="M21 16v-2l-8-5V3.5a1.5 1.5 0 00-3 0V9l-8 5v2l8-2.5V19l-2 1.5V22l3.5-1 3.5 1v-1.5L13 19v-5.5l8 2.5z" />
                    </svg>
                </div>
                <h2 className={styles.title}>Search Flights</h2>
            </div>

            <div className={styles.fields}>
                <AirportInput
                    id="origin-input"
                    label="From"
                    value={origin}
                    onChange={setOrigin}
                    airports={airports}
                    placeholder="e.g. JFK, New York"
                />

                <button
                    type="button"
                    className={styles.swapBtn}
                    onClick={handleSwap}
                    aria-label="Swap origin and destination"
                    title="Swap"
                >
                    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                        <path d="M7 16l-4-4m0 0l4-4m-4 4h18M17 8l4 4m0 0l-4 4m4-4H3" />
                    </svg>
                </button>

                <AirportInput
                    id="destination-input"
                    label="To"
                    value={destination}
                    onChange={setDestination}
                    airports={airports}
                    placeholder="e.g. LAX, Los Angeles"
                />

                <div className={styles.dateWrapper}>
                    <label htmlFor="date-input" className={styles.dateLabel}>
                        Date
                    </label>
                    <input
                        id="date-input"
                        type="date"
                        className={styles.dateInput}
                        value={date}
                        onChange={(e) => setDate(e.target.value)}
                    />
                </div>

                <button
                    type="submit"
                    className={styles.searchBtn}
                    disabled={isLoading}
                    id="search-button"
                >
                    {isLoading ? (
                        <span className={styles.spinner} />
                    ) : (
                        <>
                            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5">
                                <circle cx="11" cy="11" r="8" />
                                <path d="m21 21-4.35-4.35" />
                            </svg>
                            Search
                        </>
                    )}
                </button>
            </div>

            {error && (
                <div className={styles.error} role="alert">
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                        <circle cx="12" cy="12" r="10" />
                        <line x1="12" y1="8" x2="12" y2="12" />
                        <line x1="12" y1="16" x2="12.01" y2="16" />
                    </svg>
                    {error}
                </div>
            )}
        </form>
    );
}
