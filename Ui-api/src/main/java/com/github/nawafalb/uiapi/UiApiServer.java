
package com.github.nawafalb.uiapi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

public class UiApiServer {

    private static final String CLASS_API_BASE_URL = "http://localhost:8081";
    private static final ObjectMapper mapper = new ObjectMapper();

    public static void main(String[] args) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(8082), 0);
        System.out.println("UI API (JDK HttpServer) on http://localhost:8082");

        server.createContext("/health", UiApiServer::healthHandler);
        server.createContext("/dashboard", UiApiServer::dashboardHandler);
        // very simple CORS preflight
        server.createContext("/", UiApiServer::corsHandler);

        server.setExecutor(null);
        server.start();
    }

    private static void healthHandler(HttpExchange ex) throws IOException {
        withCors(ex);
        if ("GET".equalsIgnoreCase(ex.getRequestMethod())) {
            sendJson(ex, 200, "{\"status\":\"ok\"}");
        } else if ("OPTIONS".equalsIgnoreCase(ex.getRequestMethod())) {
            sendEmpty(ex, 204);
        } else {
            sendJson(ex, 405, "{\"error\":\"method not allowed\"}");
        }
    }

    private static void dashboardHandler(HttpExchange ex) throws IOException {
        withCors(ex);
        if ("OPTIONS".equalsIgnoreCase(ex.getRequestMethod())) {
            sendEmpty(ex, 204);
            return;
        }
        if (!"GET".equalsIgnoreCase(ex.getRequestMethod())) {
            sendJson(ex, 405, "{\"error\":\"method not allowed\"}");
            return;
        }

        String qs = Optional.ofNullable(ex.getRequestURI().getQuery()).map(q -> "?" + q).orElse("");
        String combinedUrl = CLASS_API_BASE_URL + "/combined" + qs;

        HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
        HttpRequest req = HttpRequest.newBuilder(URI.create(combinedUrl)).timeout(Duration.ofSeconds(10)).GET().build();

        try {
            HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() >= 400) {
                sendJson(ex, 502, jsonError("class-api returned status " + resp.statusCode()));
                return;
            }

            JsonNode body;
            try {
                body = mapper.readTree(resp.body());
            } catch (Exception pe) {
                sendJson(ex, 502, jsonError("class-api returned non-JSON"));
                return;
            }

            double aqi = safeDouble(body, "aqi", Double.NaN);
            double uv  = safeDouble(body, "uv",  Double.NaN);
            String summary = body.hasNonNull("summary") ? body.get("summary").asText() : "N/A";
            if (Double.isNaN(aqi) || Double.isNaN(uv)) {
                sendJson(ex, 502, jsonError("class-api JSON missing expected fields (aqi/uv)"));
                return;
            }

            ObjectNode root = mapper.createObjectNode();
            root.put("timestamp", Instant.now().toString());
            root.put("summary", summary);

            String aqiCat = aqiCategory(aqi);
            String uvRisk = uvRisk(uv);
            root.put("alertLevel", overallAlert(aqi, uv));

            ObjectNode cards = root.putObject("cards");
            ObjectNode air = cards.putObject("airQuality");
            air.put("aqi", round1(aqi));
            air.put("category", aqiCat);
            air.put("advice", aqiAdvice(aqi));
            air.put("color", aqiColor(aqiCat));

            ObjectNode uvCard = cards.putObject("uv");
            uvCard.put("uvIndex", round1(uv));
            uvCard.put("risk", uvRisk);
            uvCard.put("advice", uvAdvice(uv));
            uvCard.put("color", uvColor(uvRisk));

            sendJson(ex, 200, mapper.writeValueAsString(root));

        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            sendJson(ex, 500, jsonError("interrupted contacting class-api"));
        } catch (IOException ioe) {
            sendJson(ex, 500, jsonError("failed contacting class-api: " + ioe.getMessage()));
        }
    }

    private static JsonNode overallAlert(double aqi, double uv) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'overallAlert'");
    }

    private static void corsHandler(HttpExchange ex) throws IOException {
        withCors(ex);
        if ("OPTIONS".equalsIgnoreCase(ex.getRequestMethod())) {
            sendEmpty(ex, 204);
        } else {
            sendJson(ex, 404, "{\"error\":\"not found\"}");
        }
    }

    // -------- utilities --------
    private static void withCors(HttpExchange ex) {
        ex.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        ex.getResponseHeaders().add("Access-Control-Allow-Methods", "GET,POST,OPTIONS");
        ex.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type, Authorization");
        ex.getResponseHeaders().add("Content-Type", "application/json");
    }

    private static void sendJson(HttpExchange ex, int status, String body) throws IOException {
        byte[] bytes = body.getBytes();
        ex.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static void sendEmpty(HttpExchange ex, int status) throws IOException {
        ex.sendResponseHeaders(status, -1);
        ex.close();
    }

    private static String jsonError(String msg) {
        try {
            ObjectNode n = mapper.createObjectNode();
            n.put("error", msg);
            return mapper.writeValueAsString(n);
        } catch (Exception e) {
            return "{\"error\":\"" + msg.replace("\"", "'") + "\"}";
        }
    }

    private static double safeDouble(JsonNode node, String field, double fallback) {
        return node.has(field) && node.get(field).isNumber() ? node.get(field).asDouble() : fallback;
    }

    private static double round1(double x) { return Math.round(x * 10.0) / 10.0; }
    private static String aqiCategory(double aqi) {
        if (aqi <= 50)  return "Good";
        if (aqi <= 100) return "Moderate";
        if (aqi <= 150) return "Unhealthy for Sensitive Groups";
        if (aqi <= 200) return "Unhealthy";
        if (aqi <= 300) return "Very Unhealthy";
        return "Hazardous";
    }
    private static String aqiAdvice(double aqi) {
        switch (aqiCategory(aqi)) {
            case "Good": return "Enjoy outdoor activities.";
            case "Moderate": return "Sensitive groups: limit prolonged outdoor exertion.";
            case "Unhealthy for Sensitive Groups": return "Sensitive groups: reduce outdoor exertion.";
            case "Unhealthy": return "Everyone: reduce prolonged or heavy outdoor exertion.";
            case "Very Unhealthy": return "Avoid outdoor exertion; consider a mask/air purifier.";
            default: return "Stay indoors with clean air; follow local guidance.";
        }
    }
    private static String uvRisk(double uv) {
        if (uv < 3)   return "Low";
        if (uv < 6)   return "Moderate";
        if (uv < 8)   return "High";
        if (uv < 11)  return "Very High";
        return "Extreme";
    }
    private static String uvAdvice(double uv) {
        switch (uvRisk(uv)) {
            case "Low": return "Minimal protection needed.";
            case "Moderate": return "Wear sunglasses and SPF 30+.";
            case "High": return "Reduce time in sun; SPF 30+, hat, sunglasses.";
            case "Very High": return "Avoid midday sun; protective clothing; SPF 30+.";
            default: return "Avoid exposure; seek shade; SPF 50+.";
        }
    }
    private static String aqiColor(String cat) {
        switch (cat) {
            case "Good": return "green";
            case "Moderate": return "yellow";
            case "Unhealthy for Sensitive Groups": return "orange";
            case "Unhealthy": return "red";
            case "Very Unhealthy": return "purple";
            default: return "maroon";
        }
    }
    private static String uvColor(String risk) {
        switch (risk) {
            case "Low": return "green";
            case "Moderate": return "yellow";
            case "High": return "orange";
            case "Very High": return "red";
            default: return "purple";
        }
    }
}