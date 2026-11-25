ALTER TABLE booking
    DROP CONSTRAINT IF EXISTS chk_booking_status,
    ADD CONSTRAINT chk_booking_status CHECK (
        status IN ('CONFIRMED', 'CANCELED', 'COMPLETED')
    );

CREATE INDEX idx_booking_confirmed_end_time ON booking(end_time)
    WHERE status = 'CONFIRMED';
