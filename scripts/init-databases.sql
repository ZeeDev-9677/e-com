-- Local convenience: ONE postgres container, FOUR databases. Production: one RDS instance/cluster per service
-- (or at least separate databases + separate credentials). No service may read another service's tables.
CREATE DATABASE userdb;
CREATE DATABASE orderdb;
CREATE DATABASE paymentdb;
CREATE DATABASE notificationdb;
