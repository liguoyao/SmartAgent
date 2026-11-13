-- Run before starting services. Flyway creates the tables in each database.
CREATE DATABASE IF NOT EXISTS smartagent_gateway
    CHARACTER SET utf8mb4 COLLATE utf8mb4_bin;
CREATE DATABASE IF NOT EXISTS smartagent_agent
    CHARACTER SET utf8mb4 COLLATE utf8mb4_bin;
CREATE DATABASE IF NOT EXISTS smartagent_conversation
    CHARACTER SET utf8mb4 COLLATE utf8mb4_bin;
