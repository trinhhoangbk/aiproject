-- =====================================================================
-- V005 — seed VN national holidays for 2026
-- PLAN-008 · DEC-003 (Default = official VN holidays by MOLISA notice)
-- Operator should run a yearly migration adding the next year's dates
-- as published by the Ministry of Labour, Invalids and Social Affairs.
-- =====================================================================

INSERT INTO core.holiday_calendar (date, kind, description) VALUES
    ('2026-01-01', 'NATIONAL', 'Tết Dương lịch'),
    ('2026-02-16', 'NATIONAL', 'Tết Nguyên đán — 29 tháng Chạp'),
    ('2026-02-17', 'NATIONAL', 'Tết Nguyên đán — Mùng 1'),
    ('2026-02-18', 'NATIONAL', 'Tết Nguyên đán — Mùng 2'),
    ('2026-02-19', 'NATIONAL', 'Tết Nguyên đán — Mùng 3'),
    ('2026-02-20', 'NATIONAL', 'Tết Nguyên đán — Mùng 4'),
    ('2026-04-26', 'NATIONAL', 'Giỗ Tổ Hùng Vương (10 tháng 3 âm lịch)'),
    ('2026-04-30', 'NATIONAL', 'Giải phóng miền Nam'),
    ('2026-05-01', 'NATIONAL', 'Quốc tế Lao động'),
    ('2026-09-02', 'NATIONAL', 'Quốc khánh')
ON CONFLICT (date) DO NOTHING;

-- NOTE: Exact Tết dates for 2026 are per the official 2026 MOLISA notice.
-- Operator should verify against the authoritative government bulletin before go-live.
-- Compensated workdays (COMPENSATED_WORKDAY) and company-specific days (COMPANY_DAY)
-- are added via the Hub UI by Admin / Operations Lead per DEC-003.
