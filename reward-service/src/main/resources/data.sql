-- Badge seed — mirrors the app mockup badges
INSERT IGNORE INTO badges (id, name, description, required_points, required_deposits, required_locations) VALUES
('first-checkin',    'Eerste Check-in',      'Lever je eerste fles in via de app.',              0,   1,   0),
('recycling-pro',    'Recycling Pro',        'Behaal 200 punten door statiegeld in te leveren.', 200, 0,   0),
('ocean-saver',      'Redder van de Oceaan', 'Lever 10 flessen in en bescherm de oceanen.',      0,   10,  0),
('plastic-pack-1',   'Plastiek Pakket I',    'Lever 25 PET-flessen in.',                         0,   25,  0),
('plastic-pack-2',   'Plastiek Pakket II',   'Lever 50 PET-flessen in.',                         0,   50,  0),
('glass-master',     'Glas Meester',         'Lever glas in bij 3 verschillende locaties.',      0,   0,   3),
('battery-champion', 'Batterij Kampioen',    'Behaal 1000 punten in totaal.',                    1000, 0,  0),
('eco-family',       'Eco-Familie',          'Gebruik de app 30 dagen op rij.',                  0,   0,   0),
('green-hero',       'Groene Held',          'Behaal 3000 punten en word een groene held.',      3000, 0,  0),
('sorting-master',   'Meester Sorteerder',   'Scan 20 verschillende producten.',                 0,   20,  0);

-- INSERT IGNORE never updates rows that already exist, so changing the seed above does not reach
-- existing databases. This UPDATE is idempotent: eco-family now unlocks on a 30-day streak instead
-- of 30 deposits (ADR 0001). Badges already earned stay in user_badges and are never revoked.
UPDATE badges SET required_streak_days = 30, required_deposits = 0 WHERE id = 'eco-family';

-- Referral achievement (ADR 0002). INSERT IGNORE seeds fresh databases; the UPDATE below is idempotent
-- and brings an existing row (and the new required_referrals column) to the intended values.
INSERT IGNORE INTO badges (id, name, description, required_points, required_deposits, required_locations) VALUES
('eco-ambassador', 'Eco-Ambassadeur', 'Nodig 3 vrienden uit die hun eerste statiegeld inleveren.', 0, 0, 0);

UPDATE badges SET name = 'Eco-Ambassadeur',
                  description = 'Nodig 3 vrienden uit die hun eerste statiegeld inleveren.',
                  required_referrals = 3
WHERE id = 'eco-ambassador';
