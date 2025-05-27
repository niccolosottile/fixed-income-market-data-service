# Configuration Management

## Environment Setup

This application uses environment variables for sensitive configuration like API keys. 

### Local Development

1. Copy `.env.example` to `.env`
2. Fill in your actual FRED API key in the `.env` file
3. The `.env` file is automatically ignored by git

### Running the Application

**Development:**
```bash
# Set environment variables from .env file (if using direnv)
direnv allow

# Or export manually
export FRED_API_KEY=your_actual_api_key_here

# Run the application
./mvnw spring-boot:run
```

**Production:**
```bash
# Set environment variables
export FRED_API_KEY=prod_api_key
export DATABASE_URL=prod_db_url
export DATABASE_USERNAME=prod_user
export DATABASE_PASSWORD=prod_password

# Run with production profile
./mvnw spring-boot:run -Dspring.profiles.active=prod
```

## Configuration Files

- **application.properties**: Common settings, non-sensitive configuration
- **application-dev.properties**: Development-specific settings
- **application-prod.properties**: Production-specific settings
- **.env**: Local development secrets
- **.env.example**: Template showing required environment variables

## FRED API Key Management

Each environment should have its own FRED API key:
- Development: Personal API key in `.env` file
- Production: Separate API key per client/deployment
- Use cloud provider secret management for production

## Alternative Data Source Configuration

The application includes an alternative source for market data that can complement or replace FRED when needed.

### Configuration Properties

```properties
# Alternative Data API Configuration
alternative.api.enabled=false                    # Disabled by default
alternative.api.base-url=${ALTERNATIVE_API_URL:} # API endpoint
alternative.api.api-key=${ALTERNATIVE_API_KEY:}  # API authentication
alternative.api.use-fallback-data=true           # Enable fallback data
```