CREATE TABLE event (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    total_seats INT NOT NULL
);

CREATE TABLE seat (
    id BIGSERIAL PRIMARY KEY,
    event_id BIGINT NOT NULL REFERENCES event(id),
    label VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',
    buyer VARCHAR(255),
    version INT NOT NULL DEFAULT 0,
    CONSTRAINT uq_seat_event_label UNIQUE (event_id, label)
);

CREATE INDEX idx_seat_event_status ON seat(event_id, status);