/**
 * Format minutes into a human-readable duration string.
 * e.g., 375 → "6h 15m", 60 → "1h 0m", 45 → "45m"
 */
export function formatDuration(minutes: number): string {
    const hours = Math.floor(minutes / 60);
    const mins = minutes % 60;
    if (hours === 0) return `${mins}m`;
    return `${hours}h ${mins}m`;
}

/**
 * Format a datetime string into a time display.
 * e.g., "2024-03-15T08:30:00" → "8:30 AM"
 */
export function formatTime(dateTimeStr: string): string {
    const date = new Date(dateTimeStr);
    return date.toLocaleTimeString('en-US', {
        hour: 'numeric',
        minute: '2-digit',
        hour12: true,
    });
}

/**
 * Format a datetime string into a date display.
 * e.g., "2024-03-15T08:30:00" → "Mar 15"
 */
export function formatDate(dateTimeStr: string): string {
    const date = new Date(dateTimeStr);
    return date.toLocaleDateString('en-US', {
        month: 'short',
        day: 'numeric',
    });
}

/**
 * Format price in USD.
 */
export function formatPrice(price: number): string {
    return `$${price.toFixed(2)}`;
}

/**
 * Get a label for the number of stops.
 */
export function getStopsLabel(stops: number): string {
    if (stops === 0) return 'Direct';
    if (stops === 1) return '1 stop';
    return `${stops} stops`;
}
