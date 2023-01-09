CREATE TABLE IF NOT EXISTS mikrofrontend
(
    fnr        VARCHAR(11) NOT NULL,
    data       jsonb       NOT NULL,
    created  TIMESTAMP,
    last_changed TIMESTAMP
);

CREATE TABLE IF NOT EXISTS historikk
(
    fnr  VARCHAR(11) NOT NULL,
    date       TIMESTAMP,
    old_data jsonb,
    new_data   jsonb
)

