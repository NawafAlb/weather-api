package com.github.nawafalb;

import io.github.cdimascio.dotenv.Dotenv;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class App {
    public static void main(String[] args) throws Exception {
        Dotenv env = Dotenv.load();

        String aqicnToken = env.get("AQICN_TOKEN");
        String openUvKey = env.get("OPENUV_KEY");
        String lat = env.get("LAT");
        String lon = env.get("LON");

        if (aqicnToken == null || openUvKey == null) {
            System.err.println("Missing tokens in .env");
            return;
        }

        HttpClient client = HttpClient.newHttpClient();
        ObjectMapper mapper = new ObjectMapper();

        // --- AQICN API ---
        String aqicnUrl = "https://api.waqi.info/feed/geo:" + lat + ";" + lon + "/?token=" + aqicnToken;
        HttpRequest aqicnRequest = HttpRequest.newBuilder()
                .uri(URI.create(aqicnUrl))
                .build();

        HttpResponse<String> aqicnResponse = client.send(aqicnRequest, HttpResponse.BodyHandlers.ofString());
        JsonNode aqicnJson = mapper.readTree(aqicnResponse.body());

        int aqi = aqicnJson.path("data").path("aqi").asInt();

        // --- OpenUV API ---
        String uvUrl = "https://api.openuv.io/api/v1/uv?lat=" + lat + "&lng=" + lon;
        HttpRequest uvRequest = HttpRequest.newBuilder()
                .uri(URI.create(uvUrl))
                .header("x-access-token", openUvKey)
                .build();

        HttpResponse<String> uvResponse = client.send(uvRequest, HttpResponse.BodyHandlers.ofString());
        JsonNode uvJson = mapper.readTree(uvResponse.body());

        double uvIndex = uvJson.path("result").path("uv").asDouble();

        // --- Final Output ---
        System.out.println("📍 Location: LAT=" + lat + " | LON=" + lon);
        System.out.println("🌫 Air Quality Index (AQI): " + aqi);
        System.out.println("☀️ Current UV Index: " + uvIndex);
    }
}
