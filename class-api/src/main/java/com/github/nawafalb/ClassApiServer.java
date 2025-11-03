package com.github.nawafalb;

import static spark.Spark.*;
import java.net.http.*;
import java.net.URI;
<<<<<<< Updated upstream
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
=======
import com.fasterxml.jackson.databind.*;
>>>>>>> Stashed changes
import com.fasterxml.jackson.databind.node.ObjectNode;

public class ClassApiServer {
    private static final String DATA_API_URL = System.getenv().getOrDefault("DATA_API_URL", "http://localhost:8080");
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final HttpClient client = HttpClient.newHttpClient();

    public static void main(String[] args) {
        port(8081);
        System.out.println("Class API Server @ http://localhost:8081 (data=" + DATA_API_URL + ")");

        get("/health", (req, res) -> { res.type("application/json"); return "{\"ok\":true}"; });

        get("/combined", (req, res) -> {
            res.type("application/json");
            try {
                var aqiRes = client.send(HttpRequest.newBuilder(URI.create(DATA_API_URL + "/airquality")).GET().build(),
                                         HttpResponse.BodyHandlers.ofString());
                var uvRes  = client.send(HttpRequest.newBuilder(URI.create(DATA_API_URL + "/uv")).GET().build(),
                                         HttpResponse.BodyHandlers.ofString());

                if (aqiRes.statusCode() / 100 != 2 || uvRes.statusCode() / 100 != 2) {
                    res.status(502);
                    return jsonErr("data-api bad status",
                                   "airquality", String.valueOf(aqiRes.statusCode()),
                                   "uv",         String.valueOf(uvRes.statusCode()));
                }

                // Parse arrays (or object fallback)
                JsonNode aqiNode = mapper.readTree(aqiRes.body());
                JsonNode uvNode  = mapper.readTree(uvRes.body());

                JsonNode aqiObj = aqiNode.isArray() && aqiNode.size() > 0 ? aqiNode.get(0) : aqiNode;
                JsonNode uvObj  = uvNode.isArray()  && uvNode.size()  > 0 ? uvNode.get(0)  : uvNode;

                if (aqiObj.isMissingNode() || uvObj.isMissingNode()) {
                    res.status(502);
                    return jsonErr("data-api returned empty arrays");
                }

                // ✅ match your keys exactly
                double aqi = aqiObj.path("air_quality").asDouble(Double.NaN);
                double uv  = uvObj.path("uv_index").asDouble(Double.NaN);

                if (Double.isNaN(aqi) || Double.isNaN(uv)) {
                    res.status(502);
                    return jsonErr("missing fields", "haveAqi", aqiObj.toString(), "haveUv", uvObj.toString());
                }

                String air = (aqi < 50 ? "Good Air" : "Poor Air");
                String sun = (uv < 3 ? "Low UV" : "High UV");
                String summary = air + " & " + sun;

                var out = mapper.createObjectNode();
                out.put("aqi", aqi);
                out.put("uv", uv);
                out.put("summary", summary);
                return mapper.writeValueAsString(out);

            } catch (Exception e) {
                e.printStackTrace();
                res.status(500);
                return jsonErr("exception in class-api", "message", e.getMessage() == null ? "null" : e.getMessage());
            }
        });
    }

    private static String jsonErr(String msg, String... kv) {
        ObjectNode o = mapper.createObjectNode().put("error", msg);
        for (int i = 0; i + 1 < kv.length; i += 2) o.put(kv[i], kv[i + 1]);
        try { return mapper.writeValueAsString(o); } catch (Exception e) { return "{\"error\":\"" + msg + "\"}"; }
    }
}
