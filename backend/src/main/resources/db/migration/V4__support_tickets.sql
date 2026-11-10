CREATE TABLE support_tickets (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    category VARCHAR(24) NOT NULL,
    subject VARCHAR(180) NOT NULL,
    message TEXT NOT NULL,
    status VARCHAR(24) NOT NULL DEFAULT 'OPEN',
    admin_note TEXT NOT NULL DEFAULT '',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT support_tickets_category_check CHECK (category IN ('GENERAL', 'COMPLAINT', 'TECHNICAL')),
    CONSTRAINT support_tickets_status_check CHECK (status IN ('OPEN', 'IN_REVIEW', 'RESOLVED', 'CLOSED'))
);
CREATE INDEX idx_support_tickets_user_created ON support_tickets(user_id, created_at DESC);
CREATE INDEX idx_support_tickets_status_created ON support_tickets(status, created_at DESC);
