package io.jenkins.plugins.smartselect;

import hudson.ProxyConfiguration;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
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
                    .map(String::trim).filter(line -> !line.isEmpty())
                    .anyMatch(line -> origin.equals(origin(URI.create(line))));
            if (!allowed) throw new IllegalArgumentException("REST 数据源地址尚未获管理员授权");
            return uri;
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("REST 数据源地址无效或未获管理员授权");
        }
    }

    static String origin(URI uri) {
        String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
        if ((!scheme.equals("https") && !scheme.equals("http")) || uri.getHost() == null
                || uri.getUserInfo() != null || uri.getFragment() != null)
            throw new IllegalArgumentException("Only HTTP(S) origins without credentials or fragments are allowed");
        int port = uri.getPort() < 0 ? (scheme.equals("https") ? 443 : 80) : uri.getPort();
        if (port < 1 || port > 65535) throw new IllegalArgumentException("Invalid port");
        return scheme + "://" + uri.getHost().toLowerCase(Locale.ROOT) + ":" + port;
    }

    public static List<SelectOption> load(String url, String itemsPath, String token, String allowedOrigins) {
        URI uri = validate(url, allowedOrigins);
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) ProxyConfiguration.open(uri.toURL());
            connection.setConnectTimeout(3000);
            connection.setReadTimeout(3000);
            connection.setInstanceFollowRedirects(false);
            connection.setRequestProperty("Accept", "application/json");
            if (token != null && !token.isEmpty()) connection.setRequestProperty("Authorization", "Bearer " + token);
            if (connection.getResponseCode() != 200) throw new IllegalArgumentException("REST 数据源返回非成功状态");
            String contentType = connection.getContentType();
            if (contentType == null || !contentType.toLowerCase(Locale.ROOT).matches("application/(?:json|[a-z0-9.+-]+\\+json)(?:;.*)?"))
                throw new IllegalArgumentException("REST 数据源必须返回 JSON");
            if (connection.getContentLengthLong() > MAX_BYTES) throw new IllegalArgumentException("REST 数据源响应超过 1 MiB");
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
            byte[] bytes;
            try (InputStream input = connection.getInputStream(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
                byte[] buffer = new byte[8192];
                int count;
                while ((count = input.read(buffer)) != -1) {
                    if (output.size() + count > MAX_BYTES || System.nanoTime() > deadline)
                        throw new IllegalArgumentException("REST 数据源响应过大或超时");
                    output.write(buffer, 0, count);
                }
                bytes = output.toByteArray();
            }
            JSON json = JSONSerializer.toJSON(new String(bytes, StandardCharsets.UTF_8));
            Object items = json;
            if (itemsPath != null && !itemsPath.isBlank()) {
                if (!itemsPath.matches("[a-zA-Z0-9_-]+(?:\\.[a-zA-Z0-9_-]+)*")) throw new IllegalArgumentException("Invalid items path");
                for (String key : itemsPath.split("\\.")) {
                    if (!(items instanceof JSONObject object) || !object.containsKey(key))
                        throw new IllegalArgumentException("REST 数据源中找不到选项数组");
                    items = object.get(key);
                }
            }
            return OptionCodec.parse(items);
        } catch (java.io.IOException | RuntimeException e) {
            // Never expose endpoint URLs, response bodies, bearer tokens or transport exception details.
            throw new IllegalArgumentException("无法读取 REST 选项，请检查管理员授权、接口状态和 JSON 格式");
        } finally {
            if (connection != null) connection.disconnect();
        }
    }
}
