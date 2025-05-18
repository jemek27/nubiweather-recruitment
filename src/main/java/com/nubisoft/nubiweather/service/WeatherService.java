package com.nubisoft.nubiweather.service;

import com.nubisoft.nubiweather.model.CurrentWeatherResponse;
import com.nubisoft.nubiweather.model.ForecastWeatherResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Service
public class WeatherService {

    private final WebClient webClient;
    private final String apiKey;
    private final List<String> cities = Arrays.asList("Gliwice", "Hamburg");

    public WeatherService(
            @Value("${weather.api.base-url}") String baseUrl,
            @Value("${env.weather.api.key}") String apiKey) {
        this.webClient = WebClient.builder().baseUrl(baseUrl).build();
        this.apiKey = apiKey;
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
                ForecastWeatherResponse.class);
    }

    private Mono<CurrentWeatherResponse> getCurrent(String city) {
        return fetchWeatherData("/current.json",
                Map.of("q", city),
                CurrentWeatherResponse.class);
    }

    private <T> Mono<T> fetchWeatherData(String path, Map<String, String> queryParams, Class<T> responseType) {
        return webClient.get()
                .uri(uriBuilder -> {
                    uriBuilder.path(path).queryParam("key", apiKey);
                    queryParams.forEach(uriBuilder::queryParam);
                    return uriBuilder.build();
                })
                .retrieve()
                .bodyToMono(responseType);
    }
}