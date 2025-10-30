package com.github;

import java.nio.file.*;
import java.sql.*;
import java.util.*;

public class UvVerifier {
    public static void main(String[] args) {
        String dbUrl = "jdbc:sqlite:src/db/userData.db";
        String summaryFile = "summary_uv.txt";

        try {
            // Read summary file
            Path path = Path.of(summaryFile);
            if (!Files.exists(path)) {
                System.err.println("Summary file not found: " + summaryFile);
                return;
            }

            List<String> lines = Files.readAllLines(path);
            int expectedCount = -1;
            for (String line : lines) {
                if (line.startsWith("Rows inserted:")) {
                    expectedCount = Integer.parseInt(line.replace("Rows inserted:", "").trim());
                }
            }

            if (expectedCount == -1) {
                System.err.println("Could not find 'Rows inserted' in summary file");
                return;
            }

            // Count rows in user_DataUV
            int actualCount = 0;
            try (Connection conn = DriverManager.getConnection(dbUrl);
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT COUNT(*) AS n FROM user_DataUV")) {
                if (rs.next()) {
                    actualCount = rs.getInt("n");
                }
            }

            // Compare
            System.out.println("Summary rows: " + expectedCount);
            System.out.println("Database rows: " + actualCount);

            if (expectedCount == actualCount) {
                System.out.println("UV verification PASSED");
            } else {
                System.out.println("UV verification FAILED");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}