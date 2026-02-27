# SkyPath — Flight Connection Search Engine

A full-stack flight search application that finds optimal itineraries with multi-stop connections, timezone-aware scheduling, and real-time layover validation. Automatically applies domestic and international routing rules.

## Quick Start

```bash
# Clone and run with Docker
git clone <your-repo>
cd flight-search-system
docker-compose up
```

Then open [http://localhost:3000](http://localhost:3000).

### Local Development (without Docker)

**Backend** (requires Java 21+):
```bash
cd backend
mvn spring-boot:run
# API available at http://localhost:8080
```

**Frontend** (requires Node.js 18+):
```bash
cd frontend
npm install
npm run dev
# UI available at http://localhost:3000
```

**Run tests:**
```bash
cd backend
mvn test
```

---

## Architecture

```
┌──────────────────────────────────────────────────────────────┐
│                      Frontend (Next.js)                       │
│   Search Form → API Client → Itinerary Cards                 │
│   Port: 3000                                                 │
└────────────────────────┬─────────────────────────────────────┘
                         │ REST API
┌────────────────────────▼─────────────────────────────────────┐
│                     Backend (Spring Boot)                      │
│                                                               │
│  ┌─────────────┐  ┌───────────────────────┐                  │
│  │  Controller  │→│ FlightSearchOrchestrator│                  │
│  └─────────────┘  └───────────┬───────────┘                  │
│                               │ determines domestic/intl      │
│                    ┌──────────▼──────────┐                    │
│                    │ «interface»          │                    │
│                    │  SearchService       │                    │
│                    ├─────────────────────┤                    │
│                    │  SearchServiceImpl   │                    │
│                    └────────┬─────┬──────┘                    │
│                             │     │                           │
│              ┌──────────────┘     └──────────────┐           │
│              ▼                                    ▼           │
│  ┌────────────────────┐            ┌────────────────────┐    │
│  │ ConnectionValidator │            │ «interface»         │    │
│  └────────────────────┘            │  FlightDataSource   │    │
│                                    ├─────────────────────┤    │
│                                    │InMemoryFlightData.. │    │
│                                    │DatabaseDataSource.. │    │
│                                    └─────────────────────┘    │
│  Port: 8080                                                   │
└───────────────────────────────────────────────────────────────┘
```

### Service Layers

| Layer | Responsibility |
|-------|---------------|
| `FlightSearchController` | Input validation, REST endpoint |
| `FlightSearchOrchestrator` | Classifies route as domestic/international, sets stop limits |
| `SearchService` (interface) | Generic search contract with `isDomestic` support |
| `SearchServiceImpl` | Recursive DFS with domestic airport pruning |
| `ConnectionValidator` | Layover rules (min/max, same airport, domestic vs. international) |
| `TimeZoneService` | UTC conversions and duration calculations |
| `FlightDataSource` (interface) | Abstract data layer (swappable) |

### Key Design Decisions

1. **Abstract `FlightDataSource` interface** — All data access is behind an interface. The current in-memory implementation can be swapped for a database or external API with zero changes to business logic.

2. **Recursive depth-limited DFS search** — The search algorithm is generalized to N hops. It fetches data lazily per-airport as it traverses, so it never needs the full dataset in memory.

3. **Domestic/International routing via `FlightSearchOrchestrator`** — The orchestrator classifies the route by comparing origin and destination countries, then delegates to `SearchService` with the appropriate `maxStops` and `isDomestic` flag. Domestic routes are capped at 1 stop with same-country intermediates only; international routes allow up to 2 stops.

4. **Timezone-correct calculations** — All duration and layover calculations use UTC internally. This handles date-line crossings (e.g., SYD→LAX where arrival appears "before" departure in local time).

5. **Connection validation as a separate service** — Layover rules (45min domestic, 90min international, 6hr max) are encapsulated in `ConnectionValidator`, making them easy to modify or extend.

### Search Algorithm

```
FlightSearchOrchestrator.search(origin, destination, date):
  1. Classify route as domestic or international
  2. Set maxStops = 1 (domestic) or 2 (international)
  3. Call SearchService.search(origin, destination, date, maxStops, isDomestic)

SearchServiceImpl.search(..., isDomestic):
  Resolve domesticCountry from origin airport (if isDomestic=true)
  explore(origin, destination, dateRange, visited={origin},
          path=[], lastFlight=null, remainingStops, domesticCountry)

explore(current, destination, ..., domesticCountry):
  1. Try direct flights current → destination (shortcut query)
  2. If stops remain, fetch flights from current airport
  3. Group by destination, skip foreign intermediates when domesticCountry set
  4. Recurse through valid unvisited intermediates
  5. Backtrack visited set for other branches
```

**Optimizations:**
- Lazy per-airport data fetching (only airports on the path are queried)
- Domestic country pruning at search-time (avoids exploring international branches)
- Group-by-destination to avoid redundant queries
- Early connection pruning (invalid layovers terminate branches)
- Visited-set backtracking (prevents circular routes)

---

## Routing Rules

| Route Type | Max Stops | Intermediate Airports | Min Layover | Max Layover |
|---|---|---|---|---|
| **Domestic** (same country) | 1 | Same country only | 45 min | 6 hours |
| **International** (different countries) | 2 | Any country | 90 min | 6 hours |

Additional rules: no airport changes during layover, no circular routes.

## Data Handling

- Dataset: `flights.json` with 25 airports, ~300 flights
- Flight `SP995` has a typo (`JKF` instead of `JFK`) — skipped with a warning
- Some prices are strings — coerced to `Double` during loading
- Times are in local airport time; UTC is computed on load using timezone metadata

---

## API Reference

### Search Flights
```
GET /api/flights/search?origin=JFK&destination=LAX&date=2024-03-15
```

### List Airports
```
GET /api/airports
```

---

## Testing

Unit tests in `SearchServiceImplTest` cover:

| Scenario | What's Tested |
|---|---|
| Single direct (domestic) | JFK→LAX direct returns 1 result |
| Single direct (international) | JFK→LHR direct returns 1 result |
| Domestic multi-stop | JFK→LAX via ORD: direct + 1-stop both returned |
| Domestic excludes international | JFK→LAX via LHR filtered out in domestic mode |
| International multi-stop | JFK→NRT: direct, 1-stop, 2-stop all found |
| No match (no flights) | Empty results |
| No match (short layover) | 30-min international layover rejected |
| No match (long layover) | 8-hour layover exceeds maximum |

## Tradeoffs

- **In-memory data store** — Fast and simple for the prototype. The `FlightDataSource` interface makes migrating to a database straightforward.
- **No caching layer** — With ~300 flights, queries are sub-millisecond. A cache would add value at larger scale.
- **Recursive DFS vs BFS** — DFS with depth limit is simpler to implement with visited-set backtracking. BFS would be better if we wanted to guarantee shortest-path-first without sorting at the end.
- **No pagination** — Results are returned in full. For larger datasets, cursor-based pagination would be needed.

## What I'd Improve With More Time

- Add a database-backed `FlightDataSource` implementation (PostgreSQL with proper indexes)
- Add result caching with TTL
- Add sort-by-price option on the frontend
- Add a flight map visualization
- Add accessibility improvements (ARIA labels, keyboard navigation throughout)
- Add E2E tests with Playwright

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Backend | Java 21, Spring Boot 3.2, Maven |
| Frontend | Next.js 14, React, TypeScript |
| Testing | JUnit 5, Mockito |
| Containerization | Docker, Docker Compose |
