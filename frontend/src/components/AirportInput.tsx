'use client';

import { useState, useEffect, useRef, useCallback } from 'react';
import { Airport } from '@/lib/types';
import styles from './AirportInput.module.css';

interface AirportInputProps {
    label: string;
    value: string;
    onChange: (code: string) => void;
    airports: Airport[];
    placeholder?: string;
    id: string;
}

export default function AirportInput({
    label,
    value,
    onChange,
    airports,
    placeholder = 'Airport code',
    id,
}: AirportInputProps) {
    const [query, setQuery] = useState('');
    const [isOpen, setIsOpen] = useState(false);
    const [filtered, setFiltered] = useState<Airport[]>([]);
    const [highlightIndex, setHighlightIndex] = useState(-1);
    const wrapperRef = useRef<HTMLDivElement>(null);
    const inputRef = useRef<HTMLInputElement>(null);

    // Sync display text with value from parent
    useEffect(() => {
        if (value) {
            const airport = airports.find((a) => a.code === value);
            if (airport) {
                setQuery(`${airport.code} — ${airport.city}`);
            } else {
                setQuery(value);
            }
        } else {
            setQuery('');
        }
    }, [value, airports]);

    const filterAirports = useCallback(
        (q: string) => {
            if (!q.trim()) return airports.slice(0, 10);
            const lower = q.toLowerCase();
            return airports.filter(
                (a) =>
                    a.code.toLowerCase().includes(lower) ||
                    a.city.toLowerCase().includes(lower) ||
                    a.name.toLowerCase().includes(lower)
            );
        },
        [airports]
    );

    const handleInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        const val = e.target.value;
        setQuery(val);
        setIsOpen(true);
        setFiltered(filterAirports(val));
        setHighlightIndex(-1);
        // If user clears input, clear the value
        if (!val.trim()) {
            onChange('');
        }
    };

    const handleSelect = (airport: Airport) => {
        onChange(airport.code);
        setQuery(`${airport.code} — ${airport.city}`);
        setIsOpen(false);
        setHighlightIndex(-1);
    };

    const handleFocus = () => {
        setIsOpen(true);
        setFiltered(filterAirports(query));
    };

    const handleKeyDown = (e: React.KeyboardEvent) => {
        if (!isOpen) return;
        if (e.key === 'ArrowDown') {
            e.preventDefault();
            setHighlightIndex((prev) => Math.min(prev + 1, filtered.length - 1));
        } else if (e.key === 'ArrowUp') {
            e.preventDefault();
            setHighlightIndex((prev) => Math.max(prev - 1, 0));
        } else if (e.key === 'Enter' && highlightIndex >= 0) {
            e.preventDefault();
            handleSelect(filtered[highlightIndex]);
        } else if (e.key === 'Escape') {
            setIsOpen(false);
        }
    };

    // Close dropdown on outside click
    useEffect(() => {
        const handleClickOutside = (e: MouseEvent) => {
            if (wrapperRef.current && !wrapperRef.current.contains(e.target as Node)) {
                setIsOpen(false);
            }
        };
        document.addEventListener('mousedown', handleClickOutside);
        return () => document.removeEventListener('mousedown', handleClickOutside);
    }, []);

    return (
        <div className={styles.wrapper} ref={wrapperRef}>
            <label htmlFor={id} className={styles.label}>
                {label}
            </label>
            <input
                ref={inputRef}
                id={id}
                type="text"
                className={styles.input}
                value={query}
                onChange={handleInputChange}
                onFocus={handleFocus}
                onKeyDown={handleKeyDown}
                placeholder={placeholder}
                autoComplete="off"
            />
            {isOpen && filtered.length > 0 && (
                <ul className={styles.dropdown} role="listbox">
                    {filtered.map((airport, idx) => (
                        <li
                            key={airport.code}
                            className={`${styles.option} ${idx === highlightIndex ? styles.highlighted : ''}`}
                            onClick={() => handleSelect(airport)}
                            role="option"
                            aria-selected={idx === highlightIndex}
                        >
                            <span className={styles.code}>{airport.code}</span>
                            <span className={styles.details}>
                                {airport.city} — {airport.name}
                            </span>
                            <span className={styles.country}>{airport.country}</span>
                        </li>
                    ))}
                </ul>
            )}
        </div>
    );
}
