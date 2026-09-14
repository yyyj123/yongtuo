# 勇拓五金实业官网 V1.0 数据库设计

## 1. 原则

- MySQL 8。
- 所有 Schema 变更使用 Flyway。
- 业务内容软删除时保留 `deleted_at`。
- 产品编号唯一。
- Slug 在对应资源范围内唯一。
- 图片/PDF/附件只存对象存储 URL 和元数据。
- 动态属性不能写死进 `product` 主表。
- 已发布 URL 修改需生成 301 记录。

## 2. 核心表

### admin_user

| 字段 | 类型建议 | 约束 |
|---|---|---|
| id | BIGINT | PK |
| username | VARCHAR(64) | UNIQUE, NOT NULL |
| password_hash | VARCHAR(255) | NOT NULL |
| token_version | INT | NOT NULL DEFAULT 0 |
| last_login_at | DATETIME | NULL |
| created_at | DATETIME | NOT NULL |
| updated_at | DATETIME | NOT NULL |

`token_version` 用于改密后使旧 Refresh Token 失效。

### admin_login_log

记录登录成功/失败、IP、User-Agent、时间、失败原因。

### admin_operation_log

记录新增/编辑/删除/导入/修改全局配置等关键后台操作。

### product_category

关键字段：

- id
- parent_id
- name_zh / name_en
- slug
- cover_image
- description_zh / description_en
- category_mode: NORMAL / SHOWCASE
- sort_order
- status
- show_on_home
- seo_title_zh / seo_title_en
- seo_description_zh / seo_description_en
- created_at / updated_at / deleted_at

索引：

- `(parent_id, sort_order)`
- `(status, deleted_at)`
- `UNIQUE(slug, deleted_at)` 的实现需考虑 MySQL NULL 唯一语义；建议使用 active 唯一策略或业务层约束。

### product

关键字段：

- id
- category_id
- product_code UNIQUE
- name_zh / name_en
- slug
- summary_zh / summary_en
- description_zh / description_en
- english_status: EMPTY / AI_DRAFT / CONFIRMED
- cover_image
- is_featured
- sort_order
- status: DRAFT / PUBLISHED / OFFLINE
- seo_title_zh / seo_title_en
- seo_description_zh / seo_description_en
- created_at / updated_at / deleted_at

核心索引：

- UNIQUE(product_code)
- `(category_id, status, deleted_at, sort_order)`
- `(is_featured, status, sort_order)`
- `(updated_at)`
- Slug 唯一业务约束

### product_image

- id
- product_id
- media_id
- image_url
- alt_zh / alt_en
- sort_order
- is_cover
- created_at

索引 `(product_id, sort_order)`。

### attribute_definition

- id
- name_zh / name_en
- code UNIQUE
- data_type: TEXT / NUMBER / SELECT / MULTI_SELECT
- unit
- is_global
- default_filterable
- default_required
- sort_order
- status

### attribute_option

用于 SELECT / MULTI_SELECT：

- id
- attribute_id
- value_code
- label_zh / label_en
- sort_order
- status

唯一约束 `(attribute_id, value_code)`。

### category_attribute

- id
- category_id
- attribute_id
- is_filterable
- is_required
- show_in_detail
- sort_order

唯一 `(category_id, attribute_id)`。

### product_attribute_value

- id
- product_id
- attribute_id
- value_zh / value_en
- numeric_value
- option_id NULL
- sort_order

索引：

- `(product_id, attribute_id)`
- `(attribute_id, numeric_value)`
- `(attribute_id, option_id)`

### product_variant

- id
- product_id
- variant_code
- name_zh / name_en
- sort_order
- status

唯一 `(product_id, variant_code)`。

### product_variant_value

- id
- variant_id
- attribute_id
- value_zh / value_en
- numeric_value
- option_id NULL

### product_attachment

- id
- product_id
- media_id
- file_name
- display_name_zh / display_name_en
- file_url
- file_type
- is_public
- allow_download
- sort_order
- created_at

### article_category

- id
- name_zh / name_en
- slug
- sort_order
- status

初始化：公司动态、产品知识、CNC 加工知识、行业应用。

### article

- id
- category_id
- title_zh / title_en
- slug
- cover_image
- summary_zh / summary_en
- content_zh / content_en
- language_mode: ZH_ONLY / EN_ONLY / BILINGUAL
- english_status
- is_featured
- status
- published_at
- SEO 字段
- created_at / updated_at / deleted_at

### case_study

与 article 类似，另含：

- application_scene_zh / en
- requirement_zh / en
- solution_zh / en

### case_product

唯一 `(case_id, product_id)`。

### case_category

唯一 `(case_id, category_id)`。

### certificate

- id
- type
- name_zh / name_en
- description_zh / description_en
- cover_image
- media_id
- file_url
- is_public
- allow_download
- is_featured
- sort_order
- status
- created_at / updated_at

### catalog

- id
- title_zh / title_en
- language_mode
- version
- cover_image
- media_id
- file_url
- is_primary
- status
- published_at
- created_at / updated_at

约束：同一语言最多一个 active primary catalog，由业务事务保证。

### site_config

建议存结构化 Key，而不是大而不可控的 JSON：

- id
- config_key UNIQUE
- value_zh
- value_en
- value_type
- updated_at

### home_section

- id
- section_code UNIQUE
- title_zh / title_en
- subtitle_zh / subtitle_en
- content_zh / content_en
- image_url
- enabled
- sort_order
- updated_at

首页固定区块的 `section_code` 不允许后台创建任意页面结构。

### home_featured_product / case / article / certificate

分别保存资源 ID + sort_order。

业务限制：

- product <= 8
- case <= 4
- certificate <= 4
- article = 3

### contact_info

- id
- type: PHONE / WECHAT / EMAIL / WHATSAPP / LINKEDIN / ADDRESS
- label_zh / label_en
- value
- link_url
- sort_order_zh
- sort_order_en
- enabled

### media_file

- id
- original_name
- storage_key
- public_url
- file_type
- mime_type
- file_size
- width / height NULL
- checksum
- status
- created_at
- deleted_at

### url_redirect

- id
- old_path UNIQUE
- new_path
- redirect_type DEFAULT 301
- created_at

## 3. Flyway 顺序

```text
V1__create_admin_and_logs.sql
V2__create_product_category.sql
V3__create_product_and_media.sql
V4__create_attribute_model.sql
V5__create_product_variants_and_attachments.sql
V6__create_articles_and_cases.sql
V7__create_certificates_and_catalogs.sql
V8__create_site_home_contact.sql
V9__create_redirects.sql
V10__seed_initial_categories.sql
V11__seed_article_categories.sql
```

## 4. 初始化产品分类

初始化数据只作为 seed；前端不能硬编码。

一级：

- 五金产品
- 机械加工件

五金子类：

- 石膏板膨胀螺丝
- 插销
- 螺钉
- 牙条
- 垫圈
- 螺母
- 铆钉
- 双头螺柱
- 自攻螺丝
- 螺栓
- 混凝土锚钉
- 锚

机械加工子类：

- 顶针
- 法兰盘
- 螺丝扣

CNC 是独立服务内容模块，不强制伪装成产品分类。
