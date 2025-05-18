# Etap 1: Budowanie aplikacji
FROM gradle:8.4-jdk17-alpine AS build
WORKDIR /app
COPY . .
ARG WEATHER_API_KEY
ENV WEATHER_API_KEY=$WEATHER_API_KEY
RUN gradle build --no-daemon

# Etap 2: Finalny obraz z JAR-em
FROM eclipse-temurin:17-jdk-alpine
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
ENTRYPOINT ["java", "-jar", "/app/app.jar"]