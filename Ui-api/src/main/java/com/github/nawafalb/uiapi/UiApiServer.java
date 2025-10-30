package com.github.nawafalb.uiapi;

import io.javalin.plugin.bundled.CorsPluginConfig;
import io.javalin.Javalin;
import io.javalin.http.HttpStatus;

import java.sql.*;
import java.time.Instant;
import java.util.*;

public class UiApiServer {

    // --- config (env first, then defaults)
    private static final String DB_PATH = getenvOr("DB_PATH", "Data-api/src/db/userData.db");
    private static final int PORT = Integer.parseInt(getenvOr("PORT", "8080"));
    private static final String VERSION = getenvOr("APP_VERSION", "dev");

    public static void main(String[] args) {
        // tiny web server
       Javalin app = Javalin.create(config -> {
    config.http.defaultContentType = "application/json";
    config.bundledPlugins.enableCors(cors -> cors.add(CorsPluginConfig::anyHost));
});



        // routes
        app.get("/health", ctx -> {
            Map<String, Object> out = new LinkedHashMap<>();
            out.put("status", "ok");
            out.put("time", Instant.now().toString());
            out.put("version", VERSION);
            out.put("dbPath", DB_PATH);

            try (Connection c = conn()) {
                out.put("db", Map.of("connected", true));
            } catch (Exception e) {
                out.put("db", Map.of("connected", false, "error", e.getMessage()));
                ctx.status(HttpStatus.SERVICE_UNAVAILABLE).json(out);
                return;
            }
            ctx.json(out);
        });

        // latest AQI for a city OR nearest by lat/lon
        app.get("/aqi", ctx -> {
            String city = ctx.queryParam("city");
            String lat = ctx.queryParam("lat");
            String lon = ctx.queryParam("lon");

            String sql;
            List<Object> params = new ArrayList<>();

            // adjust table/cols if yours differ
            // expected: aqi_readings(city TEXT, lat REAL, lon REAL, aqi INTEGER, category TEXT, observed_at TEXT)
            if (city != null && !city.isBlank()) {
                sql = """
                      SELECT city, lat, lon, aqi, category, observed_at
                      FROM aqi_readings
                      WHERE LOWER(city) = LOWER(?)
                      ORDER BY datetime(observed_at) DESC
                      LIMIT 1
                      """;
                params.add(city);
            } else if (lat != null && lon != null) {
                double la = Double.parseDouble(lat), lo = Double.parseDouble(lon);
                sql = """
                      SELECT city, lat, lon, aqi, category, observed_at
                      FROM aqi_readings
                      ORDER BY ((lat-?)*(lat-?) + (lon-?)*(lon-?)) ASC,
                               datetime(observed_at) DESC
                      LIMIT 1
                      """;
                params.add(la); params.add(la); params.add(lo); params.add(lo);
            } else {
                ctx.status(HttpStatus.BAD_REQUEST)
                   .json(Map.of("error", "pass ?city=Name OR ?lat=..&lon=.."));
                return;
            }

            ctx.json(query(sql, params));
        });

        // latest UV (same vibe)
        app.get("/uv", ctx -> {
            String city = ctx.queryParam("city");
            String lat = ctx.queryParam("lat");
            String lon = ctx.queryParam("lon");

            String sql;
            List<Object> params = new ArrayList<>();

            // expected: uv_readings(city TEXT, lat REAL, lon REAL, uv_index REAL, risk TEXT, observed_at TEXT)
            if (city != null && !city.isBlank()) {
                sql = """
                      SELECT city, lat, lon, uv_index, risk, observed_at
                      FROM uv_readings
                      WHERE LOWER(city) = LOWER(?)
                      ORDER BY datetime(observed_at) DESC
                      LIMIT 1
                      """;
                params.add(city);
            } else if (lat != null && lon != null) {
                double la = Double.parseDouble(lat), lo = Double.parseDouble(lon);
                sql = """
                      SELECT city, lat, lon, uv_index, risk, observed_at
                      FROM uv_readings
                      ORDER BY ((lat-?)*(lat-?) + (lon-?)*(lon-?)) ASC,
                               datetime(observed_at) DESC
                      LIMIT 1
                      """;
                params.add(la); params.add(la); params.add(lo); params.add(lo);
            } else {
                ctx.status(HttpStatus.BAD_REQUEST)
                   .json(Map.of("error", "pass ?city=Name OR ?lat=..&lon=.."));
                return;
            }

            ctx.json(query(sql, params));
        });

        app.start(PORT);
        System.out.printf("UI API listening at http://localhost:%d  (db=%s)%n", PORT, DB_PATH);
    }

    // --- JDBC helpers
    private static Connection conn() throws SQLException {
        return DriverManager.getConnection("jdbc:sqlite:" + DB_PATH);
    }

    private static List<Map<String, Object>> query(String sql, List<Object> params) {
        List<Map<String, Object>> out = new ArrayList<>();
        try (Connection c = conn();
             PreparedStatement ps = c.prepareStatement(sql)) {

            for (int i = 0; i < params.size(); i++) ps.setObject(i + 1, params.get(i));

            try (ResultSet rs = ps.executeQuery()) {
                ResultSetMetaData md = rs.getMetaData();
                int n = md.getColumnCount();
                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    for (int i = 1; i <= n; i++) row.put(md.getColumnLabel(i), rs.getObject(i));
                    out.add(row);
                }
            }
        } catch (Exception e) {
            out.clear();
            out.add(Map.of("error", "DB_QUERY_FAILED", "detail", e.getMessage()));
        }
        return out;
    }

    private static String getenvOr(String k, String def) {
        String v = System.getenv(k);
        return (v == null || v.isBlank()) ? def : v;
    }
}
