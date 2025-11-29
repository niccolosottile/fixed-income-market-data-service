# Fixed Income Market Data Service

REST API for market data retrieval with layered caching, database persistence, and external provider integration.

## Architecture

The service implements a four-tier data flow to balance latency and reliability:

1. **Caffeine cache** - sub-millisecond response for repeated queries
2. **H2 database** - persistent storage for historical data
3. **FRED API client** - external market data provider
4. **Fallback service** - configurable fallback providers when default unavailable

Each layer delegates to the next if data isn't found, with automatic persistence of fetched data for future requests.

## Tech Stack

- **Spring Boot 3.4** with Java 24
- **Spring Data JPA** + H2 (dev), designed for PostgreSQL (prod)
- **Caffeine** for distributed caching
- **Spring Security** with API key authentication
- **OpenAPI 3.0** documentation

## Features

- Treasury yield curves (current and historical)
- Credit spreads by sector and rating
- Benchmark rates (SOFR, Fed Funds, LIBOR equivalents)
- Time series data for any tenor
- Test endpoints to verify data flow through each layer

## Setup

```bash
# Get a FRED API key from https://fred.stlouisfed.org/docs/api/api_key.html
export FRED_API_KEY=your_key_here

mvn clean install
mvn spring-boot:run
```

Access Swagger UI at `http://localhost:8080/swagger-ui/index.html`

Default API key for local dev: `default-api-key-for-dev`

## Example Usage

```bash
# Latest Treasury curve
curl -H "X-API-Key: default-api-key-for-dev" \
  http://localhost:8080/api/v1/market-data/yield-curves/latest

# Time series for 10Y yields
curl -H "X-API-Key: default-api-key-for-dev" \
  "http://localhost:8080/api/v1/market-data/time-series/10Y?startDate=2024-01-01&endDate=2024-12-31"

# Test cache/database/provider flow
curl -H "X-API-Key: default-api-key-for-dev" \
  http://localhost:8080/api/v1/test/scenarios/cold-start
```

## Testing

Service-layer tests cover the data flow logic and provider integration:

```bash
mvn test

## Development

H2 console: `http://localhost:8080/h2-console`
- JDBC URL: `jdbc:h2:mem:marketdatadb`
- User: `sa`
- Password: `password`

See `CONFIG.md` for environment-specific configuration.
