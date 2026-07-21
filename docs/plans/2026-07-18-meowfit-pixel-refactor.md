# MeowFit Pixel Refactor Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 修复 MeowFit 的数据与备份缺陷，加入营养标签换算和自定义食物模板，并将全部主要界面升级为带动态像素猫的像素风 UI，同时保护分享与导出行为。

**Architecture:** 先建立纯 Kotlin 计算和版本化数据边界，再以 Room 事务修复一致性，最后用可复用像素组件逐页替换视觉。分享图片继续使用独立 Android Canvas Renderer，通过稳定 Share Model 与 UI 解耦。

**Tech Stack:** Kotlin 1.9、Jetpack Compose Material 3、Room 2.6、Coroutines/Flow、Gson、OkHttp、JUnit4。

---

### Task 1: 固定规范、基线与测试骨架

**Files:**
- Create: `AGENT.md`
- Create: `docs/plans/2026-07-18-meowfit-pixel-refactor-design.md`
- Modify: `app/build.gradle.kts`
- Create: `app/src/test/java/com/example/calorietracker/domain/nutrition/NutritionCalculatorTest.kt`

**Steps:**
1. 建立项目规则和设计文档。
2. 为纯 Kotlin 计算测试补 `kotlinx-coroutines-test`（仅在需要时）。
3. 写营养换算失败测试，覆盖 kcal、kJ、任意基准量、g/ml/份与非法输入。
4. Run: `.\gradlew.bat testDebugUnitTest`；Expected: 新测试因实现缺失而失败。

### Task 2: 营养换算领域模型

**Files:**
- Create: `app/src/main/java/com/example/calorietracker/domain/nutrition/NutritionCalculator.kt`
- Create: `app/src/main/java/com/example/calorietracker/domain/nutrition/NutritionModels.kt`
- Test: `app/src/test/java/com/example/calorietracker/domain/nutrition/NutritionCalculatorTest.kt`

**Steps:**
1. 定义 `EnergyUnit(KCAL, KJ)`、`AmountUnit(GRAM, MILLILITER, SERVING)` 和输入/结果模型。
2. 实现 `kJ / 4.184` 及比例换算，拒绝非有限值、负值和零基准量。
3. 保持内部 Double 精度，只在格式化层舍入。
4. Run: `.\gradlew.bat testDebugUnitTest`；Expected: 营养计算测试通过。

### Task 3: Room schema 17 与模板数据

**Files:**
- Modify: `app/src/main/java/com/example/calorietracker/data/Entities.kt`
- Create: `app/src/main/java/com/example/calorietracker/data/FoodTemplateEntity.kt`
- Modify: `app/src/main/java/com/example/calorietracker/data/Daos.kt`
- Modify: `app/src/main/java/com/example/calorietracker/data/AppDatabase.kt`
- Modify: `app/src/main/java/com/example/calorietracker/CalorieTrackerApp.kt`

**Steps:**
1. 给 `CalorieItemEntity` 增加可空营养来源快照字段。
2. 增加 `FoodTemplateEntity` 与 DAO，名称建立唯一或普通索引。
3. 编写 16→17 增量迁移，只新增列、表与索引。
4. 开启 schema 导出并配置 schema 目录。
5. 添加迁移测试，验证 15→16→17 的资料、记录、周报和聊天仍存在。

### Task 4: 原子记录写入与字段保留

**Files:**
- Modify: `app/src/main/java/com/example/calorietracker/data/Daos.kt`
- Modify: `app/src/main/java/com/example/calorietracker/data/Repository.kt`
- Modify: `app/src/main/java/com/example/calorietracker/ui/MainViewModel.kt`
- Modify: `app/src/main/java/com/example/calorietracker/MainActivity.kt`
- Modify: `app/src/main/java/com/example/calorietracker/ui/screens/ProfileSetupScreen.kt`

**Steps:**
1. 增加按列更新的 DAO 方法和同步聚合查询。
2. 用 `withTransaction` 实现批量添加与一次聚合，避免多协程覆盖。
3. 页面一次提交完整列表，不再逐条启动协程。
4. 编辑资料时从旧实体 `copy`，保留服药和主题字段。
5. 为批量聚合和资料字段保留补测试。

### Task 5: AI 配置、会话与图片修复

**Files:**
- Modify: `app/src/main/java/com/example/calorietracker/data/AiDao.kt`
- Modify: `app/src/main/java/com/example/calorietracker/data/Repository.kt`
- Modify: `app/src/main/java/com/example/calorietracker/ui/AiViewModel.kt`
- Modify: `app/src/main/java/com/example/calorietracker/data/ai/AiService.kt`
- Modify: `app/src/main/java/com/example/calorietracker/ui/screens/ApiSettingsScreen.kt`
- Modify: `app/src/main/java/com/example/calorietracker/ui/screens/AnalysisDetailScreen.kt`
- Modify: `app/src/main/java/com/example/calorietracker/ui/screens/AnalysisScreen.kt`
- Create: `app/src/main/java/com/example/calorietracker/util/AppImageStore.kt`

**Steps:**
1. 全局会话使用 `weekStartDate IS NULL`；周报会话按周隔离；备份另取全量。
2. API 基础设置更新时保留自定义提示词。
3. 周报上下文以周次、记录、项目、资料为 key，并在历史加载完成前禁用发送。
4. 新周报保存成功后再清理旧会话。
5. 图片在 IO 线程采样解码，保存到独立目录；清空会话同步清理文件。

### Task 6: 版本化、兼容且原子的备份

