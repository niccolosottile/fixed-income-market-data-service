# Fixed Income Market Data Service
A Spring Boot application that provides market data and valuation services for fixed income assets.

## Overview
This service is designed to:
- Fetch and store market data for fixed income instruments
- Calculate accurate valuations for bonds and other fixed income assets
- Provide risk metrics like duration and convexity
- Expose a RESTful API for integration with other applications

## Project Structure
The application follows a layered architecture:
- **Controllers**: REST API endpoints
- **Services**: Business logic implementation
- **Repositories**: Data access layer
- **Models**: Domain entities
- **DTOs**: Data transfer objects
- **Config**: Application configuration
- **Exception**: Exception handling

## Getting Started

### Prerequisites
- JDK 17 or higher
- Maven 3.6 or higher
- Git
- FRED API Key (from https://fred.stlouisfed.org/docs/api/api_key.html)

### Setup Instructions
1. Clone the repository:
```bash
git clone https://github.com/niccolosottile/fixed-income-market-data-service.git
cd fixed-income-market-data-service
```

2. Set up environment variables:
```bash
# For Linux/macOS
export FRED_API_KEY=your_fred_api_key_here

# For Windows
set FRED_API_KEY=your_fred_api_key_here
```

3. Build the project:
```bash
mvn clean install
```

4. Run the application:
```bash
mvn spring-boot:run
```

The application will start on `http://localhost:8080`

### API Documentation
Once the application is running, you can access the Swagger UI at:
```
http://localhost:8080/swagger-ui/index.html
```

## Development

### Environment Variables
The application requires the following environment variables:
- `FRED_API_KEY`: Your API key for the Federal Reserve Economic Data (FRED) API

For local development, you can also set these in your IDE's run configuration.

### Database
The application uses H2 in-memory database for development. The H2 console is available at:
```
http://localhost:8080/h2-console
```

Connection details:
- JDBC URL: `jdbc:h2:mem:marketdatadb`
- Username: `sa`
- Password: `password`

### Testing
Run the tests with:
```bash
mvn test
```

## API Key Authentication
The API is secured with API key authentication. For development, you can use the default API key:
```
X-API-Key: default-api-key-for-dev
```

For production, set a secure API key in the application properties or environment variables.

## License
[MIT License](LICENSE)
