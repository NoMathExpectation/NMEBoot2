#!/usr/bin/env bash
set -e

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
	CREATE USER playground WITH PASSWORD 'playground';
  CREATE DATABASE playground;
  GRANT ALL PRIVILEGES ON DATABASE playground TO playground;
  \c playground
  GRANT CREATE ON SCHEMA PUBLIC TO playground;
EOSQL