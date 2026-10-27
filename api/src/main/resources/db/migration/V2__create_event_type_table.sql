CREATE TABLE event_type (
    id UUID PRIMARY KEY,
    provider_id UUID NOT NULL,
    title VARCHAR(100) NOT NULL,
    slug VARCHAR(100) NOT NULL,
    description TEXT,
    duration_minutes INTEGER NOT NULL,
    buffer_after_minutes INTEGER NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_event_type_provider FOREIGN KEY (provider_id) REFERENCES provider(id) ON DELETE CASCADE,
    CONSTRAINT uk_event_type_provider_slug UNIQUE (provider_id, slug),
    CONSTRAINT chk_event_type_duration_positive CHECK (duration_minutes > 0),
    CONSTRAINT chk_event_type_buffer_non_negative CHECK (buffer_after_minutes >= 0)
);

CREATE INDEX idx_event_type_provider ON event_type(provider_id);
