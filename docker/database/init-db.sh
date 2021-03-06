#!/usr/bin/env bash

set -e

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" <<-EOSQL
    CREATE USER annotation;
    CREATE DATABASE annotation
        WITH OWNER = annotation
        ENCODING ='UTF-8'
        CONNECTION LIMIT = -1;
EOSQL

psql -v ON_ERROR_STOP=1 --dbname=annotation --username "$POSTGRES_USER" <<-EOSQL
    CREATE SCHEMA annotation AUTHORIZATION annotation;
EOSQL

psql -v ON_ERROR_STOP=1 --dbname=annotation --username "$POSTGRES_USER" <<-EOSQL
    CREATE EXTENSION lo;
EOSQL
