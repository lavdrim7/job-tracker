CREATE TABLE applications (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company      VARCHAR(255) NOT NULL,
    position     VARCHAR(255) NOT NULL,
    status       VARCHAR(20)  NOT NULL,
    deadline     DATE,
    applied_date DATE,
    job_url      VARCHAR(2048),
    notes        TEXT,
    created_at   TIMESTAMPTZ  NOT NULL,
    updated_at   TIMESTAMPTZ  NOT NULL,
    CONSTRAINT applications_status_check
        CHECK (status IN ('WISHLIST', 'APPLIED', 'INTERVIEW', 'OFFER', 'REJECTED'))
);

CREATE INDEX idx_applications_status ON applications (status);
CREATE INDEX idx_applications_deadline ON applications (deadline);
