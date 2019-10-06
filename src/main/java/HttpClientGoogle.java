import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.javanet.NetHttpTransport;

import java.io.IOException;

public class HttpClientGoogle implements HttpClient {

    @Override
    public String get(final String url) throws HttpClientException {

        try {
            final HttpRequestFactory requestFactory = new NetHttpTransport().createRequestFactory();
            final HttpRequest request = requestFactory.buildGetRequest(new GenericUrl(url));
            return request.execute().parseAsString();
        } catch (final IOException exception) {
            throw new HttpClientException(exception);
        }
    }
}
