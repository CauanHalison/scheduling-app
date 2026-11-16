ALTER TABLE booking
    ADD COLUMN reminder_sent_at TIMESTAMPTZ NULL;

CREATE INDEX idx_booking_pending_reminder ON booking(start_time)
    WHERE status = 'CONFIRMED' AND reminder_sent_at IS NULL;