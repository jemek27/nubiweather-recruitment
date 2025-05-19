package com.nubisoft.nubiweather.service;

import com.nubisoft.nubiweather.model.CurrentWeatherResponse;
import com.nubisoft.nubiweather.model.ForecastWeatherResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class WeatherService {

    private final WebClient webClient;
    private final String apiKey;
    private final ReactiveRedisTemplate<String, CurrentWeatherResponse> currentWeatherRedis;
    private final ReactiveRedisTemplate<String, ForecastWeatherResponse> forecastWeatherRedis;
    private final List<String> cities = Arrays.asList("Gliwice", "Hamburg");

    public WeatherService(
            @Value("${weather.api.base-url}") String baseUrl,
            @Value("${env.weather.api.key}") String apiKey,
            ReactiveRedisTemplate<String, CurrentWeatherResponse> currentWeatherRedis,
            ReactiveRedisTemplate<String, ForecastWeatherResponse> forecastWeatherRedis
    ) {
        this.webClient = WebClient.builder().baseUrl(baseUrl).build();
        this.apiKey = apiKey;
        this.currentWeatherRedis = currentWeatherRedis;
        this.forecastWeatherRedis = forecastWeatherRedis;
    }

    public Flux<CurrentWeatherResponse> getRealtimeWeather() {
        return Flux.fromIterable(cities)
                .flatMap(this::getCurrent);
    }

    public Flux<ForecastWeatherResponse> getForecastWeather(int days) {
        return Flux.fromIterable(cities)
                .flatMap(city -> getForecast(city, days));
    }

    public Mono<ForecastWeatherResponse> getForecast(String city, int days) {
        return fetchWeatherData("/forecast.json",
                Map.of("q", city, "days", String.valueOf(days)),
                ForecastWeatherResponse.class,
                forecastWeatherRedis);
    }

    private Mono<CurrentWeatherResponse> getCurrent(String city) {
        return fetchWeatherData("/current.json",
                Map.of("q", city),
                CurrentWeatherResponse.class,
                currentWeatherRedis);
    }

    private <T> Mono<T> fetchWeatherData(String path,
                                         Map<String,String> params,
                                         Class<T> type,
                                         ReactiveRedisTemplate<String, T> redisTemplate) {
        String key = buildCacheKey(path, params);

        return redisTemplate.opsForValue()
                    .get(key)
                    .switchIfEmpty(Mono.defer(() -> {
                        System.out.println("Cache miss for key: " + key);
                        return fetchAndCache(path, params, type, key, redisTemplate);
                    }));
    }

    private <T> Mono<T> fetchAndCache(String path,
                                      Map<String,String> params,
                                      Class<T> type,
                                      String cacheKey,
                                      ReactiveRedisTemplate<String, T> redisTemplate) {
        return webClient.get()
                .uri(u -> {
                    var b = u.path(path)
                            .queryParam("key", apiKey);
                    params.forEach(b::queryParam);
                    return b.build();
                })
                .retrieve()
                .bodyToMono(type)
                .flatMap(resp ->
                        // save to Redis and return the object
                        redisTemplate.opsForValue()
                                .set(cacheKey, resp)
                                .flatMap(ok -> redisTemplate.expire(cacheKey, getTtlForType(path))) // TTL
                                .thenReturn(resp)
                );
    }

    private String buildCacheKey(String path, Map<String,String> params) {
        return path + "?" +
                params.entrySet().stream()
                        .map(e -> e.getKey() + "=" + e.getValue())
                        .collect(Collectors.joining("&"));
    }

    private Duration getTtlForType(String dataType) {
        return switch (dataType) {
            case "/current.json" -> Duration.ofMinutes(15);
            case "/forecast.json" -> Duration.ofMinutes(60);
            default -> Duration.ofMinutes(30);
        };
    }
}