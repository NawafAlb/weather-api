package com.github.nawafalb;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseHelper {
  private static final String URL = "jdbc:sqlite:src/db/userData.db";

  public static Connection connect() throws SQLException {
    return DriverManager.getConnection(URL);
  }
}