**Files:**
- Modify: `app/src/main/java/com/example/calorietracker/data/backup/BackupManager.kt`
- Modify: `app/src/main/java/com/example/calorietracker/ui/BackupViewModel.kt`
- Test: `app/src/androidTest/java/com/example/calorietracker/data/backup/BackupCompatibilityTest.kt`

**Steps:**
1. 给 `BackupUserProfile` 补 `medicationTimes`，新增模板和 AI 图片清单。
2. 保持旧字段解析容错并限制 ZIP、JSON 和单资源大小。
3. 恢复前生成内部恢复点；解析完成后使用单一 Room 事务写入。
4. 恢复图片时生成新内部路径，不复用旧设备绝对路径。
5. 按明细重算受影响日期合计。
6. 覆盖旧 JSON、旧 ZIP、当前 ZIP、新 ZIP 和损坏 ZIP。

### Task 7: 手动输入与自定义食物模板

**Files:**
- Modify: `app/src/main/java/com/example/calorietracker/ui/screens/AddEntryScreen.kt`
- Modify: `app/src/main/java/com/example/calorietracker/ui/MainViewModel.kt`
- Modify: `app/src/main/java/com/example/calorietracker/data/Repository.kt`
- Create: `app/src/main/java/com/example/calorietracker/ui/components/PixelNutritionCalculator.kt`

**Steps:**
1. 增加快速记录/营养标签换算切换。
2. 实现基准量、实际量、kcal/kJ、三大营养素和 g/ml/份输入。
3. 实时显示结果卡并映射为最终 `EntryItem`。
4. 支持保存、搜索、套用和删除自定义食物模板。
5. 旧记录进入快速模式；带快照记录恢复换算输入。

### Task 8: 像素设计系统和动态猫

**Files:**
- Modify: `app/src/main/java/com/example/calorietracker/ui/theme/Color.kt`
- Modify: `app/src/main/java/com/example/calorietracker/ui/theme/Theme.kt`
- Modify: `app/src/main/java/com/example/calorietracker/ui/theme/Type.kt`
- Create: `app/src/main/java/com/example/calorietracker/ui/components/PixelComponents.kt`
- Create: `app/src/main/java/com/example/calorietracker/ui/components/PixelCat.kt`
- Create: `app/src/main/java/com/example/calorietracker/ui/components/PixelIcons.kt`
- Modify: `app/src/main/java/com/example/calorietracker/MainActivity.kt`

**Steps:**
1. 定义日间糖果像素与夜间星空猫配色、阶梯圆角、块状阴影和间距令牌。
2. 实现像素按钮、卡片、标签、输入容器、空状态、状态条和像素图标。
3. 实现动态猫状态与短帧动画，并尊重系统动画设置。
4. 替换全局主题和底部导航。

### Task 9: 逐页视觉重构

**Files:**
- Modify: `TodayScreen.kt`, `AddEntryScreen.kt`, `OverviewScreen.kt`, `StatisticsScreen.kt`
- Modify: `AnalysisScreen.kt`, `AnalysisDetailScreen.kt`, `SettingsScreen.kt`
- Modify: `ProfileSetupScreen.kt`, `BackupSettingsScreen.kt`, `ApiSettingsScreen.kt`, `SystemPromptSettingsScreen.kt`

**Steps:**
1. 首页加入动态猫状态区、像素爪印进度和贴纸数据卡。
2. 统一添加记录页、资料页和设置页的像素组件与错误提示。
3. 统一统计、分析、备份页面的卡片、标签、空状态和加载状态。
4. 保持所有现有操作入口、可访问标签和深色模式。

### Task 10: 日历曲线与导出保护

**Files:**
- Create: `app/src/main/java/com/example/calorietracker/domain/chart/TrendCurve.kt`
- Modify: `app/src/main/java/com/example/calorietracker/ui/screens/OverviewScreen.kt`
- Refactor: `TodayScreen.kt` share renderer section
- Refactor: `OverviewScreen.kt` calendar renderer section
- Test: `app/src/test/java/com/example/calorietracker/domain/chart/TrendCurveTest.kt`

**Steps:**
1. 为单点、两点、重复值、断档和极端值写曲线测试。
2. 实现不过冲的单调三次插值和统一坐标映射。
3. 页面增加像素网格、渐变面积、选中提示和自然动画。
4. 抽取 Share Model，保持隐私开关和原数据选择不变。
5. 验证首页长图、月历、年历的保存和分享 URI。

### Task 11: 安全、版本和文档同步

**Files:**
- Modify: `app/src/main/AndroidManifest.xml`
- Create: `app/src/main/res/xml/backup_rules.xml`
- Create: `app/src/main/res/xml/data_extraction_rules.xml`
- Modify: `app/src/main/java/com/example/calorietracker/MainActivity.kt`
- Modify: `README.md`
- Create: `ARCHITECTURE.md`

**Steps:**
1. 排除 API Key 的系统备份并收窄 FileProvider 路径。
2. 使用 `BuildConfig.VERSION_NAME`，修正自动分析和自定义图标文案。
3. 同步像素 UI、营养换算、备份格式和隐私说明。
4. 记录模块职责、数据流和迁移策略。

### Task 12: 最终验证

**Steps:**
1. Run: `.\gradlew.bat testDebugUnitTest`; Expected: 全部单元测试通过。
2. Run: `.\gradlew.bat assembleDebug`; Expected: Debug APK 构建成功。
3. 检查 `git diff --check` 和未跟踪生成物。
4. 输出 Android Studio 手测清单：升级旧数据库、恢复旧备份、营养换算、像素 UI、图片、首页长图、月/年日历分享、深色模式。

> 本计划不自动执行 git commit：当前工作区已有用户未提交改动，避免将既有修改错误归入重构提交。

