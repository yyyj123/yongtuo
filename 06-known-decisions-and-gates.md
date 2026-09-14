# 已冻结决定与实施前外部信息 Gate

## 已冻结，无需再次讨论

- 中文主站，English 辅助。
- 官网前台 Nuxt 3。
- 后台 Vue 3 + Vite + Element Plus。
- Spring Boot 单体 API。
- MySQL 8 + Flyway。
- 对象存储 + CDN。
- 中国大陆服务器。
- 单超级管理员。
- 产品不显示价格。
- 客户通过站外联系方式沟通。
- Desktop 搜索从右侧弹出。
- 首页推荐产品 6–8。
- 分类和规格采用混合模式。
- 搜索使用关键词 + 通用参数 + 分类自定义参数。
- 新闻与案例为轻量模块。
- 证书默认展示与下载权限分开。
- Excel 预检后导入。
- V1 批量图片，V2 再做 ZIP 一键导入。
- Logo 暂不设计，先用 YONGTUO / 勇拓五金实业文字品牌。
- 公司成立于 2011 年的资料可以使用。

## 不阻塞代码框架、但上线前必须提供

- 公司正式联系电话
- 微信
- Email
- WhatsApp
- 地址
- ICP 备案主体信息
- 域名
- 真实产品图片
- 产品真实规格/标准/材质
- 工厂/设备照片
- 可公开证书
- 产品目录 PDF
- 隐私政策所需统计/第三方脚本清单

## 执行到对应任务前必须决定

### 对象存储 / CDN 云厂商

架构只依赖 `ObjectStorageService` 接口。实际厂商 adapter 在 staging 部署前确定。

### AI 英文初稿提供商

目前没有确定 OpenAI、DeepSeek 或其他服务。

实施规则：

1. 先实现 `TranslationProvider` 接口、状态机和测试替身。
2. 未配置真实 Provider 时 `/admin/translation/draft` 返回 `FEATURE_NOT_CONFIGURED`。
3. 真实 AI Adapter 必须在业务方选择提供商后单独接入。
4. 不允许为赶进度在代码中写死某个供应商。
