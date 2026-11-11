ALTER TABLE otp_challenges ADD COLUMN email_hash VARCHAR(64);

-- Old phone-only challenges cannot prove the email bound to a new request.
UPDATE otp_challenges
SET used_at = now()
WHERE used_at IS NULL
  AND email_hash IS NULL;

-- Normalize existing profile emails first; keep one (the oldest account) for any legacy duplicate.
UPDATE users
SET email = NULLIF(lower(btrim(email)), '')
WHERE email IS NOT NULL;

WITH ranked_emails AS (
    SELECT id, row_number() OVER (PARTITION BY email ORDER BY created_at ASC, id ASC) AS duplicate_number
    FROM users
    WHERE email IS NOT NULL
)
UPDATE users AS account
SET email = NULL
FROM ranked_emails AS ranked
WHERE account.id = ranked.id
  AND ranked.duplicate_number > 1;

CREATE UNIQUE INDEX uq_users_email_case_insensitive
    ON users (lower(email))
    WHERE email IS NOT NULL;

-- Legacy system role rows are kept until a successful OTP login, when the new backend re-evaluates them from the configured email+phone pair.
