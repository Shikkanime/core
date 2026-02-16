<p>
    <img src="src/main/resources/assets/img/dark_banner.webp" width="700" height="93" alt="Shikkanime Dark Banner">
    <br>
    <br>
    <a href="https://www.shikkanime.fr">Website</a> •
    <a href="#-features">Features</a> •
    <a href="#-getting-started">Getting Started</a>
    <br>
    <img src="https://img.shields.io/github/v/tag/Shikkanime/core" alt="Version">
    <img src="" alt="Code Coverage">
    <img src="https://img.shields.io/github/license/Shikkanime/core" alt="License">
</p>

**Shikkanime** is an anime tracking platform that monitors and aggregates information about anime episodes across multiple streaming services. This repository contains the core backend service that powers the Shikkanime platform.

## 📋 Features

### 📺 Multi-platform Support
Tracks anime episodes from multiple streaming platforms:
- **Crunchyroll** - Global leader in anime streaming
- **Netflix** - Popular streaming service with a growing anime selection
- **Animation Digital Network (ADN)** - French platform specialized in anime
- **Disney Plus** - Offering a selection of anime and animated content
- **Prime Video** - Amazon's streaming service with an anime collection

### 🔄 Real-time Updates
- Automatic retrieval of information about new episodes
- Updating anime data based on latest releases
- Continuous monitoring of platforms to detect new content

### 📱 Social Network Integration
Sends notifications about new episodes to:
- **Discord** - Instant notifications on Discord servers
- **Twitter** - Automated tweets for new releases
- **Threads** - Posts on the Meta Threads platform
- **Bluesky** - Sharing on the decentralized social network
- **Firebase** - Mobile push notifications for app users

### 🔌 API Access
- Complete RESTful API for accessing anime and episode data
- Secure endpoints with JWT authentication

### 👤 User Management
- Robust authentication and authorization system
- User profile management
- Personalized preferences for anime tracking

### 🔍 Search Capabilities
- Advanced search functionality for finding anime
- Filtering by season, platform, etc.
- Optimized text search

## 🚀 Getting Started

### Prerequisites

- **JDK 25** or higher
- **PostgreSQL** database
- **Docker** (optional, for containerized deployment)

### Environment Variables

The application can be configured using the following environment variables:

```properties
# JWT Configuration
JWT_AUDIENCE - JWT audience (default: jwt-audience)
JWT_DOMAIN - JWT domain (default: https://jwt-provider-domain/)
JWT_REALM - JWT realm (default: ktor sample app)
JWT_SECRET - JWT secret (default: secret)

# Application URLs
API_URL - API URL (default: http://localhost:37100/api)
BASE_URL - Base URL (default: http://localhost:37100)
```

### Running Locally

1. **Clone the repository**:
   ```bash
   git clone https://github.com/Shikkanime/core.git
   cd core
   ```

2. **Build the project**:
   ```bash
   ./gradlew build
   ```

3. **Run the application**:
   ```bash
   ./gradlew run --args="--enable-jobs"
   ```

   The `--enable-jobs` flag enables scheduled jobs for fetching and updating anime data.

### Docker Deployment

1. **Build the Docker image**:
   ```bash
   docker build -t shikkanime-core .
   ```

2. **Run the container**:
   ```bash
   docker run -p 37100:37100 -e JWT_SECRET=your_secret shikkanime-core
   ```

Alternatively, use the provided docker-compose.yml:
```bash
docker-compose up -d
```

## 📂 Project Structure

- `src/main/kotlin/fr/shikkanime/`
  - `Application.kt` - Main application entry point
  - `controllers/` - API controllers
  - `services/` - Business logic services
  - `repositories/` - Data access layer
  - `entities/` - Database entity models
  - `platforms/` - Streaming platform integrations
  - `socialnetworks/` - Social network integrations
  - `jobs/` - Scheduled background jobs
  - `utils/` - Utility classes

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## 📄 License

This project is licensed under the terms of the license included in the repository.
