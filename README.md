# 阅读Record（LegadoR）

阅读Record 是基于 [Legado](https://github.com/gedoor/legado) 与 [阅读Sigma](https://github.com/Luoyacheng/legado-E) 二次开发的 Android 阅读应用。

项目完整保留上游的书源、发现等功能和**ui设计风格**，并在此基础上新增了**阅读记录数据可视化**，包括阅读时间详情、统计、评分等。

保留 Legado / 阅读Sigma 功能，支持共存安装。

> 本应用不提供任何内容源。小说、漫画、RSS 等内容需要用户自行导入书源、订阅源或本地文件。


## 核心功能

### 阅读总记录

主界面新增“记录”标签页，可在“我的 - 其它设置”中开关显示。记录页内部包含两个页面：

- **阅读记录：** 年度热力图、周期统计、周阅读柱状图、24 小时阅读分布、阅读最长的书、最近阅读等。
- **评分总览：** 按 `5.0` 到 `0.0` 星筛选书架书籍。

### 阅读详情

- 书籍详情页新增“阅读数据”模块，展示累计阅读时长、阅读天数、开始阅读时间等信息。
- 新增月份阅读日历，可查看单本书每天的阅读分布。

### 统计口径更新

- 新统计修复了原app按书名识别书籍的问题，改为按URL识别，避免“同名不同书”互相污染。
- 提供旧记录合并能力，在”我的-阅读记录”界面可以手动把历史同名记录归并到指定书籍。

## 免责声明

阅读依赖系统 WebView 和用户自定义第三方规则访问网页内容。项目本身不提供内容，也不对第三方书源、订阅源或网页返回内容的合法性、准确性和可用性承担责任。用户应自行判断导入规则和访问内容的风险。

## 致谢

感谢以下项目及其贡献者：

- [Legado](https://github.com/gedoor/legado)
- [阅读Sigma](https://github.com/Luoyacheng/legado-E)
- jsoup、JsoupXpath、json-path、rhino-android、okhttp、Glide、NanoHTTPD、BGAQRCode、ColorPicker、commons-text、Markwon、HanLP、epublib-core、LyricViewX、Rosemoe Editor 等开源库

本项目基于 GPL-3.0 许可发布，详见 [LICENSE](LICENSE)。
