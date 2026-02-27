'use client';

import { useState } from 'react';
import SearchForm from '@/components/SearchForm';
import ItineraryCard from '@/components/ItineraryCard';
import { searchFlights, ApiError } from '@/lib/api';
import { Itinerary } from '@/lib/types';
import { getStopsLabel } from '@/lib/utils';
import styles from './page.module.css';

type SearchState =
  | { kind: 'idle' }
  | { kind: 'loading' }
  | { kind: 'success'; itineraries: Itinerary[]; origin: string; destination: string }
  | { kind: 'error'; message: string };

export default function Home() {
  const [state, setState] = useState<SearchState>({ kind: 'idle' });
  const [filter, setFilter] = useState<'all' | 'direct' | '1stop' | '2stop'>('all');

  const handleSearch = async (origin: string, destination: string, date: string) => {
    setState({ kind: 'loading' });
    try {
      const response = await searchFlights(origin, destination, date);
      setState({
        kind: 'success',
        itineraries: response.itineraries,
        origin: response.origin,
        destination: response.destination,
      });
      setFilter('all');
    } catch (err) {
      if (err instanceof ApiError) {
        setState({ kind: 'error', message: err.message });
      } else {
        setState({ kind: 'error', message: 'Failed to connect to the server. Please try again.' });
      }
    }
  };

  const filteredItineraries =
    state.kind === 'success'
      ? state.itineraries.filter((it) => {
        if (filter === 'all') return true;
        if (filter === 'direct') return it.stops === 0;
        if (filter === '1stop') return it.stops === 1;
        if (filter === '2stop') return it.stops >= 2;
        return true;
      })
      : [];

  return (
    <main className={styles.main}>
      <div className={styles.container}>
        {/* Hero header */}
        <header className={styles.hero}>
          <div className={styles.logo}>
            <svg width="36" height="36" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5">
              <path d="M21 16v-2l-8-5V3.5a1.5 1.5 0 00-3 0V9l-8 5v2l8-2.5V19l-2 1.5V22l3.5-1 3.5 1v-1.5L13 19v-5.5l8 2.5z" />
            </svg>
          </div>
          <h1 className={styles.heroTitle}>SkyPath</h1>
          <p className={styles.heroSub}>Find the best flight connections worldwide</p>
        </header>

        {/* Search form */}
        <SearchForm onSearch={handleSearch} isLoading={state.kind === 'loading'} />

        {/* Results */}
        <section className={styles.results}>
          {state.kind === 'loading' && (
            <div className={styles.loadingState}>
              <div className={styles.loadingPlane}>
                <svg width="40" height="40" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5">
                  <path d="M21 16v-2l-8-5V3.5a1.5 1.5 0 00-3 0V9l-8 5v2l8-2.5V19l-2 1.5V22l3.5-1 3.5 1v-1.5L13 19v-5.5l8 2.5z" />
                </svg>
              </div>
              <p>Searching for the best routes...</p>
            </div>
          )}

          {state.kind === 'error' && (
            <div className={styles.errorState}>
              <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <circle cx="12" cy="12" r="10" />
                <line x1="12" y1="8" x2="12" y2="12" />
                <line x1="12" y1="16" x2="12.01" y2="16" />
              </svg>
              <p>{state.message}</p>
            </div>
          )}

          {state.kind === 'success' && state.itineraries.length === 0 && (
            <div className={styles.emptyState}>
              <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5">
                <path d="M9.172 16.172a4 4 0 015.656 0M9 10h.01M15 10h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
              </svg>
              <h3>No flights found</h3>
              <p>
                No itineraries available from {state.origin} to {state.destination} on this date.
                Try different airports or a different date.
              </p>
            </div>
          )}

          {state.kind === 'success' && state.itineraries.length > 0 && (
            <>
              <div className={styles.resultsHeader}>
                <h2 className={styles.resultsTitle}>
                  {state.origin} → {state.destination}
                  <span className={styles.resultCount}>{state.itineraries.length} itineraries</span>
                </h2>
                <div className={styles.filters}>
                  {(['all', 'direct', '1stop', '2stop'] as const).map((f) => {
                    const count =
                      f === 'all'
                        ? state.itineraries.length
                        : state.itineraries.filter((it) =>
                          f === 'direct' ? it.stops === 0 : f === '1stop' ? it.stops === 1 : it.stops >= 2
                        ).length;
                    if (count === 0 && f !== 'all') return null;
                    return (
                      <button
                        key={f}
                        className={`${styles.filterBtn} ${filter === f ? styles.filterActive : ''}`}
                        onClick={() => setFilter(f)}
                      >
                        {f === 'all' ? 'All' : f === 'direct' ? 'Direct' : getStopsLabel(f === '1stop' ? 1 : 2)}
                        <span className={styles.filterCount}>{count}</span>
                      </button>
                    );
                  })}
                </div>
              </div>

              <div className={styles.itineraryList}>
                {filteredItineraries.map((itinerary, index) => (
                  <ItineraryCard key={index} itinerary={itinerary} index={index} />
                ))}
                {filteredItineraries.length === 0 && (
                  <p className={styles.noFilterResults}>No itineraries match this filter.</p>
                )}
              </div>
            </>
          )}
        </section>
      </div>
    </main>
  );
}
