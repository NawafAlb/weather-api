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

public class AqiLoader {
    public static void main(String[] args) throws Exception {
        Dotenv env = Dotenv.load();

        String aqicnToken = env.get("AQICN_TOKEN");
        String lat = env.get("LAT");
        String lon = env.get("LON");

        if (aqicnToken == null || lat == null || lon == null) {
            System.err.println("Missing AQICN_TOKEN or LAT/LON in .env");
            return;
        }

        HttpClient client = HttpClient.newHttpClient();
        ObjectMapper mapper = new ObjectMapper();

        // --- AQICN API ---
        String aqicnUrl = "https://api.waqi.info/feed/geo:" + lat + ";" + lon + "/?token=" + aqicnToken;
        HttpRequest aqiRequest = HttpRequest.newBuilder()
                .uri(URI.create(aqicnUrl))
                .build();

        HttpResponse<String> aqiResponse = client.send(aqiRequest, HttpResponse.BodyHandlers.ofString());
        JsonNode aqiJson = mapper.readTree(aqiResponse.body());
        int aqi = aqiJson.path("data").path("aqi").asInt();

        System.out.println("🌫 AQI: " + aqi + " @ LAT=" + lat + ", LON=" + lon);

        saveToDatabaseAqi(lat, lon, aqi);
    }

    private static void saveToDatabaseAqi(String lat, String lon, int aqi) {
        String url = "jdbc:sqlite:weather.db";
        String deleteSQL = "DELETE FROM user_DataAirQuality;";
        String insertSQL = "INSERT INTO user_DataAirQuality(latitude, longitude, air_quality, recorded_at) VALUES(?,?,?,?);";

            // insert: aqi filled, uv = NULL
            try (Connection conn = DatabaseHelper.connect();
                Statement stmt = conn.createStatement();
                PreparedStatement pstmt = conn.prepareStatement(insertSQL)) {

                // clear tables
                stmt.execute(deleteSQL);
                
                pstmt.setDouble(1, Double.parseDouble(lat));
                pstmt.setDouble(2, Double.parseDouble(lon));
                pstmt.setInt(3, aqi);
                pstmt.setString(4, LocalDateTime.now().toString());
                pstmt.executeUpdate();
            }
            // summary
            try (FileWriter w = new FileWriter("summary_aqi.txt", false)) {
                w.write("AQI load complete\n");
                w.write("Timestamp: " + LocalDateTime.now() + "\n");
                w.write("Rows inserted: 1\n");
            }
            System.out.println("Saved AQI to DB + summary_aqi.txt");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

