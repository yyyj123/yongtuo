# 勇拓五金实业官网 V1.0 验收清单

## 阶段 Gate

每阶段必须：

- [ ] 目标测试通过
- [ ] 受影响模块完整测试通过
- [ ] 构建通过
- [ ] 无未说明失败
- [ ] 数据库迁移可从空库执行
- [ ] 文档与接口变化同步
- [ ] Git 提交清晰
- [ ] 阶段报告完成

## 产品系统

- [ ] 产品新增、编辑、复制、上下架、软删除
- [ ] 产品编号唯一三层保护
- [ ] 分类树可后台新增，不改前端代码
- [ ] NORMAL / SHOWCASE 分类模式
- [ ] 分类有子分类或产品时不可直接删除
- [ ] 动态参数按分类加载
- [ ] SELECT / NUMBER / TEXT 参数能保存和筛选
- [ ] 产品多规格维护
- [ ] 规格差异大时仍可建独立产品
- [ ] 产品附件公开与允许下载分离

## 搜索与筛选

- [ ] 中文名
- [ ] 英文名
- [ ] 产品编号
- [ ] 型号
- [ ] 材质
- [ ] 标准
- [ ] 分类
- [ ] 多条件组合
- [ ] 清除单项
- [ ] 清除全部
- [ ] URL 刷新保留状态
- [ ] pageSize 最大值后端限制
- [ ] 无结果空状态

## Excel / 批量图片

- [ ] 下载模板
- [ ] Excel 预检
- [ ] 未确认前数据库无正式写入
- [ ] 重复编号错误明确
- [ ] 分类不存在错误明确
- [ ] 确认导入与预览一致
- [ ] 按 `产品编号-序号` 批量匹配图片
- [ ] 未匹配图片显示原因

## 双语

- [ ] 中文 `/`
- [ ] English `/en/...`
- [ ] 切换保持当前内容
- [ ] AI_DRAFT 不公开
- [ ] CONFIRMED 才公开英文
- [ ] 仅中文文章不出现在英文列表
- [ ] 仅英文文章不出现在中文列表
- [ ] 正式英文公司全称不被编造

## 首页

- [ ] 单 Hero，无自动轮播
- [ ] 三大业务入口
- [ ] 分类后台驱动
- [ ] 推荐产品 6–8
- [ ] 精选案例 <=4
- [ ] 精选资质 <=4
- [ ] 精选文章 =3
- [ ] 联系方式后台修改全站同步
- [ ] 首页固定结构不能被后台自由拖坏

## 搜索 UI

- [ ] Desktop 右侧抽屉
- [ ] Close
- [ ] Overlay close
- [ ] ESC close
- [ ] Suggestions
- [ ] Enter to search
- [ ] Mobile 全屏搜索

## SEO

- [ ] SSR HTML 含核心正文
- [ ] 独立 Title
- [ ] Meta Description
- [ ] canonical
- [ ] hreflang
- [ ] sitemap.xml
- [ ] robots.txt
- [ ] BreadcrumbList
- [ ] Organization
- [ ] Product
- [ ] Article
- [ ] Open Graph
- [ ] 图片 alt
- [ ] 筛选组合 noindex, follow
- [ ] 草稿/下架不进 Sitemap
- [ ] 真 HTTP 404
- [ ] Slug 变更真 HTTP 301

## 安全

- [ ] BCrypt
- [ ] Access + Refresh Token
- [ ] 改密后旧 Refresh Token 失效
- [ ] 登录失败限速
- [ ] Admin API 未登录拒绝
- [ ] MySQL 不开放全公网
- [ ] 文件扩展名/MIME/文件头/大小校验
- [ ] 可执行文件拒绝
- [ ] 富文本 XSS 清洗
- [ ] SQL 参数化
- [ ] 动态排序白名单
- [ ] 生产环境不返回堆栈
- [ ] CORS 白名单
- [ ] HTTPS

## 响应式与性能

- [ ] 375
- [ ] 430
- [ ] 768
- [ ] 1024
- [ ] 1440
- [ ] 1920
- [ ] Chrome
- [ ] Edge
- [ ] Safari
- [ ] iOS Safari
- [ ] Android Chrome
- [ ] 图片 Lazy Load
- [ ] 产品列表不加载原始超大图
- [ ] LCP < 2.5s 设计目标
- [ ] CLS < 0.1
- [ ] INP < 200ms

## 运维

- [ ] local
- [ ] staging
- [ ] production
- [ ] Flyway 从空库完整执行
- [ ] MySQL 自动备份
- [ ] 至少一次恢复演练
- [ ] Docker 镜像回滚演练
- [ ] Nginx access/error 日志
- [ ] Spring Boot 日志轮转
- [ ] 服务健康检查
- [ ] HTTP 5xx 监控
- [ ] SSL 到期监控
