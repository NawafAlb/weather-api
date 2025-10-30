package com.github.nawafalb;

import static spark.Spark.*;
import java.net.http.*;
import java.net.URI;
import java.io.IOException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * ClassApiServer acts as a higher-level API layer.
 * It communicates with the Data API (http://localhost:8080)
 * to fetch raw data and then combines or transforms it
 * into richer, object-oriented responses.
 */
public class ClassApiServer {

    // The Data API base URL (the one running on port 8080)
    private static final String DATA_API_BASE_URL = "http://localhost:8080";

    public static void main(String[] args) {
        // Run this Class API on port 8081
        port(8081);
        System.out.println("✅ Class API Server running on http://localhost:8081");

        // Example endpoint: combines Air Quality and UV data
        get("/combined", (req, res) -> {
            res.type("application/json");

            // Create an HTTP client to talk to the Data API
            HttpClient client = HttpClient.newHttpClient();

            try {
                // Prepare HTTP requests to the Data API
                HttpRequest airQualityRequest = HttpRequest.newBuilder()
                        .uri(URI.create(DATA_API_BASE_URL + "/airquality"))
                        .build();

                HttpRequest uvRequest = HttpRequest.newBuilder()
                        .uri(URI.create(DATA_API_BASE_URL + "/uv"))
                        .build();

                // Send both requests and capture responses
                HttpResponse<String> airQualityResponse = client.send(airQualityRequest, HttpResponse.BodyHandlers.ofString());
                HttpResponse<String> uvResponse = client.send(uvRequest, HttpResponse.BodyHandlers.ofString());

                // Parse JSON responses using Jackson
                ObjectMapper mapper = new ObjectMapper();
                JsonNode airNode = mapper.readTree(airQualityResponse.body());
                JsonNode uvNode = mapper.readTree(uvResponse.body());

                // Air Quality
                JsonNode airData = airNode.get(0);  // get first element
                double aqi = airData.get("air_Quality").asDouble();

                // UV
                JsonNode uvData = uvNode.get(0);
                double uvIndex = uvData.get("uv_index").asDouble();

                // Simple business logic: combine and interpret data
                String summary = (aqi < 50 ? "Good Air" : "Poor Air") +
                                 " & " +
                                 (uvIndex < 3 ? "Low UV" : "High UV");

                // Build a combined JSON string
                String combinedJson = String.format(
                    "{\"aqi\": %.2f, \"uv\": %.2f, \"summary\": \"%s\"}",
                    aqi, uvIndex, summary
                );

                return combinedJson;

            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
                res.status(500);
                return "{\"error\": \"Failed to fetch or combine data\"}";
            }
        });
    }
}
