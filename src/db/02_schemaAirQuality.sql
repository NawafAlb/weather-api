-- Drops user_DataAirQuality table if it already exists
DROP TABLE IF EXISTS user_DataAirQuality;

-- Creates user_DataAirQuality table with (User ID, latitude & longitude, air quality)
CREATE TABLE user_DataAirQuality (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  latitude REAL NOT NULL,
  longitude REAL NOT NULL,
  air_Quality REAL,
);

-- Goes through the table trying to find an instance of matching latitude and longitude (Could be modified to search for different variables)
CREATE INDEX IF NOT EXISTS idx_lat_lon ON user_DataAirQuality (latitude, longitude);
