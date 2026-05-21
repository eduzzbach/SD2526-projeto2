package sd2526.trab.impl.zoho;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class ZohoInitialRefreshToken {

    static final String TOKEN_ENDPOINT = "https://accounts.zoho.eu/oauth/v2/token";

    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            System.err.println("Usage: ZohoInitialRefreshToken <grant_code>");
            System.exit(1);
        }

        String grantCode = args[0];

        String body = "code=" + grantCode
                + "&client_id=" + Zoho.CLIENT_ID
                + "&client_secret=" + Zoho.CLIENT_SECRET
                + "&grant_type=authorization_code";

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(TOKEN_ENDPOINT))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        System.out.println("Status: " + response.statusCode());
        System.out.println("Response: " + response.body());

        // Extract and print the refresh token clearly
        String responseBody = response.body();
        if (responseBody.contains("refresh_token")) {
            int start = responseBody.indexOf("\"refresh_token\"") + "\"refresh_token\"".length();
            start = responseBody.indexOf("\"", start) + 1;
            int end = responseBody.indexOf("\"", start);
            System.out.println("\nRefresh token: " + responseBody.substring(start, end));
        } else {
            System.err.println("\nNo refresh_token in response. Check the grant code and try again.");
        }
    }
}
