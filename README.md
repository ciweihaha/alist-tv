# AlistTV

Android TV 专用 Alist / OpenList 客户端：把服务器上挂载的全部网盘（百度、阿里、Google Drive 等）聚在一个电视应用里浏览和播放。

## 功能

- **登录**：首次启动用遥控器友好的向导页填写服务器地址、用户名、密码，调 `POST /api/auth/login` 拿 token，存到 SharedPreferences，之后启动自动登录。401 时自动用保存的账号重登一次再重试。
- **浏览**：调 `POST /api/fs/list`（每页 100 条，自动翻页）展示当前目录，文件夹在前、按名字排序；遥控器方向键移动焦点，确认键进文件夹或打开文件，返回键回上级目录；顶部标题栏显示当前路径，根目录就是各网盘挂载点。
- **播放**：视频（mp4 / mkv / avi / mov / webm / flv / ts / m2ts / rmvb 等）先调 `POST /api/fs/get` 拿 `raw_url`，用 Media3 ExoPlayer 全屏播放；确认键播放/暂停，左/右快退/快进 10 秒，返回键退出。
- **看图**：图片（jpg / png / webp / gif 等）全屏 ImageView 查看，左/右在当前目录的图片之间翻页。
- **设置**：浏览页第一个“设置”卡片（或遥控器菜单键）进入，可改服务器地址和账号（保存后重新登录），或退出登录回到登录页。

## 默认服务器

`https://openlist.ciweihaha.win`（OpenList，Cloudflare 隧道 → oracle-1 的 5244 端口）。首次登录时可以直接改成别的地址。

## 技术栈

Kotlin + androidx.leanback（VerticalGridSupportFragment + GuidedStepSupportFragment + ImageCardView）
+ Media3 ExoPlayer + OkHttp + Gson。包名 `com.ciweihaha.alisttv`，minSdk 21，targetSdk 34。

## 构建

本地没有 Android SDK 时，用 GitHub Actions 编译（仓库里已配好 `.github/workflows/android.yml`）：

- push 到 `main`、提 PR 或手动触发，Job 用 JDK 17 + Gradle 8.7 跑 `gradle assembleDebug`，
- 编好的 APK 作为 artifact `AlistTV-debug` 上传（`app/build/outputs/apk/debug/app-debug.apk`）。

本地构建（需 JDK 17 + Android SDK，compileSdk 34）：

```bash
gradle assembleDebug
```

本仓库不含 Gradle Wrapper（本地没有 Android SDK，无法生成校验），CI 通过 `gradle/actions/setup-gradle@v3` 安装 Gradle 8.7；本地开发可装 Gradle 8.7 后跑 `gradle wrapper` 生成。

## 目录结构

```
settings.gradle.kts / build.gradle.kts / gradle.properties
.github/workflows/android.yml        CI：JDK 17 + Gradle 8.5 编译 debug APK
app/build.gradle.kts                  依赖：leanback 1.2.0 / media3 1.3.1 / okhttp 4.12.0 / gson 2.11.0
app/src/main/AndroidManifest.xml      LEANBACK_LAUNCHER + banner，minSdk 21 / targetSdk 34
app/src/main/java/com/ciweihaha/alisttv/
  api/    AlistModels.kt / AlistClient.kt / AlistRepository.kt   登录、列目录、取直链、401 重登
  util/   Prefs.kt / FileUtils.kt
  ui/     BrowseActivity.kt / BrowseFragment.kt / BrowseItemPresenter.kt
          LoginActivity.kt / SettingsActivity.kt
          PlayerActivity.kt / ImageViewerActivity.kt
app/src/main/res/                     布局、字符串、主题、图标、banner
```

## 说明

- 服务器游客访问是关闭的，必须用账号密码登录；App 不存网盘本身的凭证，只存 OpenList 账号。
- 个别网盘驱动不返回 `raw_url` 时，App 回退到本站直链 `{server}/d{path}?sign={sign}`。
- 图片直链与本站同域时才带 `Authorization` 请求头，不会把 token 发给第三方。
