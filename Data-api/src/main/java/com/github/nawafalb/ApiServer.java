package com.github.nawafalb;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

public class ApiServer {

    private static final String DB_URL = "jdbc:sqlite:src/db/userData.db";
    private static final ObjectMapper mapper = new ObjectMapper();

    public static void main(String[] args) throws IOException {
        int port = 8080;
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

        // AQI endpoints
        server.createContext("/airquality", new GetAllHandler("user_DataAirQuality"));
        server.createContext("/airquality/", new GetByIdHandler("user_DataAirQuality"));

        // UV endpoints
        server.createContext("/uv", new GetAllHandler("user_DataUV"));
        server.createContext("/uv/", new GetByIdHandler("user_DataUV"));

        // Generic endpoints
        server.createContext("/table", new GenericGetAllHandler());
        server.createContext("/table/", new GenericGetByIdHandler());

        server.setExecutor(null);
        server.start();
        System.out.println("Server started on http://localhost:" + port);
    }

    static class GetAllHandler implements HttpHandler {
        private final String tableName;
        GetAllHandler(String tableName) { this.tableName = tableName; }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "Method Not Allowed");
                return;
            }

            ArrayList<Map<String, Object>> results = new ArrayList<>();

            try (Connection conn = DriverManager.getConnection(DB_URL);
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT * FROM " + tableName)) {

                ResultSetMetaData meta = rs.getMetaData();
                int columnCount = meta.getColumnCount();

                while (rs.next()) {
                    Map<String, Object> row = new HashMap<>();
                    for (int i = 1; i <= columnCount; i++) {
                        row.put(meta.getColumnName(i), rs.getObject(i));
                    }
                    results.add(row);
                }

                sendResponse(exchange, 200, mapper.writeValueAsString(results));

            } catch (SQLException e) {
                e.printStackTrace();
                sendResponse(exchange, 500, "{\"error\": \"" + e.getMessage() + "\"}");
            }
        }
    }

    static class GetByIdHandler implements HttpHandler {
        private final String tableName;
        GetByIdHandler(String tableName) { this.tableName = tableName; }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            String[] parts = path.split("/");

            if (parts.length < 3 || parts[2].isEmpty()) {
                sendResponse(exchange, 400, "{\"error\": \"Missing ID\"}");
                return;
            }

            String id = parts[2];

            try (Connection conn = DriverManager.getConnection(DB_URL);
                 PreparedStatement pstmt = conn.prepareStatement("SELECT * FROM " + tableName + " WHERE id = ?")) {
                pstmt.setString(1, id);

                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    ResultSetMetaData meta = rs.getMetaData();
                    ObjectNode jsonNode = mapper.createObjectNode();
                    for (int i = 1; i <= meta.getColumnCount(); i++) {
                        jsonNode.put(meta.getColumnName(i), rs.getString(i));
                    }
                    sendResponse(exchange, 200, mapper.writeValueAsString(jsonNode));
                } else {
                    sendResponse(exchange, 404, "{\"error\": \"Record not found\"}");
                }

            } catch (SQLException e) {
                e.printStackTrace();
                sendResponse(exchange, 500, "{\"error\": \"" + e.getMessage() + "\"}");
            }
        }
    }

    // Generic GET all records
    static class GenericGetAllHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String query = exchange.getRequestURI().getQuery();
            if (query == null || !query.startsWith("name=")) {
                sendResponse(exchange, 400, "{\"error\":\"Missing table name\"}");
                return;
            }
            String tableName = query.substring(5);
            new GetAllHandler(tableName).handle(exchange);
        }
    }

    // Generic GET by ID
    static class GenericGetByIdHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath(); // /table/tableName/id
            String[] parts = path.split("/");
            if (parts.length != 4) {
                sendResponse(exchange, 400, "{\"error\":\"Usage: /table/{tableName}/{id}\"}");
                return;
            }
            String tableName = parts[2];
            String id = parts[3];

            new GetByIdHandler(tableName) {
                @Override
                public void handle(HttpExchange ex) throws IOException {
                    ex.setAttribute("idParam", id);
                    super.handle(ex);
                }
            }.handle(exchange);
        }
    }

    private static void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        byte[] bytes = response.getBytes();
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}
