CREATE EXTENSION IF NOT EXISTS btree_gist;

CREATE TYPE timerange AS RANGE (subtype = time);

CREATE TABLE availability_schedule (
    id UUID PRIMARY KEY,
    provider_id UUID NOT NULL,
    day_of_week VARCHAR(20) NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_availability_schedule_provider FOREIGN KEY (provider_id) REFERENCES provider(id) ON DELETE CASCADE,
    CONSTRAINT chk_availability_schedule_time CHECK (start_time < end_time),
    CONSTRAINT chk_availability_schedule_day_of_week CHECK (
        day_of_week IN ('MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY')
    ),
    CONSTRAINT excl_availability_schedule_no_overlap EXCLUDE USING gist(
        provider_id WITH =,
        day_of_week WITH =,
        timerange(start_time, end_time, '[)') WITH &&
    )
);

CREATE INDEX idx_availability_schedule_provider ON availability_schedule(provider_id);
