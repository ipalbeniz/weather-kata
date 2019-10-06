import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.javanet.NetHttpTransport;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

public class Forecast {

    private static final String DEFAULT_WEATHER_API_BASE_URL = "https://www.metaweather.com";

    private final String weatherApiBaseUrl;

    public Forecast() {
        this(DEFAULT_WEATHER_API_BASE_URL);
    }

    public Forecast(String weatherApiBaseUrl) {
        this.weatherApiBaseUrl = weatherApiBaseUrl;
    }

    public Optional<String> predictWind(ForecastRequest request) throws IOException {
        return this.predict(request.getCity(), request.getDate(), true);
    }

    public Optional<String> predictWeather(ForecastRequest request) throws IOException {
        return this.predict(request.getCity(), request.getDate(), false);
    }

    private Optional<String> predict(String city, LocalDate datetime, boolean wind) throws IOException {
        // When date is not provided we look for the current prediction
        if (datetime == null) {
            datetime = LocalDate.now();
        }
        String format = datetime.format(DateTimeFormatter.ISO_LOCAL_DATE);

        // If there are predictions
        if (datetime.isBefore(LocalDate.now().plusDays(6))) {

            // Find the id of the city on metawheather
            HttpRequestFactory requestFactory
                    = new NetHttpTransport().createRequestFactory();
            HttpRequest request = requestFactory.buildGetRequest(
                    new GenericUrl(weatherApiBaseUrl + "/api/location/search/?query=" + city));
            String rawResponse = request.execute().parseAsString();
            JSONArray jsonArray = new JSONArray(rawResponse);
            String woeid = jsonArray.getJSONObject(0).get("woeid").toString();

            // Find the predictions for the city
            requestFactory = new NetHttpTransport().createRequestFactory();
            request = requestFactory.buildGetRequest(
                    new GenericUrl(weatherApiBaseUrl + "/api/location/" + woeid));
            rawResponse = request.execute().parseAsString();
            JSONArray results = new JSONObject(rawResponse).getJSONArray("consolidated_weather");

            for (int i = 0; i < results.length(); i++) {
//            // When the date is the expected
                if (format.equals(results.getJSONObject(i).get("applicable_date").toString())) {
//                // If we have to return the wind information
                    if (wind) {
                        return Optional.of(results.getJSONObject(i).get("wind_speed").toString());
                    } else {
                        return Optional.of(results.getJSONObject(i).get("weather_state_name").toString());
                    }
                }
            }
        } else {
            return Optional.empty();
        }
        return Optional.empty();
    }
}
