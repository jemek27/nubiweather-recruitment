package com.nubisoft.nubiweather.controller;

import com.nubisoft.nubiweather.model.ForecastWeatherResponse;
import com.nubisoft.nubiweather.model.CurrentWeatherResponse;
import com.nubisoft.nubiweather.service.WeatherService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
public class WeatherController {

    private final WeatherService service;

    public WeatherController(WeatherService service) {
        this.service = service;
    }

    @GetMapping(value = "/realtime-weather", produces = MediaType.APPLICATION_JSON_VALUE)
    public Flux<CurrentWeatherResponse> realtimeWeather() {
        return service.getRealtimeWeather();
    }

    @GetMapping(value = "/forecast-weather", produces = MediaType.APPLICATION_JSON_VALUE)
    public Flux<ForecastWeatherResponse> forecastWeather() {
        return service.getForecastWeather(3);
    }
}
