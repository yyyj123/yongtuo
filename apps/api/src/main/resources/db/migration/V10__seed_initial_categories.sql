-- Initial product categories are data-driven; CNC remains a separate service module.
-- Existing live rows are addressed by stable slug and are never updated.
INSERT INTO product_category (parent_id, name_zh, slug, sort_order, status)
SELECT NULL, '五金产品', 'wujin-chanpin', 10, 'ACTIVE'
WHERE NOT EXISTS (
    SELECT 1 FROM product_category WHERE slug = 'wujin-chanpin' AND deleted_at IS NULL
);

INSERT INTO product_category (parent_id, name_zh, slug, sort_order, status)
SELECT NULL, '机械加工件', 'jixie-jiagongjian', 20, 'ACTIVE'
WHERE NOT EXISTS (
    SELECT 1 FROM product_category WHERE slug = 'jixie-jiagongjian' AND deleted_at IS NULL
);

-- A live root slug attached below another category is a structural conflict. Let the
-- unique live-slug key abort the migration rather than silently moving that content.
INSERT INTO product_category (parent_id, name_zh, slug, sort_order, status)
SELECT NULL, 'V10 seed conflict: wujin-chanpin must be a root', 'wujin-chanpin', 10, 'ACTIVE'
WHERE EXISTS (
    SELECT 1 FROM product_category
    WHERE slug = 'wujin-chanpin' AND deleted_at IS NULL AND parent_id IS NOT NULL
);

INSERT INTO product_category (parent_id, name_zh, slug, sort_order, status)
SELECT NULL, 'V10 seed conflict: jixie-jiagongjian must be a root', 'jixie-jiagongjian', 20, 'ACTIVE'
WHERE EXISTS (
    SELECT 1 FROM product_category
    WHERE slug = 'jixie-jiagongjian' AND deleted_at IS NULL AND parent_id IS NOT NULL
);

-- A live child slug attached to a different parent is also a structural conflict. The
-- duplicate slug is intentional: it produces a transactional, diagnosable failure.
INSERT INTO product_category (parent_id, name_zh, slug, sort_order, status)
SELECT p.id, c.name_zh, c.slug, c.sort_order, 'ACTIVE'
FROM product_category p
JOIN (
    SELECT '石膏板膨胀螺丝' AS name_zh, 'shigao-ban-pengzhang-luosi' AS slug, 10 AS sort_order
    UNION ALL SELECT '插销', 'chaxiao', 20
    UNION ALL SELECT '螺钉', 'luoding', 30
    UNION ALL SELECT '牙条', 'yatia', 40
    UNION ALL SELECT '垫圈', 'dianquan', 50
    UNION ALL SELECT '螺母', 'luomu', 60
    UNION ALL SELECT '铆钉', 'maoding', 70
    UNION ALL SELECT '双头螺柱', 'shuangtou-luozhu', 80
    UNION ALL SELECT '自攻螺丝', 'zigong-luosi', 90
    UNION ALL SELECT '螺栓', 'luoshuan', 100
    UNION ALL SELECT '混凝土锚钉', 'hunningtu-maoding', 110
    UNION ALL SELECT '锚', 'mao', 120
) c ON p.slug = 'wujin-chanpin' AND p.deleted_at IS NULL
WHERE EXISTS (
    SELECT 1 FROM product_category existing
    WHERE existing.slug = c.slug AND existing.deleted_at IS NULL
      AND NOT (existing.parent_id <=> p.id)
);

INSERT INTO product_category (parent_id, name_zh, slug, sort_order, status)
SELECT p.id, c.name_zh, c.slug, c.sort_order, 'ACTIVE'
FROM product_category p
JOIN (
    SELECT '顶针' AS name_zh, 'dingzhen' AS slug, 10 AS sort_order
    UNION ALL SELECT '法兰盘', 'falanpan', 20
    UNION ALL SELECT '螺丝扣', 'luosikou', 30
) c ON p.slug = 'jixie-jiagongjian' AND p.deleted_at IS NULL
WHERE EXISTS (
    SELECT 1 FROM product_category existing
    WHERE existing.slug = c.slug AND existing.deleted_at IS NULL
      AND NOT (existing.parent_id <=> p.id)
);

INSERT INTO product_category (parent_id, name_zh, slug, sort_order, status)
SELECT p.id, c.name_zh, c.slug, c.sort_order, 'ACTIVE'
FROM product_category p
JOIN (
    SELECT '石膏板膨胀螺丝' AS name_zh, 'shigao-ban-pengzhang-luosi' AS slug, 10 AS sort_order
    UNION ALL SELECT '插销', 'chaxiao', 20
    UNION ALL SELECT '螺钉', 'luoding', 30
    UNION ALL SELECT '牙条', 'yatia', 40
    UNION ALL SELECT '垫圈', 'dianquan', 50
    UNION ALL SELECT '螺母', 'luomu', 60
    UNION ALL SELECT '铆钉', 'maoding', 70
    UNION ALL SELECT '双头螺柱', 'shuangtou-luozhu', 80
    UNION ALL SELECT '自攻螺丝', 'zigong-luosi', 90
    UNION ALL SELECT '螺栓', 'luoshuan', 100
    UNION ALL SELECT '混凝土锚钉', 'hunningtu-maoding', 110
    UNION ALL SELECT '锚', 'mao', 120
) c ON p.slug = 'wujin-chanpin' AND p.deleted_at IS NULL
WHERE NOT EXISTS (
    SELECT 1 FROM product_category existing
    WHERE existing.slug = c.slug AND existing.deleted_at IS NULL
);

INSERT INTO product_category (parent_id, name_zh, slug, sort_order, status)
SELECT p.id, c.name_zh, c.slug, c.sort_order, 'ACTIVE'
FROM product_category p
JOIN (
    SELECT '顶针' AS name_zh, 'dingzhen' AS slug, 10 AS sort_order
    UNION ALL SELECT '法兰盘', 'falanpan', 20
    UNION ALL SELECT '螺丝扣', 'luosikou', 30
) c ON p.slug = 'jixie-jiagongjian' AND p.deleted_at IS NULL
WHERE NOT EXISTS (
    SELECT 1 FROM product_category existing
    WHERE existing.slug = c.slug AND existing.deleted_at IS NULL
);
