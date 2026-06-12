-- Database-per-service: a single PostgreSQL instance, one database per domain service.
-- Executed once, on first container init (empty data volume), from /docker-entrypoint-initdb.d.
CREATE DATABASE orderdb;
CREATE DATABASE inventorydb;
CREATE DATABASE paymentdb;
CREATE DATABASE notificationdb;
