import { Airport, SearchResponse, ErrorResponse } from './types';

const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080';

class ApiError extends Error {
    status: number;
    errorResponse: ErrorResponse;

    constructor(response: ErrorResponse) {
        super(response.message);
        this.status = response.status;
        this.errorResponse = response;
        this.name = 'ApiError';
    }
}

async function handleResponse<T>(response: Response): Promise<T> {
    if (!response.ok) {
        let errorResponse: ErrorResponse;
        try {
            errorResponse = await response.json();
        } catch {
            errorResponse = {
                status: response.status,
                error: response.statusText,
                message: 'An unexpected error occurred. Please try again.',
                timestamp: new Date().toISOString(),
            };
        }
        throw new ApiError(errorResponse);
    }
    return response.json();
}

export async function searchFlights(
    origin: string,
    destination: string,
    date: string
): Promise<SearchResponse> {
    const params = new URLSearchParams({ origin, destination, date });
    const response = await fetch(`${API_BASE_URL}/api/flights/search?${params}`);
    return handleResponse<SearchResponse>(response);
}

export async function getAirports(): Promise<Airport[]> {
    const response = await fetch(`${API_BASE_URL}/api/airports`);
    return handleResponse<Airport[]>(response);
}

export { ApiError };
