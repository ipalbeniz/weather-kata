import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.jayway.jsonpath.JsonPath;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Weather forecast using the Meta Weather public API (https://www.metaweather.com/api)
 */
public class ForecastMetaWeather implements Forecast {

    private static final String DEFAULT_META_WEATHER_API_BASE_URL = "https://www.metaweather.com";
    private static final int MAX_DAYS_WITH_PREDICTIONS = 6;
    private static final Cache<String, String> CITY_IDS_BY_NAME = Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(1, TimeUnit.HOURS)
            .build();

    private final String weatherApiBaseUrl;
    private final HttpClient httpClient;

    public ForecastMetaWeather(final HttpClient httpClient) {
        this(httpClient, DEFAULT_META_WEATHER_API_BASE_URL);
    }

    ForecastMetaWeather(final HttpClient httpClient, final String weatherApiBaseUrl) {
        this.weatherApiBaseUrl = weatherApiBaseUrl;
        this.httpClient = httpClient;
    }

    @Override
    public Optional<String> predictWind(final ForecastRequest request) throws ForecastException {
        return this.predict(request.getCityName(), request.getDate(), true);
    }

    @Override
    public Optional<String> predictWeather(final ForecastRequest request) throws ForecastException {
        return this.predict(request.getCityName(), request.getDate(), false);
    }

    private Optional<String> predict(final String cityName, LocalDate date, final boolean wind) throws ForecastException {

        if (date == null) {
            date = LocalDate.now();
        }

        if (this.dateIsOutOfPredictionsRange(date)) {
            return Optional.empty();
        }

        final String cityId = this.findCityIdFrom(cityName);

        return wind
                ? this.findCityPrediction(cityId, date).map(Prediction::getWind)
                : this.findCityPrediction(cityId, date).map(Prediction::getWeather);
    }

    private String findCityIdFrom(final String cityName) throws ForecastException {
        return CITY_IDS_BY_NAME.get(cityName, this::getCityIdFromMetaWeatherApi);
    }

    private String getCityIdFromMetaWeatherApi(final String cityName) throws ForecastException {
        try {
            final String rawCityDetails = this.httpClient.get(this.weatherApiBaseUrl + "/api/location/search/?query=" + cityName);
            return JsonPath.read(rawCityDetails, "$[0].woeid").toString();
        } catch (final HttpClientException exception) {
            throw new ForecastException("something failed while getting the city id", exception);
        }
    }

    private Optional<Prediction> findCityPrediction(final String cityId, final LocalDate predictionDate) throws ForecastException {

        try {
            final String rawCityPredictions = this.httpClient.get(this.weatherApiBaseUrl + "/api/location/" + cityId);
            final List<Map<String, Object>> cityPredictions = JsonPath.read(rawCityPredictions, "$.consolidated_weather[*]");

            return cityPredictions.stream()
                    .filter(cityPrediction -> this.equalsPredictionDate(predictionDate, cityPrediction.get("applicable_date")))
                    .map(this::buildPrediction)
                    .findAny();
        } catch (final HttpClientException exception) {
            throw new ForecastException("something failed while getting the city prediction", exception);
        }
    }

    private boolean equalsPredictionDate(final LocalDate predictionDate, final Object cityPredictionDate) {
        return predictionDate.equals(LocalDate.parse(cityPredictionDate.toString()));
    }

    private Prediction buildPrediction(final Map<String, Object> cityPrediction) {
        return Prediction.builder()
                .weather(cityPrediction.get("weather_state_name").toString())
                .wind(cityPrediction.get("wind_speed").toString())
                .build();
    }

    private boolean dateIsOutOfPredictionsRange(final LocalDate datetime) {
        return datetime.isAfter(this.getLastDayWithPredictions());
    }

    private LocalDate getLastDayWithPredictions() {
        return LocalDate.now().plusDays(MAX_DAYS_WITH_PREDICTIONS - 1);
    }

    @Data
    @Builder
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    private static class Prediction {
        private final String weather;
        private final String wind;
    }
}
