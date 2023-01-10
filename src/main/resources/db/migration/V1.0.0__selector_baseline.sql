CREATE TABLE IF NOT EXISTS person
(
    ident        VARCHAR(11) NOT NULL,
    microfrontends       jsonb       NOT NULL,
    created  TIMESTAMP,
    last_changed TIMESTAMP
);

CREATE TABLE IF NOT EXISTS changelog
(
    ident  VARCHAR(11) NOT NULL,
    date       TIMESTAMP,
    old_data jsonb,
    new_data   jsonb
)

