export interface Airport {
    code: string;
    name: string;
    city: string;
    country: string;
    timezone: string;
}

export interface FlightSegment {
    flightNumber: string;
    airline: string;
    originCode: string;
    originName: string;
    originCity: string;
    destinationCode: string;
    destinationName: string;
    destinationCity: string;
    departureTime: string;
    arrivalTime: string;
    durationMinutes: number;
    aircraft: string;
}

export interface Layover {
    airportCode: string;
    airportName: string;
    airportCity: string;
    durationMinutes: number;
    type: 'domestic' | 'international';
}

export interface Itinerary {
    segments: FlightSegment[];
    layovers: Layover[];
    stops: number;
    totalDurationMinutes: number;
    totalPrice: number;
}

export interface SearchResponse {
    origin: string;
    destination: string;
    date: string;
    resultCount: number;
    itineraries: Itinerary[];
}

export interface ErrorResponse {
    status: number;
    error: string;
    message: string;
    timestamp: string;
}
