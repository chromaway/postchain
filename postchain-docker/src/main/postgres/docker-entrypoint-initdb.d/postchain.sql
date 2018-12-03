-- SQL init script for Postchain node

CREATE DATABASE postchain;

CREATE ROLE postchain WITH LOGIN ENCRYPTED PASSWORD 'postchain';

GRANT ALL ON DATABASE postchain TO postchain;
