# SkyPath — Flight Connection Search Engine

A full-stack flight search application that finds optimal itineraries with multi-stop connections, timezone-aware scheduling, and real-time layover validation.

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

---

## Architecture

```
┌─────────────────────────────────────────────────────────┐
│                    Frontend (Next.js)                     │
│  Search Form → API Client → Itinerary Cards              │
│  Port: 3000                                              │
└──────────────────────┬──────────────────────────────────┘
                       │ REST API
┌──────────────────────▼──────────────────────────────────┐
│                   Backend (Spring Boot)                   │
│                                                           │
│  ┌─────────────┐  ┌──────────────┐  ┌────────────────┐  │
│  │  Controller  │→│ SearchService │→│ ConnectionValid.│  │
│  └─────────────┘  └──────┬───────┘  └───────┬────────┘  │
│                          │                   │           │
│             ┌────────────▼───────────────────▼──────┐    │
│             │   «interface» FlightDataSource         │    │
│             ├────────────────────────────────────────┤    │
│             │ InMemoryFlightDataSource (current)     │    │
│             │ DatabaseDataSource (future)             │    │
│             │ ApiDataSource (future)                  │    │
│             └────────────────────────────────────────┘    │
│  Port: 8080                                              │
└──────────────────────────────────────────────────────────┘
```

### Key Design Decisions

1. **Abstract `FlightDataSource` interface** — All data access is behind an interface. The current in-memory implementation can be swapped for a database or external API with zero changes to business logic.

2. **Recursive depth-limited DFS search** — The search algorithm is generalized to N hops (configurable `max-stops` in `application.yml`). It fetches data lazily per-airport as it traverses, so it never needs the full dataset in memory.

3. **Timezone-correct calculations** — All duration and layover calculations use UTC internally. This handles date-line crossings (e.g., SYD→LAX where arrival appears "before" departure in local time).

4. **Connection validation as a separate service** — Layover rules (45min domestic, 90min international, 6hr max) are encapsulated in `ConnectionValidator`, making them easy to modify or extend.

### Search Algorithm

```
search(origin, destination, date):
  explore(origin, destination, dateRange, visited={origin},
          path=[], lastFlight=null, remainingStops=maxStops)

explore(current, destination, ...):
  1. Try direct flights current → destination (shortcut query)
  2. If stops remain, fetch flights from current airport
  3. Group by destination, recurse through unvisited intermediates
  4. Backtrack visited set for other branches
```

**Optimizations:**
- Lazy per-airport data fetching (only airports on the path are queried)
- Group-by-destination to avoid redundant queries
- Early connection pruning (invalid layovers terminate branches)
- Visited-set backtracking (prevents circular routes)

---

## Data Handling

- Dataset: `flights.json` with 25 airports, ~300 flights
- Flight `SP995` has a typo (`JKF` instead of `JFK`) — skipped with a warning
- Some prices are strings — coerced to `Double` during loading
- Times are in local airport time; UTC is computed on load using timezone metadata

## Connection Rules

| Rule | Value |
|------|-------|
| Min layover (domestic) | 45 minutes |
| Min layover (international) | 90 minutes |
| Max layover | 6 hours |
| Airport changes during layover | Not allowed |
| Domestic definition | Both arriving and departing flights are within the same country |

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

## Tradeoffs

- **In-memory data store** — Fast and simple for the prototype. The `FlightDataSource` interface makes migrating to a database straightforward.
- **No caching layer** — With ~300 flights, queries are sub-millisecond. A cache would add value at larger scale.
- **Recursive DFS vs BFS** — DFS with depth limit is simpler to implement with visited-set backtracking. BFS would be better if we wanted to guarantee shortest-path-first without sorting at the end.
- **No pagination** — Results are returned in full. For larger datasets, cursor-based pagination would be needed.

## What I'd Improve With More Time

- Add a database-backed `FlightDataSource` implementation (PostgreSQL with proper indexes)
- Add comprehensive unit + integration tests
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
| Containerization | Docker, Docker Compose |
