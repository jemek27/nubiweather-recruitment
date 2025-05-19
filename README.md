# NubiWeather

A simple Spring Boot WebFlux application that fetches current weather and forecast data for a list of cities (Gliwice, Hamburg) or given city, with Redis caching.

## Features

* **Reactive API** using Spring WebClient & WebFlux
* **Endpoints**:

  * `GET /realtime-weather` → Current weather for configured cities
  * `GET /forecast-weather` → 3-day forecast for configured cities
  * `GET /current?city={city}` → Current weather for specified city
  * `GET /forecast?city={city}&days={days}` → Forecast for specified city and number of days
* **Redis caching**:

  * Current weather cached for **15 minutes**
  * Forecast cached for **60 minutes**
* **Dockerized**:
  * `docker-compose.yml` includes Redis and app services

## Quick Start

1. Create `.env` with:

   ```
   WEATHER_API_KEY=<your_api_key>
   ```

2. Run:
   ```
   docker-compose up --build -d
   ```
