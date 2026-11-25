CREATE TABLE users (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email         VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL,
    CONSTRAINT users_email_key UNIQUE (email),
    -- E-post lagres alltid med små bokstaver, så unikhet er ikke avhengig av store/små.
    CONSTRAINT users_email_lowercase CHECK (email = lower(email))
);

-- Kolonnen er nullable fordi eksisterende rader fra før innlogging ikke har noen eier.
-- Slike rader er usynlige for alle brukere. Applikasjonen setter alltid user_id på nye søknader.
ALTER TABLE applications
    ADD COLUMN user_id UUID REFERENCES users (id) ON DELETE CASCADE;

CREATE INDEX idx_applications_user_id ON applications (user_id);
