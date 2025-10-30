package com.github.nawafalb;

import static spark.Spark.*;
import java.net.http.*;
import java.net.URI;
import com.fasterxml.jackson.databind.*;

public class ClassApiServer {
    private static final String DATA_API_URL = System.getenv().getOrDefault("DATA_API_URL", "http://localhost:8080");
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final HttpClient HTTP = HttpClient.newHttpClient();

    public static void main(String[] args) {
        port(8081);
        System.out.println("Class API @ http://localhost:8081 (talking to " + DATA_API_URL + ")");

        get("/health", (req, res) -> { res.type("application/json"); return "{\"ok\":true}"; });

        get("/combined", (req, res) -> {
            res.type("application/json");
            try {
                // --- call Data API
                var aqiHttp = HTTP.send(HttpRequest.newBuilder(URI.create(DATA_API_URL + "/airquality")).GET().build(),
                                        HttpResponse.BodyHandlers.ofString());
                var uvHttp  = HTTP.send(HttpRequest.newBuilder(URI.create(DATA_API_URL + "/uv")).GET().build(),
                                        HttpResponse.BodyHandlers.ofString());

                System.out.println("[ClassAPI] /airquality status=" + aqiHttp.statusCode());
                System.out.println("[ClassAPI] /airquality body=" + aqiHttp.body());
                System.out.println("[ClassAPI] /uv         status=" + uvHttp.statusCode());
                System.out.println("[ClassAPI] /uv         body=" + uvHttp.body());

                if (aqiHttp.statusCode() / 100 != 2 || uvHttp.statusCode() / 100 != 2) {
                    res.status(502);
                    return err("data-api bad status",
                               "airquality", String.valueOf(aqiHttp.statusCode()),
                               "uv",         String.valueOf(uvHttp.statusCode()));
                }

                // --- parse JSON safely (array or object)
                JsonNode aqiNode = JSON.readTree(aqiHttp.body());
                JsonNode uvNode  = JSON.readTree(uvHttp.body());

                JsonNode aqiObj = aqiNode.isArray() ? (aqiNode.size() > 0 ? aqiNode.get(0) : JSON.nullNode()) : aqiNode;
                JsonNode uvObj  = uvNode.isArray()  ? (uvNode.size()  > 0 ? uvNode.get(0)  : JSON.nullNode()) : uvNode;

                if (aqiObj.isNull() || uvObj.isNull() || aqiObj.isMissingNode() || uvObj.isMissingNode()) {
                    res.status(502);
                    return err("data-api returned empty arrays");
                }

                // --- YOUR KEYS: air_quality, uv_index
                double aqi = aqiObj.path("air_quality").asDouble(Double.NaN);
                double uv  = uvObj.path("uv_index").asDouble(Double.NaN);

                if (Double.isNaN(aqi) || Double.isNaN(uv)) {
                    System.out.println("[ClassAPI] Parsed aqiObj=" + aqiObj);
                    System.out.println("[ClassAPI] Parsed uvObj=" + uvObj);
                    res.status(502);
                    return err("missing fields",
                               "haveAqi", aqiObj.toString(),
                               "haveUv",  uvObj.toString());
                }

                String air = (aqi < 50 ? "Good Air" : "Poor Air");
                String sun = (uv  < 3  ? "Low UV"   : "High UV");
                String summary = air + " & " + sun;

                var out = JSON.createObjectNode();
                out.put("aqi", aqi);
                out.put("uv",  uv);
                out.put("summary", summary);
                return JSON.writeValueAsString(out);

            } catch (Exception e) {
                e.printStackTrace(); // print full stack to console
                res.status(500);
                return err("exception in class-api", "message", e.getClass().getSimpleName());
            }
        });
    }

    private static String err(String msg, String... kv) {
        ObjectNode o = JSON.createObjectNode().put("error", msg);
        for (int i = 0; i + 1 < kv.length; i += 2) o.put(kv[i], kv[i + 1]);
        try { return JSON.writeValueAsString(o); } catch (Exception ignore) { return "{\"error\":\"" + msg + "\"}"; }
    }
}
