CREATE EXTENSION IF NOT EXISTS btree_gist;


CREATE TABLE schedule_override (
    id UUID PRIMARY KEY,
    provider_id UUID NOT NULL,
    start_time TIMESTAMP WITH TIME ZONE NOT NULL,
    end_time TIMESTAMP WITH TIME ZONE NOT NULL,
    reason VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_schedule_override_provider FOREIGN KEY (provider_id) REFERENCES provider(id) ON DELETE CASCADE,
    CONSTRAINT chk_schedule_override_time CHECK ( start_time < end_time ),
    CONSTRAINT excl_schedule_override_no_overlap EXCLUDE USING gist(
        provider_id WITH =,
        tstzrange(start_time, end_time, '[)') WITH &&
    )
);

CREATE INDEX idx_schedule_override_provider_end ON schedule_override(provider_id, end_time);