INSERT INTO roles (code, display_name) VALUES ('USER', 'Oddiy foydalanuvchi') ON CONFLICT (code) DO NOTHING;
