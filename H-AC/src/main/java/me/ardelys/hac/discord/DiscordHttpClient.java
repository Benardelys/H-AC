package me.ardelys.hac.discord;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;

public class DiscordHttpClient {

    public record ResponseStatus(int statusCode, long retryAfterMs, boolean success, String errorMessage) {}

    private final HttpClient httpClient;

    public DiscordHttpClient() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    public ResponseStatus send(String webhookUrl, String jsonPayload) {
        return send(webhookUrl, jsonPayload, Duration.ofSeconds(5));
    }

    public ResponseStatus send(String webhookUrl, String jsonPayload, Duration timeout) {
        if (webhookUrl == null || webhookUrl.isEmpty() || jsonPayload == null) {
            return new ResponseStatus(0, 0, false, "Invalid URL or payload");
        }

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(webhookUrl))
                    .header("Content-Type", "application/json; charset=utf-8")
                    .header("User-Agent", "H-AC-AntiCheat/1.0.1 (Paper 1.21.11; HukümCraft)")
                    .timeout(timeout != null ? timeout : Duration.ofSeconds(5))
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            int status = response.statusCode();

            if (status == 200 || status == 204) {
                return new ResponseStatus(status, 0, true, null);
            }

            if (status == 429) {
                long retryAfter = parseRetryAfter(response);
                return new ResponseStatus(status, retryAfter, false, "Rate limited by Discord");
            }

            return new ResponseStatus(status, 0, false, "HTTP " + status + ": " + response.body());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new ResponseStatus(0, 0, false, "Request interrupted");
        } catch (Throwable t) {
            return new ResponseStatus(0, 0, false, t.getMessage());
        }
    }

    private long parseRetryAfter(HttpResponse<String> response) {
        Optional<String> header = response.headers().firstValue("Retry-After");
        if (header.isPresent()) {
            try {
                double sec = Double.parseDouble(header.get().trim());
                return sec > 100.0 ? (long) sec : (long) (sec * 1000.0);
            } catch (NumberFormatException ignored) {
            }
        }

        String body = response.body();
        if (body != null && body.contains("retry_after")) {
            try {
                int idx = body.indexOf("\"retry_after\":");
                if (idx != -1) {
                    int start = idx + 14;
                    int end = body.indexOf(",", start);
                    if (end == -1) end = body.indexOf("}", start);
                    if (end != -1) {
                        double val = Double.parseDouble(body.substring(start, end).trim());
                        return val > 100.0 ? (long) val : (long) (val * 1000.0);
                    }
                }
            } catch (Throwable ignored) {
            }
        }

        return 1500L;
    }
}
