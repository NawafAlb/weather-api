package com.github.nawafalb;

import static spark.Spark.*;
import java.net.http.*;
import java.net.URI;
import java.io.IOException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

public class ClassApiServer {
    private static final String DATA_API_URL = "http://localhost:8080";
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final HttpClient client = HttpClient.newHttpClient();

    public static void main(String[] args) {
        port(8081);
        System.out.println("Class API Server running on http://localhost:8081");

        get("/combined", (req, res) -> {
            res.type("application/json");
            try {
                // Talk to Data API
                HttpRequest aqiReq = HttpRequest.newBuilder()
                        .uri(URI.create(DATA_API_URL + "/airquality"))
                        .build();

                HttpRequest uvReq = HttpRequest.newBuilder()
                        .uri(URI.create(DATA_API_URL + "/uv"))
                        .build();

                // Get both responses
                HttpResponse<String> aqiRes = client.send(aqiReq, HttpResponse.BodyHandlers.ofString());
                HttpResponse<String> uvRes = client.send(uvReq, HttpResponse.BodyHandlers.ofString());

                // Parse JSON
                JsonNode aqiJson = mapper.readTree(aqiRes.body()).get(0);
                JsonNode uvJson = mapper.readTree(uvRes.body()).get(0);

                double aqi = aqiJson.get("air_Quality").asDouble();
                double uv = uvJson.get("uv_index").asDouble();

                String summary = (aqi < 50 ? "Good Air" : "Poor Air") + " & " + (uv < 3 ? "Low UV" : "High UV");

                return String.format("{\"aqi\": %.2f, \"uv\": %.2f, \"summary\": \"%s\"}", aqi, uv, summary);
            } catch (Exception e) {
                e.printStackTrace();
                res.status(500);
                return "{\"error\": \"Failed to fetch or combine data\"}";
            }
        });
    }
}
