#!/bin/bash
# Creates one database per Swizima service on the FIRST start of the container
# (scripts in /docker-entrypoint-initdb.d only run when the data directory is empty).
# Databases come from POSTGRES_MULTIPLE_DATABASES (comma-separated).
set -euo pipefail

if [ -z "${POSTGRES_MULTIPLE_DATABASES:-}" ]; then
  echo "POSTGRES_MULTIPLE_DATABASES not set - no extra databases created"
  exit 0
fi

IFS=',' read -ra DATABASES <<< "$POSTGRES_MULTIPLE_DATABASES"
for db in "${DATABASES[@]}"; do
  db="$(echo "$db" | xargs)"   # trim spaces
  [ -z "$db" ] && continue
  if [[ ! "$db" =~ ^[a-zA-Z_][a-zA-Z0-9_]*$ ]]; then
    echo "Skipping invalid database name: $db" >&2
    continue
  fi
  echo "Creating database '$db'"
  psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
    SELECT 'CREATE DATABASE $db OWNER "$POSTGRES_USER"'
    WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = '$db')\gexec
EOSQL
done
