package io.jenkins.plugins.smartselect;

import hudson.ProxyConfiguration;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import net.sf.json.JSON;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;

/** Server-side GET with administrator-approved origins, no redirects and bounded resources. */
public final class RestOptionSource {
    private static final int MAX_BYTES = 1_048_576;

    private RestOptionSource() {}

    public static URI validate(String url, String allowedOrigins) {
        try {
            URI uri = URI.create(url);
            String origin = origin(uri);
            boolean allowed = Arrays.stream((allowedOrigins == null ? "" : allowedOrigins).split("\\R"))
                    .map(String::trim)
                    .filter(line -> !line.isEmpty())
                    .anyMatch(line -> origin.equals(origin(URI.create(line))));
            if (!allowed) throw new IllegalArgumentException(Messages.RestOriginUnauthorized());
            return uri;
        } catch (RuntimeException e) {
            throw new IllegalArgumentException(Messages.RestOriginInvalid());
        }
    }

    static String origin(URI uri) {
        String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
        if ((!scheme.equals("https") && !scheme.equals("http"))
                || uri.getHost() == null
                || uri.getUserInfo() != null
                || uri.getFragment() != null)
            throw new IllegalArgumentException("Only HTTP(S) origins without credentials or fragments are allowed");
        int port = uri.getPort() < 0 ? (scheme.equals("https") ? 443 : 80) : uri.getPort();
        if (port < 1 || port > 65535) throw new IllegalArgumentException("Invalid port");
        return scheme + "://" + uri.getHost().toLowerCase(Locale.ROOT) + ":" + port;
    }

    public static List<SelectOption> load(String url, String itemsPath, String token, String allowedOrigins) {
        URI uri = validate(url, allowedOrigins);
        try (HttpClient client = ProxyConfiguration.newHttpClientBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build()) {
            HttpRequest.Builder builder = HttpRequest.newBuilder(uri)
                    .timeout(Duration.ofSeconds(3))
                    .header("Accept", "application/vnd.github.raw+json, application/json")
                    .GET();
            if (token != null && !token.isEmpty()) builder.header("Authorization", "Bearer " + token);
            HttpResponse<InputStream> response =
                    client.send(builder.build(), HttpResponse.BodyHandlers.ofInputStream());
            byte[] bytes;
            var executor = Executors.newVirtualThreadPerTaskExecutor();
            try (InputStream input = response.body()) {
                if (response.statusCode() != 200) throw new IllegalArgumentException(Messages.RestStatus());
                List<String> contentTypes = response.headers().allValues("Content-Type");
                if (contentTypes.isEmpty()
                        || !contentTypes.stream()
                                .flatMap(value -> Arrays.stream(value.split(",", -1)))
                                .allMatch(value -> value.trim()
                                        .toLowerCase(Locale.ROOT)
                                        .matches("application/(?:json|[a-z0-9.+-]+\\+json)(?:;.*)?")))
                    throw new IllegalArgumentException(Messages.RestJson());
                if (response.headers().firstValueAsLong("Content-Length").orElse(-1) > MAX_BYTES)
                    throw new IllegalArgumentException(Messages.RestSize());
                // Keep a body deadline: ofInputStream completes the response at the headers.
                bytes = executor.submit(() -> input.readNBytes(MAX_BYTES + 1)).get(5, TimeUnit.SECONDS);
                if (bytes.length > MAX_BYTES) throw new IllegalArgumentException(Messages.RestSize());
            } finally {
                executor.shutdownNow();
            }
            JSON json = JSONSerializer.toJSON(new String(bytes, StandardCharsets.UTF_8));
            Object items = json;
            if (itemsPath != null && !itemsPath.isBlank()) {
                if (!itemsPath.matches("[a-zA-Z0-9_-]+(?:\\.[a-zA-Z0-9_-]+)*"))
                    throw new IllegalArgumentException("Invalid items path");
                for (String key : itemsPath.split("\\.")) {
                    if (!(items instanceof JSONObject object) || !object.containsKey(key))
                        throw new IllegalArgumentException(Messages.RestArray());
                    items = object.get(key);
                }
            }
            return OptionCodec.parse(items);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalArgumentException(Messages.RestRead());
        } catch (java.io.IOException | ExecutionException | TimeoutException | RuntimeException e) {
            // Never expose endpoint URLs, response bodies, bearer tokens or transport exception details.
            throw new IllegalArgumentException(Messages.RestRead());
        }
    }
}
