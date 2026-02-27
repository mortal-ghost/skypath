'use client';

import { useState } from 'react';
import { Itinerary } from '@/lib/types';
import { formatDuration, formatTime, formatPrice, getStopsLabel } from '@/lib/utils';
import styles from './ItineraryCard.module.css';

interface ItineraryCardProps {
    itinerary: Itinerary;
    index: number;
}

export default function ItineraryCard({ itinerary, index }: ItineraryCardProps) {
    const [expanded, setExpanded] = useState(false);
    const { segments, layovers, stops, totalDurationMinutes, totalPrice } = itinerary;

    const firstSegment = segments[0];
    const lastSegment = segments[segments.length - 1];

    return (
        <div
            className={styles.card}
            style={{ animationDelay: `${index * 0.06}s` }}
            onClick={() => setExpanded(!expanded)}
            role="button"
            tabIndex={0}
            onKeyDown={(e) => e.key === 'Enter' && setExpanded(!expanded)}
            id={`itinerary-${index}`}
        >
            {/* Summary row */}
            <div className={styles.summary}>
                <div className={styles.route}>
                    <div className={styles.airport}>
                        <span className={styles.time}>{formatTime(firstSegment.departureTime)}</span>
                        <span className={styles.code}>{firstSegment.originCode}</span>
                    </div>

                    <div className={styles.connector}>
                        <div className={styles.duration}>{formatDuration(totalDurationMinutes)}</div>
                        <div className={styles.line}>
                            <div className={styles.lineBg} />
                            {stops > 0 &&
                                layovers.map((_, i) => (
                                    <div
                                        key={i}
                                        className={styles.dot}
                                        style={{ left: `${((i + 1) / (stops + 1)) * 100}%` }}
                                    />
                                ))}
                            <svg className={styles.plane} viewBox="0 0 24 24" fill="currentColor" width="14" height="14">
                                <path d="M21 16v-2l-8-5V3.5a1.5 1.5 0 00-3 0V9l-8 5v2l8-2.5V19l-2 1.5V22l3.5-1 3.5 1v-1.5L13 19v-5.5l8 2.5z" />
                            </svg>
                        </div>
                        <div className={styles.stopsLabel}>{getStopsLabel(stops)}</div>
                    </div>

                    <div className={styles.airport}>
                        <span className={styles.time}>{formatTime(lastSegment.arrivalTime)}</span>
                        <span className={styles.code}>{lastSegment.destinationCode}</span>
                    </div>
                </div>

                <div className={styles.price}>{formatPrice(totalPrice)}</div>

                <div className={`${styles.expandIcon} ${expanded ? styles.expandedIcon : ''}`}>
                    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                        <polyline points="6 9 12 15 18 9" />
                    </svg>
                </div>
            </div>

            {/* Expanded details */}
            {expanded && (
                <div className={styles.details}>
                    {segments.map((seg, i) => (
                        <div key={i}>
                            <div className={styles.segment}>
                                <div className={styles.segmentHeader}>
                                    <span className={styles.flightNumber}>{seg.flightNumber}</span>
                                    <span className={styles.airline}>{seg.airline}</span>
                                    <span className={styles.aircraft}>{seg.aircraft}</span>
                                </div>
                                <div className={styles.segmentRoute}>
                                    <div className={styles.segmentAirport}>
                                        <span className={styles.segmentTime}>{formatTime(seg.departureTime)}</span>
                                        <span className={styles.segmentCode}>{seg.originCode}</span>
                                        <span className={styles.segmentCity}>{seg.originCity}</span>
                                    </div>
                                    <div className={styles.segmentLine}>
                                        <div className={styles.segmentDuration}>{formatDuration(seg.durationMinutes)}</div>
                                        <div className={styles.segmentLineBg} />
                                    </div>
                                    <div className={styles.segmentAirport}>
                                        <span className={styles.segmentTime}>{formatTime(seg.arrivalTime)}</span>
                                        <span className={styles.segmentCode}>{seg.destinationCode}</span>
                                        <span className={styles.segmentCity}>{seg.destinationCity}</span>
                                    </div>
                                </div>
                            </div>

                            {i < layovers.length && (
                                <div className={styles.layover}>
                                    <div className={styles.layoverIcon}>
                                        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                            <circle cx="12" cy="12" r="10" />
                                            <polyline points="12 6 12 12 16 14" />
                                        </svg>
                                    </div>
                                    <span>
                                        {formatDuration(layovers[i].durationMinutes)} layover in{' '}
                                        <strong>{layovers[i].airportCity}</strong> ({layovers[i].airportCode})
                                    </span>
                                    <span className={styles.layoverType}>{layovers[i].type}</span>
                                </div>
                            )}
                        </div>
                    ))}
                </div>
            )}
        </div>
    );
}
