# Fixed Income Market Data Service

A Spring Boot service for fetching market data, calculating bond valuations, and computing risk metrics (duration, convexity) for fixed income instruments.

## Tech Stack

- Java 25, Spring Boot
- H2 (dev) with JPA
- FRED API integration for market data
- OpenAPI/Swagger documentation

## Quick Start
```bash
# Set your FRED API key (get one at https://fred.stlouisfed.org/docs/api/api_key.html)
export FRED_API_KEY=your_key_here

# Build and run
mvn clean install
mvn spring-boot:run
```

The app runs on `http://localhost:8080`. API docs available at `/swagger-ui/index.html`.

## API Authentication

Requests require an `X-API-Key` header. For local development: `default-api-key-for-dev`

## Development

H2 console available at `/h2-console` (JDBC URL: `jdbc:h2:mem:marketdatadb`, user: `sa`, password: `password`)
```bash
mvn test
```

## License
MIT
