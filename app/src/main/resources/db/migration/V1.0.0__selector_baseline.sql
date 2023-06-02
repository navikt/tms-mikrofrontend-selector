CREATE TABLE IF NOT EXISTS person
(
    ident        VARCHAR(11) PRIMARY KEY,
    microfrontends       jsonb       NOT NULL,
    created  TIMESTAMP,
    last_changed TIMESTAMP
);

CREATE TABLE IF NOT EXISTS changelog
(
    ident  VARCHAR(11) REFERENCES person(ident),
    timestamp      TIMESTAMP,
    original_data jsonb,
    new_data   jsonb
)

