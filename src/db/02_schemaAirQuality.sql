-- Drops user_data table if it already exists
DROP TABLE IF EXISTS user_DataAirQuality;

-- Creates use_data table with (User ID, latitude & longitude, uv index, air quality, and the time the uv/air quality were calculated)
CREATE TABLE user_DataAirQuality (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  latitude REAL NOT NULL,
  longitude REAL NOT NULL,
  air_Quality REAL,
);

-- Goes through the table trying to find an instance of matching latitude and longitude (Could be modified to take another variable)
CREATE INDEX IF NOT EXISTS idx_lat_lon ON user_DataAirQuality (latitude, longitude);
