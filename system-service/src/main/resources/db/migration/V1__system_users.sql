CREATE TABLE IF NOT EXISTS sys_users (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  username VARCHAR(64) UNIQUE,
  password VARCHAR(128),
  display_name VARCHAR(128),
  created_at DATETIME
);
