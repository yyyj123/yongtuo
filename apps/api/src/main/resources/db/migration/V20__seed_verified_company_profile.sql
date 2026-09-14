-- Approved source: 01-PRD.md section 2 and the frozen V1 company profile.
-- English prose is an AI draft, never implicitly confirmed or published.
INSERT INTO site_config(config_key,value_zh,value_en,value_type,english_status) VALUES
('brand','勇拓五金实业','YONGTUO','TEXT','CONFIRMED'),
('company_name','勇拓五金实业','YONGTUO','TEXT','CONFIRMED'),
('company_legal_name',NULL,NULL,'TEXT','EMPTY'),
('founded_year','2011','2011','NUMBER','CONFIRMED'),
('company_profile',
'<p>勇拓五金实业成立于2011年，是专注精密五金零部件加工的实体制造企业。</p><p>具备普通车床和CNC加工中心，支持根据客户图纸和样品定制，可加工不锈钢、铜、铝、铁等金属。</p><p>加工产品包括轴类、轴套、螺丝和连接部件，工艺包括车削、成型、精加工和尺寸校准。</p>',
'<p>Established in 2011, YONGTUO is a manufacturer focused on precision hardware components.</p><p>Its equipment includes conventional lathes and CNC machining centers. Custom machining is available based on customer drawings and samples, using metals including stainless steel, copper, aluminum and iron.</p><p>Components include shafts, bushings, screws and connecting parts. Processes include turning, forming, finishing and dimensional calibration.</p>',
'HTML','AI_DRAFT')
ON DUPLICATE KEY UPDATE config_key=VALUES(config_key);

UPDATE home_section h JOIN site_config c ON c.config_key='company_profile'
SET h.title_zh='关于勇拓',h.title_en='About YONGTUO',h.content_zh=c.value_zh,h.content_en=c.value_en,
    h.english_status='AI_DRAFT',h.enabled=1
WHERE h.section_code='ABOUT' AND h.title_zh IS NULL AND h.content_zh IS NULL AND h.content_en IS NULL;
