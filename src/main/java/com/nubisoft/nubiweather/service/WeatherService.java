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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class WeatherService {

    private final WebClient webClient;
    private final String apiKey;
    private final ReactiveRedisTemplate<String, CurrentWeatherResponse> currentWeatherRedis;
    private final ReactiveRedisTemplate<String, ForecastWeatherResponse> forecastWeatherRedis;
    private final ReactiveRedisTemplate<String, Integer> redisMeta;
    private final List<String> cities = Arrays.asList("Gliwice", "Hamburg");
    private final String currentWeatherPath = "/current.json";
    private final String forecastWeatherPath = "/forecast.json";

    public WeatherService(
            @Value("${weather.api.base-url}") String baseUrl,
            @Value("${env.weather.api.key}") String apiKey,
            ReactiveRedisTemplate<String, CurrentWeatherResponse> currentWeatherRedis,
            ReactiveRedisTemplate<String, ForecastWeatherResponse> forecastWeatherRedis,
            ReactiveRedisTemplate<String, Integer> redisIntegerTemplate
    ) {
        this.webClient = WebClient.builder().baseUrl(baseUrl).build();
        this.apiKey = apiKey;
        this.currentWeatherRedis = currentWeatherRedis;
        this.forecastWeatherRedis = forecastWeatherRedis;
        this.redisMeta = redisIntegerTemplate;
    }

    public Flux<CurrentWeatherResponse> getRealtimeWeather() {
        return Flux.fromIterable(cities)
                .flatMap(this::getCurrent);
    }

    public Mono<CurrentWeatherResponse> getRealtimeWeather(String city) {
        return getCurrent(city);
    }

    private Mono<CurrentWeatherResponse> getCurrent(String city) {
        return fetchCurrentWeatherData(Map.of("q", city));
    }

    private Mono<CurrentWeatherResponse> fetchCurrentWeatherData(Map<String,String> params) {
        String key = buildCacheKey(currentWeatherPath, params);

        return currentWeatherRedis.opsForValue()
                .get(key)
                .switchIfEmpty(Mono.defer(() -> {
                    System.out.println("Cache miss for key: " + key);
                    return fetchAndCache(currentWeatherPath, params, CurrentWeatherResponse.class,
                            key, currentWeatherRedis);
                }));
    }


    public Flux<ForecastWeatherResponse> getForecastWeather(int days) {
        return Flux.fromIterable(cities)
                .flatMap(city -> getForecast(city, days));
    }

    public Mono<ForecastWeatherResponse> getForecastWeather(String city, int days) {
        return getForecast(city, days);
    }

    public Mono<ForecastWeatherResponse> getForecast(String city, int days) {
        return fetchForecastWeatherData(new HashMap<>(Map.of("q", city, "days", String.valueOf(days))));
    }

    private Mono<ForecastWeatherResponse> fetchForecastWeatherData(Map<String,String> params) {
        String key = buildCacheKey(forecastWeatherPath, params);

        String metaKey = params.get("q") + ":maxDays";
        int daysN = Integer.parseInt(params.get("days"));
        return redisMeta.opsForValue().get(metaKey)
                .defaultIfEmpty(0)
                .flatMap(maxFetched -> {
                    if (maxFetched >= daysN) {
                        // we have a forecast in cache for `maxFetched` days
                        params.put("days", String.valueOf(maxFetched));
                        String fullKey = buildCacheKey(forecastWeatherPath, params);

                        return forecastWeatherRedis.opsForValue().get(fullKey)
                                .map(fullResp -> {
                                    List<ForecastWeatherResponse.ForecastDay> list = fullResp.getForecast().getForecastDays();
                                    fullResp.getForecast().setForecastDays(list.subList(0, daysN));
                                    return fullResp;
                                });
                    } else {
                        saveMaxDays(metaKey, daysN);
                        System.out.println("Cache miss for key: " + key);
                        return fetchAndCache(forecastWeatherPath, params, ForecastWeatherResponse.class,
                                key, forecastWeatherRedis);
                    }
                });
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
            case currentWeatherPath -> Duration.ofMinutes(15);
            case forecastWeatherPath -> Duration.ofMinutes(60);
            default -> Duration.ofMinutes(1);
        };
    }

    private void saveMaxDays(String metaKey, int N) {
        redisMeta.opsForValue().get(metaKey)
                .defaultIfEmpty(0)
                .flatMap(oldMax -> {
                    if (N > oldMax) {
                        return redisMeta.opsForValue()
                                .set(metaKey, N)
                                .flatMap(ok -> redisMeta.expire(metaKey, getTtlForType(forecastWeatherPath))) // TTL
                                .thenReturn(N);
                    } else {
                        return Mono.just(oldMax);
                    }
                })
                .subscribe();
    }
}