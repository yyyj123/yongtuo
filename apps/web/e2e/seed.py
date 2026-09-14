"""Synthetic rows only, restricted to the disposable Phase 5 acceptance database."""
import subprocess

container = 'yongtuo-phase5-test-mysql-1'
def sql(statement):
    result = subprocess.run(['docker', 'exec', '-i', container, 'sh', '-c',
        'exec mysql --default-character-set=utf8mb4 -u root -p"$MYSQL_ROOT_PASSWORD" "$MYSQL_DATABASE" -N'],
        input=statement.encode('utf-8'), capture_output=True)
    if result.returncode:
        raise RuntimeError(result.stderr.decode('utf-8', errors='replace'))
    return result.stdout.decode('utf-8').strip()

if sql("SELECT COUNT(*) FROM product WHERE slug='synthetic-1'") != '0':
    print('Synthetic acceptance rows already exist.')
else:
    statements = """
    INSERT INTO product_category(name_zh,name_en,slug,show_on_home) VALUES ('验收测试分类','Acceptance test parts','synthetic-parts',1);
    SET @category=LAST_INSERT_ID();
    INSERT INTO attribute_definition(name_zh,name_en,code,data_type,is_global,default_filterable) VALUES ('测试材质','Test material','material','SELECT',1,1);
    SET @material=LAST_INSERT_ID();
    INSERT INTO attribute_option(attribute_id,value_code,label_zh,label_en) VALUES (@material,'steel','测试钢','Test steel');
    SET @steel=LAST_INSERT_ID();
    INSERT INTO attribute_definition(name_zh,name_en,code,data_type,is_global,default_filterable,unit) VALUES ('测试直径','Test diameter','diameter','NUMBER',1,1,'mm');
    SET @diameter=LAST_INSERT_ID();
    INSERT INTO category_attribute(category_id,attribute_id,is_filterable,show_in_detail) VALUES (@category,@material,1,1),(@category,@diameter,1,1);
    """
    for i in range(1, 26):
        statements += f"""
        INSERT INTO product(category_id,product_code,slug,name_zh,name_en,summary_zh,summary_en,description_zh,description_en,status,english_status)
        VALUES (@category,'TEST-{i}','synthetic-{i}','验收测试产品 {i}','Acceptance product {i}','仅用于隔离环境验收','Synthetic acceptance data only','<p>测试产品说明与应用。</p>','<p>Synthetic description and applications.</p>','PUBLISHED','CONFIRMED');
        SET @product=LAST_INSERT_ID();
        INSERT INTO product_attribute_value(product_id,attribute_id,option_id) VALUES (@product,@material,@steel);
        INSERT INTO product_attribute_value(product_id,attribute_id,numeric_value) VALUES (@product,@diameter,10);
        """
    statements += """
    INSERT INTO product(category_id,product_code,slug,name_zh,status) VALUES (@category,'TEST-DRAFT','synthetic-draft','测试草稿','DRAFT'),(@category,'TEST-OFFLINE','synthetic-offline','测试下架','OFFLINE');
    INSERT INTO product_variant(product_id,variant_code,name_zh,name_en,status) SELECT id,'SYNTHETIC-LONG-VARIANT-CODE-1234567890','测试规格','Test variant','PUBLISHED' FROM product WHERE slug='synthetic-1';
    SET @articleCategory=(SELECT MIN(id) FROM article_category);
    INSERT INTO article(category_id,slug,title_zh,title_en,content_zh,content_en,language_mode,english_status,status,published_at) VALUES
      (@articleCategory,'synthetic-article','验收测试文章','Acceptance article','<p>仅用于测试。</p>','<p>Synthetic test content.</p>','BILINGUAL','CONFIRMED','PUBLISHED',CURRENT_TIMESTAMP),
      (@articleCategory,'synthetic-zh-only','仅中文测试文章',NULL,'<p>中文测试正文。</p>',NULL,'ZH_ONLY','EMPTY','PUBLISHED',CURRENT_TIMESTAMP),
      (@articleCategory,'synthetic-en-only',NULL,'English-only acceptance article',NULL,'<p>English-only synthetic content.</p>','EN_ONLY','CONFIRMED','PUBLISHED',CURRENT_TIMESTAMP),
      (@articleCategory,'synthetic-draft-article','测试草稿',NULL,'<p>测试</p>',NULL,'ZH_ONLY','EMPTY','DRAFT',NULL);
    INSERT INTO case_study(slug,title_zh,title_en,content_zh,content_en,language_mode,english_status,status) VALUES ('synthetic-case','验收测试案例','Acceptance case','<p>仅用于测试。</p>','<p>Synthetic test content.</p>','BILINGUAL','CONFIRMED','PUBLISHED');
    INSERT INTO url_redirect(old_path,new_path,redirect_type) VALUES ('/synthetic-old','/products',301);
    """
    sql("START TRANSACTION;"+statements+"COMMIT;")
    print('Seeded 25 published products and language/publication fixtures in the disposable database only.')

sql("INSERT IGNORE INTO url_redirect(old_path,new_path,redirect_type) VALUES ('/synthetic-old','/products',301)")
