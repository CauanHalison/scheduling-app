CREATE EXTENSION IF NOT EXISTS btree_gist;

CREATE TABLE booking (
    id UUID PRIMARY KEY,
    event_type_id UUID NOT NULL,
    provider_id UUID NOT NULL,
    client_name VARCHAR(255) NOT NULL,
    client_email VARCHAR(255) NOT NULL,
    start_time TIMESTAMP WITH TIME ZONE NOT NULL,
    end_time TIMESTAMP WITH TIME ZONE NOT NULL,
    blocked_until TIMESTAMP WITH TIME ZONE NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'CONFIRMED',
    cancel_token UUID NOT NULL,
    cancellation_reason VARCHAR(500),
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_booking_event_type FOREIGN KEY(event_type_id) REFERENCES event_type(id) ON DELETE RESTRICT ,
    CONSTRAINT fk_booking_provider FOREIGN KEY(provider_id) REFERENCES provider(id) ON DELETE CASCADE,
    CONSTRAINT uk_booking_cancel_token UNIQUE (cancel_token),
    CONSTRAINT chk_booking_time CHECK (start_time < end_time AND end_time <= blocked_until),
    CONSTRAINT chk_booking_status CHECK (
        status IN ('CONFIRMED', 'CANCELED')
    ),
    CONSTRAINT excl_booking_no_overlap EXCLUDE USING gist(
        provider_id WITH =,
        tstzrange(start_time, blocked_until, '[)') WITH &&
    ) WHERE (status = 'CONFIRMED')
);

CREATE INDEX idx_booking_provider_start_time ON booking(provider_id, start_time);
CREATE INDEX idx_booking_client_email ON booking(client_email);