-- Drops user_DataUV table if it already exists
DROP TABLE IF EXISTS user_DataUV;

-- Creates use_DataUV table with (User ID, latitude & longitude, uv index, and the time the uv were calculated)
CREATE TABLE user_DataUV (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  latitude REAL NOT NULL,
  longitude REAL NOT NULL,
  uv_index REAL,
  dateTime DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- Goes through the table trying to find an instance of matching latitude and longitude (Could be modified to take another variable)
CREATE INDEX IF NOT EXISTS idx_lat_lon ON user_DataUV (latitude, longitude);






