# 勇拓五金实业官网 V1.0 API 契约

统一前缀：`/api/v1`

统一响应：

```json
{
  "code": 0,
  "message": "success",
  "data": {}
}
```

分页：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "items": [],
    "page": 1,
    "pageSize": 24,
    "total": 0,
    "totalPages": 0
  }
}
```

## 1. Public API

### Home

`GET /public/home`

聚合 Hero、业务入口、首页分类、推荐产品、CNC、能力、案例、证书、文章、About 和联系方式。

### Categories

- `GET /public/categories`
- `GET /public/categories/{slug}`

### Products

`GET /public/products`

Query：

- keyword
- category
- page
- pageSize
- sort
- `attr.<code>=<value>`

后端 pageSize 默认 24，最大 100。

`GET /public/products/{slug}`

返回：

- base
- images
- attributes
- variants
- publicAttachments
- relatedCases
- relatedProducts
- SEO metadata

### Search

- `GET /public/search/suggestions?q=...`
- `GET /public/search?keyword=...&type=...&page=...`

联想最多返回 10 条。

### Content

- `GET /public/cnc-machining`
- `GET /public/capabilities`
- `GET /public/cases`
- `GET /public/cases/{slug}`
- `GET /public/articles`
- `GET /public/articles/{slug}`
- `GET /public/certificates`
- `GET /public/catalogs`
- `GET /public/contact`
- `GET /public/site`

### Language

公开接口读取 `Accept-Language: zh-CN|en`。

### URL Redirects

`GET /public/redirects?path=<old-path>`

命中时返回 `oldPath`、`newPath` 和 `redirectType: 301`，供公开站点发出真实 HTTP 301；未命中返回 404。

Public DTO 返回当前语言的主显示字段；后台 DTO 返回双语字段。

## 2. Admin Auth

- `POST /admin/auth/login`
- `POST /admin/auth/refresh`
- `POST /admin/auth/logout`
- `GET /admin/auth/me`
- `PUT /admin/auth/password`

登录：

```json
{
  "username": "admin",
  "password": "user-input"
}
```

认证说明：login/refresh 成功的 `data` 为 `{accessToken, refreshToken, tokenType: "Bearer", expiresInSeconds: 900}`。
refresh/logout 请求体为 `{refreshToken}`，logout 还需要 access Bearer；me 返回 `{id, username}`。
password 请求体为 `{currentPassword, newPassword}`，新密码至少 12 字符且 UTF-8 编码不超过 72 字节。
改密后旧 access/refresh 均立即失效。logout 仅撤销指定 refresh session，access 最长保留至 15 分钟到期。
认证错误：10001 登录失败，10002 refresh 无效或过期，10003 未认证，10004 旧密码不正确。
登录限流：账号或连接 IP 在 15 分钟窗口达到 5 次失败后返回 HTTP 429 / 10005；成功登录重置相关计数。

## 3. Admin Products

- `GET /admin/products`
- `POST /admin/products`
- `GET /admin/products/{id}`
- `PUT /admin/products/{id}`
- `DELETE /admin/products/{id}`
- `POST /admin/products/{id}/duplicate`
- `PUT /admin/products/{id}/status`
- `PUT /admin/products/{id}/featured`
- `PUT /admin/products/{id}/sort`

### Import

- `POST /admin/products/import/preview`
- `POST /admin/products/import/confirm`
- `POST /admin/products/images/batch`

`preview` 成功后返回短期 `importToken` 和逐行结果；confirm 只能引用服务端保存的预检快照，不接受前端重新提交任意未经预检数据。

## 4. Categories & Attributes

- `GET /admin/categories/tree`
- `POST /admin/categories`
- `PUT /admin/categories/{id}`
- `DELETE /admin/categories/{id}`
- `PUT /admin/categories/sort`
- `GET /admin/categories/{id}/attributes`
- `PUT /admin/categories/{id}/attributes`

Attributes：

- `GET /admin/attributes`
- `POST /admin/attributes`
- `PUT /admin/attributes/{id}`
- `DELETE /admin/attributes/{id}`

Attribute definitions carry `dataType` (`TEXT`, `NUMBER`, `SELECT`, or `MULTI_SELECT`)
and an `options` array. Options are accepted only for the two select types. A category
binding replacement is transactional: the request body is the complete binding list, and
each item contains `attributeId`, `isFilterable`, `isRequired`, `showInDetail`, and
`sortOrder`. Global active attributes are included for every active category using their
defaults; an explicit category binding overrides those defaults. `INACTIVE` preserves the
definition and its values. `DELETE` physically removes an attribute only when it has no
category binding, product value, variant value, or option reference; otherwise it returns
`ATTRIBUTE_IN_USE` and the caller must use `PUT` to set `status=INACTIVE`.

Product writes use the effective ACTIVE global/category binding set. `isRequired` is
independent from `isFilterable`: when a required attribute has a product-level value it
is inherited by all variants; when it has no product-level value and variants exist, every
variant must provide it; with no variants the product-level value is mandatory. `TEXT`,
`NUMBER`, `SELECT`, and `MULTI_SELECT` values have strict shapes. A multi-select stores one
row per ACTIVE option and detail DTOs return the grouped, stable `optionIds` list.
The internal persistence key is not part of the DTO: TEXT/NUMBER/SELECT use
`value_key=0`, while MULTI_SELECT uses the real `option_id` as `value_key`; future Task 9/10
imports must use the same normalization and must not reintroduce CSV values.

分类存在子分类或有效产品时，DELETE 必须返回业务错误。

## 5. Content Admin

Articles：

- `GET/POST /admin/articles`
- `GET/PUT/DELETE /admin/articles/{id}`

文章分类：`GET/POST /admin/article-categories`、`PUT/DELETE /admin/article-categories/{id}`、
`GET /public/article-categories`。分类被文章（包括软删除记录）引用时禁止物理删除，可设为 INACTIVE。
文章列表默认 page=1/pageSize=24，最大 100；后台返回双语字段，公开返回当前语言 title/summary/content/SEO。
写入包含 categoryId、slug、titleZh/En、coverImage、summaryZh/En、contentZh/En、languageMode、
englishStatus、status、isFeatured、sortOrder、seoTitleZh/En、seoDescriptionZh/En。
DELETE 为软删除；slug 保留唯一。仅可见语言生成已发布 slug 的 301。

Cases：

- `GET/POST /admin/cases`
- `GET/PUT/DELETE /admin/cases/{id}`

案例写入为平铺的双语字段、languageMode/englishStatus/status、SEO、isFeatured/sortOrder，另含
applicationSceneZh/En、requirementZh/En、solutionZh/En、productIds、categoryIds、imageUrls。
后台响应 `{base, productIds, categoryIds, imageUrls}`；公开响应为当前语言字段与可见的 products/categories 链接。
产品公开详情新增 `relatedCases: [{id, slug, title, coverImage, summary}]`，按当前语言过滤。

Certificates：

- `GET/POST /admin/certificates`
- `GET/PUT/DELETE /admin/certificates/{id}`

Catalogs：

- `GET/POST /admin/catalogs`
- `GET/PUT/DELETE /admin/catalogs/{id}`
- `PUT /admin/catalogs/{id}/primary`

证书/目录使用 DRAFT/PUBLISHED/OFFLINE 和 englishStatus；英文公开必须 CONFIRMED。
证书公开响应 `downloadUrl` 仅在 allowDownload=true 时提供，预览 coverImage 独立。
目录写入不直接设置 isPrimary，使用 primary 接口切换；同一语言最多一个有效主版本，BILINGUAL 占两种语言位置，旧版本保留。

Home：

- `GET /admin/home`
- `PUT /admin/home/hero`
- `PUT /admin/home/business`
- `PUT /admin/home/cnc`
- `PUT /admin/home/about`
- `PUT /admin/home/featured-products`
- `PUT /admin/home/featured-cases`
- `PUT /admin/home/featured-articles`
- `PUT /admin/home/featured-certificates`

Site / Contact：

- `GET/PUT /admin/site`
- `GET/PUT /admin/contact`

Site PUT 为 `{configKey: {valueZh,valueEn,englishStatus}}`，仅允许品牌、公司信息、默认 SEO、备案、版权、Logo/Favicon、服务介绍和隐私政策等固定键。
Contact PUT 为完整数组，字段 type、labelZh/En、value、valueEn（可选英文地址）、linkUrl、sortOrderZh/En、enabled。
Home 固定区块写入 titleZh/En、subtitleZh/En、contentZh/En、imageUrl、englishStatus、enabled；补充 `/admin/home/capabilities`。
`/admin/home/business` 接受恰好三个固定 code（HARDWARE/MACHINED/CNC）的业务入口；字段 titleZh/En、contentZh/En、imageUrl、linkPath、englishStatus、enabled。
四种 featured 接口接受资源 ID 数组，文章必须3项，其他上限依次8/4/4。公开聚合只取当下可见源内容。

## 6. Media

操作日志：`GET /admin/operation-logs?page=1&pageSize=24`，最大100条，仅后台认证访问。
记录成功写入的路由模板、方法、资源类型/ID、管理员ID、IP和时间，不保存请求正文或令牌。

- `POST /admin/media/images`
- `POST /admin/media/files`
- `GET /admin/media`
- `DELETE /admin/media/{id}`

校验：

- 扩展名
- MIME
- 文件头
- 大小
- 图像解析
- 可执行文件黑名单
- 对象存储路径隔离

## 7. AI English Draft

业务接口建议采用提供者抽象，而不把供应商写死在 Controller：

`POST /admin/translation/draft`

请求：

```json
{
  "resourceType": "PRODUCT",
  "resourceId": 1001,
  "fields": ["name", "summary", "description"]
}
```

只有配置了真实 `TranslationProvider` 时才允许执行；否则返回明确的 `FEATURE_NOT_CONFIGURED`。生成结果状态必须是 `AI_DRAFT`，不能自动改为 `CONFIRMED`。

实际 AI 提供商尚未由业务方选择，因此实施时必须通过接口适配层接入，不得把某个供应商 SDK 扩散到业务模块。

draft 请求需包含 `replaceConfirmed`（默认工作流传false；替换已确认英文必须显式true）。
支持 PRODUCT/ARTICLE/CASE/CERTIFICATE/CATALOG/HOME_SECTION/SITE_CONFIG 的固定文案字段，禁止编号/参数/分类。
独立 `POST /admin/translation/confirm` 请求 `{resourceType,resourceId,expectedFields}`：expectedFields必须等于当前全部非空可翻译英文字段，防止确认旧草稿；确认前不会自动发布英文。

## 8. Error Codes

建议：

- 100xx Auth
- 200xx Product
- 210xx Category
- 220xx Attribute
- 300xx Article
- 310xx Case
- 400xx Media
- 500xx System

典型：

- 10001 LOGIN_FAILED
- 10002 TOKEN_EXPIRED
- 20001 PRODUCT_NOT_FOUND
- 20002 PRODUCT_CODE_DUPLICATE
- 20003 PRODUCT_SLUG_DUPLICATE
- 20004 PRODUCT_CATEGORY_INVALID
- 20005 PRODUCT_CONSTRAINT_VIOLATION
- 21001 CATEGORY_NOT_FOUND
- 21002 CATEGORY_NOT_EMPTY
- 22001 ATTRIBUTE_NOT_FOUND
- 22002 ATTRIBUTE_CODE_DUPLICATE
- 22003 ATTRIBUTE_OPTION_VALUE_DUPLICATE
- 22004 CATEGORY_ATTRIBUTE_DUPLICATE
- 22005 ATTRIBUTE_OPTIONS_NOT_ALLOWED
- 22006 ATTRIBUTE_REFERENCE_INVALID
- 22007 ATTRIBUTE_IN_USE
- 22008 ATTRIBUTE_TYPE_CHANGE_NOT_ALLOWED
- 22009 ATTRIBUTE_OPTION_INVALID
- 40001 UNSUPPORTED_FILE_TYPE
- 40002 FILE_TOO_LARGE
- 50001 FEATURE_NOT_CONFIGURED

## Phase 4 后台媒体维护补充

- `POST /admin/media`：multipart `file`，JPG/PNG/WebP/PDF ≤20 MB，扩展名、MIME、文件头验证，返回 media 元数据。未配置上传供应商返回503/50001。
- `GET/PUT /admin/products/{id}/images`：完整图片关联列表；PUT字段 mediaId/altZh/altEn/sortOrder/isCover，最多100张且最多一个主图；同步产品主图。
- `GET/PUT /admin/products/{id}/attachments`：完整附件关联列表；PUT字段 mediaId/displayNameZh/displayNameEn/isPublic/allowDownload/sortOrder，两个权限独立。
- 只引用有效media元数据；全量替换在事务中完成，上传回滚补偿删除对象。移除关联保留原媒体，不修改Schema或存储选型。

`GET /admin/products/import/template` 返回与预检器一致的 .xlsx 空模板；需 Bearer 鉴权。
