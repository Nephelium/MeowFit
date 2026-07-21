# MeowFit 项目协作规范

## 适用范围

本文件适用于 `MeowFit/` 整个 Android 工程。开始修改前先阅读本文件；若新增子目录，应在本文件补充其职责和命名约定。

## 目录与职责

- `app/src/main/java/com/example/calorietracker/data/`：Room 实体、DAO、Repository 与本地数据源。
- `app/src/main/java/com/example/calorietracker/data/backup/`：版本化备份、恢复、校验和资源映射。
- `app/src/main/java/com/example/calorietracker/data/ai/`：AI 配置、协议适配与请求实现。
- `app/src/main/java/com/example/calorietracker/domain/`：不依赖 Android UI 的计算规则与用例。
- `app/src/main/java/com/example/calorietracker/ui/components/`：可复用像素风组件、图标和像素猫。
- `app/src/main/java/com/example/calorietracker/ui/screens/`：页面组合，不直接访问 DAO 或自行创建网络服务。
- `app/src/test/`：纯 Kotlin 单元测试；`app/src/androidTest/`：Room 迁移、备份和截图类测试。
- `docs/plans/`：已经确认的设计和实施计划。

## 数据兼容红线

- Room 升级必须提供连续迁移；禁止 `fallbackToDestructiveMigration`。
- 已发布字段不删除、不改含义；新字段优先可空或提供兼容默认值。
- 备份格式必须显式带版本号，并兼容历史 JSON 与 ZIP。
- 恢复前必须完整解析和校验；数据库写入必须置于事务中，失败整体回滚。
- 新备份必须包含记录图片、AI 对话图片、自定义图标及自定义食物模板。
- 修改实体或备份 DTO 时，必须补迁移测试或往返测试。

## UI 与图片规则

- 统一使用像素设计系统；正文和输入内容优先保证中文可读性。
- 像素素材按整数倍、最近邻方式缩放；不对像素猫使用模糊缩放。
- 页面不得直接进行原图解码、压缩或文件写入，图片工作放到 IO 调度器。
- 首页长图和日历导出是受保护模块：修改前固定回归样例，保留隐私开关、尺寸限制、MediaStore 保存与 FileProvider 分享行为。
- 动画应短促并尊重系统动画缩放设置；重要操作不能依赖动画才能完成。

## 工程纪律

- 保留工作区中不属于当前步骤的现有改动，不回退、不覆盖。
- 修改行为前先写纯逻辑测试；UI 变更至少保证编译通过，并列出需要在 Android Studio 手测的页面。
- 错误必须返回可诊断状态，不使用空 `catch` 隐藏数据写入或恢复失败。
- 密钥不进入源码、备份、日志或版本控制；`ai_prefs.xml` 必须排除系统云备份。
- 版本号以 Gradle 配置和 `BuildConfig.VERSION_NAME` 为唯一来源。

## 验证命令

在项目根目录使用 PowerShell：

```powershell
.\gradlew.bat testDebugUnitTest
.\gradlew.bat assembleDebug
```

涉及 Room 迁移或 Android 资源时，再在 Android Studio 运行 `connectedDebugAndroidTest`。不得通过注释错误或添加绕过标记使验证假通过。

