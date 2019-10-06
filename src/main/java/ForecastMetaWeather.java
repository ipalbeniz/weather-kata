import com.google.common.collect.Streams;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.json.JSONArray;
import org.json.JSONObject;

import java.time.LocalDate;
import java.util.Optional;

/**
 * Weather forecast using the Meta Weather public API (https://www.metaweather.com/api)
 */
public class ForecastMetaWeather implements Forecast {

    private static final String DEFAULT_META_WEATHER_API_BASE_URL = "https://www.metaweather.com";
    private static final int DAYS_WITH_PREDICTIONS = 6;

    private final String weatherApiBaseUrl;
    private final HttpClient httpClient;

    public ForecastMetaWeather(final HttpClient httpClient) {
        this(httpClient, DEFAULT_META_WEATHER_API_BASE_URL);
    }

    public ForecastMetaWeather(final HttpClient httpClient, final String weatherApiBaseUrl) {
        this.weatherApiBaseUrl = weatherApiBaseUrl;
        this.httpClient = httpClient;
    }

    @Override
    public Optional<String> predictWind(final ForecastRequest request) throws ForecastException {
        return this.predict(request.getCity(), request.getDate(), true);
    }

    @Override
    public Optional<String> predictWeather(final ForecastRequest request) throws ForecastException {
        return this.predict(request.getCity(), request.getDate(), false);
    }

    private Optional<String> predict(final String city, LocalDate date, final boolean wind) throws ForecastException {

        if (date == null) {
            date = LocalDate.now();
        }

        if (this.dateIsOutOfPredictionsRange(date)) {
            return Optional.empty();
        }

        final String cityId = this.findCityId(city);

        return wind
                ? this.findCityPrediction(cityId, date).map(Prediction::getWind)
                : this.findCityPrediction(cityId, date).map(Prediction::getWeather);
    }

    private String findCityId(final String city) throws ForecastException {

        try {
            final String rawCityDetails = this.httpClient.get(this.weatherApiBaseUrl + "/api/location/search/?query=" + city);
            final JSONArray jsonArray = new JSONArray(rawCityDetails);
            return jsonArray.getJSONObject(0).get("woeid").toString();
        } catch (final HttpClientException exception) {
            throw new ForecastException("something failed while getting the city id", exception);
        }
    }

    private Optional<Prediction> findCityPrediction(final String cityId, final LocalDate predictionDate) throws ForecastException {

        try {
            final String rawCityPredictions = this.httpClient.get(this.weatherApiBaseUrl + "/api/location/" + cityId);
            final JSONArray cityPredictionsByDate = new JSONObject(rawCityPredictions).getJSONArray("consolidated_weather");

            return Streams.stream(cityPredictionsByDate)
                    .map(JSONObject.class::cast)
                    .filter(jsonObject -> equalsPredictionDate(predictionDate, jsonObject))
                    .map(this::buildPrediction)
                    .findAny();
        } catch (final HttpClientException exception) {
            throw new ForecastException("something failed while getting the city prediction", exception);
        }
    }

    private boolean equalsPredictionDate(LocalDate predictionDate, JSONObject jsonObject) {
        return predictionDate.equals(LocalDate.parse(jsonObject.get("applicable_date").toString()));
    }

    private Prediction buildPrediction(final JSONObject jsonObject) {
        return Prediction.builder()
                .weather(jsonObject.get("weather_state_name").toString())
                .wind(jsonObject.get("wind_speed").toString())
                .build();
    }

    private boolean dateIsOutOfPredictionsRange(final LocalDate datetime) {
        return datetime.isAfter(this.getLastDayWithPredictions());
    }

    private LocalDate getLastDayWithPredictions() {
        return LocalDate.now().plusDays(DAYS_WITH_PREDICTIONS - 1);
    }

    @Data
    @Builder
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    private static class Prediction {
        private final String weather;
        private final String wind;
    }
}
