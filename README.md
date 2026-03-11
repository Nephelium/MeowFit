# 减肥日历 Android App

这是一个基于 Web 版 "减肥日历" 复刻的原生 Android 应用，旨在帮助用户追踪每日热量摄入与消耗，通过可视化的方式管理体重目标。

## 技术栈

- **语言**: Kotlin
- **UI**: Jetpack Compose (Material Design 3)
- **架构**: MVVM (Model-View-ViewModel)
- **数据库**: Room (SQLite)
- **网络**: Retrofit (用于 AI 功能)
- **异步处理**: Coroutines & Flow
- **导航**: Jetpack Navigation Compose

## 项目结构

- `app/src/main/java/com/example/calorietracker/`
  - `data/`: 数据层 (Room Database, DAOs, Entities, Repository)
  - `ui/`: UI 层 (Screens, ViewModels, Theme, Components)
  - `util/`: 工具类 (日期处理, 卡路里计算)
  - `MainActivity.kt`: 应用入口
  - `CalorieTrackerApp.kt`: Application 类 (依赖注入容器)

## 功能实现

1.  **个人资料与目标设定**
    - 设置性别、体重、身高、年龄、活动水平等。
    - 自动计算每日基础代谢 (BMR) 和每日总能量消耗 (TDEE)。
    - 设定减重/增重/保持体重的目标。

2.  **今日概览 (仪表盘)**
    - 实时显示今日摄入、消耗、剩余热量及进度条。
    - **今日记录列表**:
        - 清晰展示每条饮食或运动记录。
        - 支持点击记录进行修改（名称、热量、时间、备注）。
        - 支持删除记录（红色删除按钮）。
    - 快速记录体重。
    - 运动计时器功能。

3.  **智能记录 (AI 助手)**
    - 集成 AI 对话功能，通过自然语言输入（如“吃了一个苹果”）自动分析热量并生成记录。
    - 支持自定义 AI 服务商配置 (OpenAI, DeepSeek, Moonshot, Aliyun 等)。

4.  **统计分析**
    - **多维度统计**: 支持按周、月、年查看运动与热量数据。
    - **图表可视化**: 包含体重变化趋势图等。

5.  **日历视图**
    - **热力图**: 直观展示每日热量盈余/亏缺情况（类似 GitHub 提交热力图）。
    - 支持按月查看详细记录。

6.  **数据管理**
    - **本地持久化**: 所有数据存储在本地 SQLite 数据库中，保护隐私。
    - **备份与恢复**: 支持导出数据为 JSON 文件及从文件恢复，防止数据丢失。

## UI 特性

- **Material Design 3**: 全面采用最新的 Material Design 设计规范。
- **自定义底部导航栏**: 选中项高亮显示，图标与文字结合的胶囊样式，提升交互体验。
- **深色模式**: 完美适配系统深色/浅色主题。

## 如何运行

1.  使用 **Android Studio** (推荐 Giraffe 或更新版本) 打开 `android` 目录。
2.  等待 Gradle 同步完成。
3.  连接 Android 设备或启动模拟器 (最低支持 Android 8.0 / API 26)。
4.  点击 "Run" (运行) 按钮。
5.  (可选) 在设置中配置 AI API Key 以启用智能记录功能。

## 注意事项

- 本项目为一个完整的 Android 客户端实现，部分高级功能（如 AI 分析）依赖网络连接。
- 首次运行时需要进行个人资料设置以初始化数据。
