package infrastructure;

public interface HttpClient {
    String get(String url) throws HttpClientException;
}
