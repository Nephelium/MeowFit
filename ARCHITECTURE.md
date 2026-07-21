# MeowFit 架构说明

## 分层

- `ui/screens` 组合页面与输入；`ui/components` 提供像素组件、主题化无文字底部导航和动态猫。底部导航只属于五个顶级页面，编辑/详情页保持全屏。
- `ui/*ViewModel` 管理页面状态和协程，将批量记录以单次用例交给 Repository。
- `domain/nutrition` 是不依赖 Android UI 的营养换算规则，统一处理任意基准份量与 kJ/kcal。
- `data/Repository` 是记录写入边界，在 Room 事务中创建日记录、批量写入条目并重算汇总。
- `data/backup` 负责备份版本、ZIP 资源映射、输入限额、旧字段默认和事务恢复。
- `data/ai` 负责 OpenAI-compatible 协议和回应解析；聊天图片由 `AppImageStore` 限尺寸解码与持久化。

## 数据兼容

Room 当前版本为 17。公开版旧库通过连续的 `MIGRATION_15_16`、`MIGRATION_16_17` 升级；后一段只对 `calorie_items` 添加可空营养标签快照，并新建 `food_templates`，不重建或删除旧表。

备份当前版本为 v6。恢复采用“校验后合并”：同 ID 条目更新，其他本机记录保留；旧备份未包含的新用户字段优先保留当前设备值。API Key 不进入手动备份，`ai_prefs.xml` 也被排除在 Android 系统备份外。

## 分享图保护边界

首页长图与日历分享图仍由各自独立的 Bitmap 生成器渲染，不依赖屏幕 Compose 树。本次只收紧 FileProvider 到 `cache/camera` 和 `cache/images`；两类分享图仍使用 `cache/images` 与临时读权限。

## 验证

- 纯逻辑：`NutritionCalculatorTest`。
- 数据库：`Migration16To17Test`，覆盖公开版 15→17 与 16→17，验证旧条目、旧对话、营养列和模板表。
- 手测重点：覆盖安装、v5 及更旧备份导入、营养换算、AI 图片对话、首页长图和日历分享。
