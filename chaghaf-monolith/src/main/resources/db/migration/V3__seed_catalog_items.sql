-- ═══════════════════════════════════════════════════════════════
-- V3 — Catalogue initial : boissons & snacks marocains
-- Idempotent : on n'insère que si la table est vide pour éviter
-- les doublons sur une DB déjà peuplée.
-- ═══════════════════════════════════════════════════════════════

INSERT INTO catalog_items (type, name, description, price, emoji, available, stock_quantity, category)
SELECT * FROM (VALUES
    -- ── Boissons marocaines ──────────────────────────────────────
    ('BOISSON', 'Thé à la menthe',         'Atay nous-nous traditionnel, menthe fraîche',  15.00, '🍵', TRUE, 200, 'Chaud'),
    ('BOISSON', 'Café noir',                'Espresso',                                      12.00, '☕', TRUE, 200, 'Chaud'),
    ('BOISSON', 'Nous-nous',                'Café au lait moitié-moitié',                    15.00, '☕', TRUE, 200, 'Chaud'),
    ('BOISSON', 'Café crème',               'Café avec lait mousse',                         18.00, '☕', TRUE, 200, 'Chaud'),
    ('BOISSON', 'Cappuccino',               'Espresso, lait vapeur, mousse',                 22.00, '☕', TRUE, 150, 'Chaud'),
    ('BOISSON', 'Thé vert nature',          'Thé Gunpowder sans sucre',                      12.00, '🍵', TRUE, 200, 'Chaud'),
    ('BOISSON', 'Chocolat chaud',           'Lait, cacao, sucre',                            22.00, '🍫', TRUE, 100, 'Chaud'),
    ('BOISSON', 'Jus d''orange frais',      'Pressé minute',                                 20.00, '🍊', TRUE, 80,  'Frais'),
    ('BOISSON', 'Jus d''avocat',            'Avocat, lait, sucre',                           25.00, '🥑', TRUE, 60,  'Frais'),
    ('BOISSON', 'Jus de fraise',            'De saison',                                     25.00, '🍓', TRUE, 50,  'Frais'),
    ('BOISSON', 'Citronnade maison',        'Citron, eau, menthe',                           15.00, '🍋', TRUE, 100, 'Frais'),
    ('BOISSON', 'Lait d''amande',           'Amandes broyées, eau de fleur d''oranger',      22.00, '🥛', TRUE, 60,  'Frais'),
    ('BOISSON', 'Eau minérale 50cl',        'Sidi Ali / Aïn Saïss',                           8.00, '💧', TRUE, 300, 'Frais'),
    ('BOISSON', 'Eau gazeuse',              'Oulmès',                                        10.00, '💧', TRUE, 150, 'Frais'),
    ('BOISSON', 'Coca-Cola 33cl',           'Boîte',                                         12.00, '🥤', TRUE, 200, 'Soda'),
    ('BOISSON', 'Hawaï',                    'Soda local ananas',                             12.00, '🥤', TRUE, 150, 'Soda'),
    ('BOISSON', 'Pom''s',                   'Soda pomme local',                              12.00, '🥤', TRUE, 100, 'Soda'),

    -- ── Snacks marocains ─────────────────────────────────────────
    ('SNACK',   'Msemmen',                  'Crêpe feuilletée, miel ou fromage',              8.00, '🥞', TRUE, 80,  'Petit-déj'),
    ('SNACK',   'Harcha',                   'Galette de semoule, beurre & miel',              8.00, '🍞', TRUE, 80,  'Petit-déj'),
    ('SNACK',   'Baghrir',                  'Crêpe mille trous, miel et beurre fondu',       10.00, '🥞', TRUE, 60,  'Petit-déj'),
    ('SNACK',   'Sfenj',                    'Beignet marocain traditionnel',                  5.00, '🍩', TRUE, 100, 'Petit-déj'),
    ('SNACK',   'Briouates au fromage',     'Feuilles de brick, fromage, herbes',            15.00, '🥟', TRUE, 60,  'Salé'),
    ('SNACK',   'Briouates à la viande',    'Brick, viande hachée épicée',                   20.00, '🥟', TRUE, 50,  'Salé'),
    ('SNACK',   'Mini pastilla poulet',     'Poulet, amandes, cannelle, sucre glace',        25.00, '🥧', TRUE, 40,  'Salé'),
    ('SNACK',   'Sandwich kefta',           'Pain, kefta, oignons, tomates, harissa',        30.00, '🥪', TRUE, 40,  'Sandwich'),
    ('SNACK',   'Sandwich poulet',          'Pain, escalope, salade, sauce',                 28.00, '🥪', TRUE, 40,  'Sandwich'),
    ('SNACK',   'Sandwich thon',            'Pain, thon, olives, tomates',                   25.00, '🥪', TRUE, 40,  'Sandwich'),
    ('SNACK',   'Mini tagine kefta',        'Tagine individuel, œuf, kefta',                 35.00, '🍲', TRUE, 25,  'Plat'),
    ('SNACK',   'Mini tagine poulet',       'Tagine individuel poulet citron olives',        38.00, '🍲', TRUE, 25,  'Plat'),
    ('SNACK',   'Salade marocaine',         'Tomates, concombre, oignons, coriandre',        18.00, '🥗', TRUE, 50,  'Salade'),
    ('SNACK',   'Bissara',                  'Soupe de fèves, cumin, huile d''olive',         15.00, '🍜', TRUE, 40,  'Soupe'),
    ('SNACK',   'Harira',                   'Soupe traditionnelle ramadanesque',             18.00, '🍜', TRUE, 40,  'Soupe'),
    ('SNACK',   'Cornes de gazelle',        'Pâtisserie aux amandes',                        12.00, '🥮', TRUE, 80,  'Pâtisserie'),
    ('SNACK',   'Chebakia',                 'Sésame, miel, fleur d''oranger',                 8.00, '🍯', TRUE, 100, 'Pâtisserie'),
    ('SNACK',   'Ghriba',                   'Sablé semoule / amandes',                        8.00, '🍪', TRUE, 100, 'Pâtisserie'),
    ('SNACK',   'Crêpe au miel',            'Crêpe sucrée, miel, beurre',                    20.00, '🥞', TRUE, 50,  'Sucré'),
    ('SNACK',   'Fruits de saison',         'Assortiment du jour',                           15.00, '🍎', TRUE, 60,  'Sucré')
) AS seed(type, name, description, price, emoji, available, stock_quantity, category)
WHERE NOT EXISTS (SELECT 1 FROM catalog_items WHERE catalog_items.name = seed.name);
