package com.github.nawafalb;

import io.github.cdimascio.dotenv.Dotenv;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Statement;

import java.time.LocalDateTime;
import java.io.FileWriter;

public class UvLoader {
    public static void main(String[] args) throws Exception {
        Dotenv env = Dotenv.load();

        String openUvKey = env.get("OPENUV_KEY");
        String lat = env.get("LAT");
        String lon = env.get("LON");

        if (openUvKey == null || lat == null || lon == null) {
            System.err.println("Missing OPENUV_KEY or LAT/LON in .env");
            return;
        }

        HttpClient client = HttpClient.newHttpClient();
        ObjectMapper mapper = new ObjectMapper();

        // --- OpenUV API ---
        String uvUrl = "https://api.openuv.io/api/v1/uv?lat=" + lat + "&lng=" + lon;
        HttpRequest uvRequest = HttpRequest.newBuilder()
                .uri(URI.create(uvUrl))
                .header("x-access-token", openUvKey)
                .build();

        HttpResponse<String> uvResponse = client.send(uvRequest, HttpResponse.BodyHandlers.ofString());
        JsonNode uvJson = mapper.readTree(uvResponse.body());
        double uvIndex = uvJson.path("result").path("uv").asDouble();

        System.out.println("UV Index: " + uvIndex + " @ LAT=" + lat + ", LON=" + lon);

        saveToDatabaseUv(lat, lon, uvIndex);
    }

    private static void saveToDatabaseUv(String lat, String lon, double uvIndex) {
        String url = "jdbc:sqlite:weather.db";
        String deleteSQL = "DELETE FROM user_DataUV;";
        String insertSQL = "INSERT INTO user_DataUV(latitude, longitude, uv_index, recorded_at) VALUES(?,?,?,?);";

            // insert: uv filled, aqi = NULL
            try (Connection conn = DatabaseHelper.connect();
                Statement stmt = conn.createStatement();
                PreparedStatement pstmt = conn.prepareStatement(insertSQL)) {

                //Wipe user_DataAirQuality ute(deleteSQL);
                
                pstmt.setDouble(1, Double.parseDouble(lat));
                pstmt.setDouble(2, Double.parseDouble(lon));
                pstmt.setDouble(3, uvIndex);
                pstmt.setString(4, LocalDateTime.now().toString());
                pstmt.executeUpdate();
            }
            // summary
            try (FileWriter w = new FileWriter("summary_uv.txt", false)) {
                w.write("UV load complete\n");
                w.write("Timestamp: " + LocalDateTime.now() + "\n");
                w.write("Rows inserted: 1\n");
            }
            System.out.println("Saved UV to DB + summary_uv.txt");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

