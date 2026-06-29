# 图像画质分析工具 README

## 目标

本项目实现一个 Android 端本地图像画质分析 Demo。用户选择手机相册或文件中的图片后，App 会读取图片基础信息，计算传统图像质量指标，并输出 0-100 综合评分、各维度子分和中文解释。

本项目对应“AI 图像识别与画质分析工具”方向中的画质分析部分。它的核心任务是说明一张图片在清晰度、曝光、噪点、对比度、色彩和细节信息方面是否存在明显问题。

## 非目标

- 不识别图片中的具体物体、人物或场景类别。
- 不使用深度学习模型，不训练神经网络。
- 不把综合分解释为审美分、摄影作品分或内容价值分。
- 不保证 HEIC/HEIF、AVIF、GIF 等扩展格式在所有 Android 设备上表现一致。

## 完成判据

- Android 工程可以在 Android Studio 中打开并完成 Gradle 同步。
- Debug APK 能够构建生成。
- App 能选择本地图片并显示预览、图片信息、综合分和子项评分。
- 对清晰、模糊、过曝、欠曝、噪点明显等样本，评分趋势与人工判断基本一致。
- 至少保留一个评分失准反例，并解释算法为什么会误判。

## 适用对象

- 需要提交 Android APK、源码仓库或真机演示的课程作业。
- 需要快速筛查 JPEG、PNG、WebP 图片画质的场景。
- 需要可解释传统 CV 指标，而不是黑盒模型的场景。

## 输入与输出

输入：

- 本地图片文件或相册图片。
- 核心保证格式：JPEG、PNG、WebP。
- 扩展尝试格式：HEIC/HEIF、AVIF、BMP、GIF 首帧。

输出：

- 图片预览。
- 文件名、格式、原始尺寸、分析尺寸、采样倍率、文件大小。
- 综合画质评分，范围 0-100。
- 清晰度、曝光、噪点、对比度、色偏、色彩丰富度、信息熵 7 个子项。
- 每个指标的原始数值和中文评价说明。

## 操作步骤

1. 使用 Android Studio 打开本工程目录。
2. 等待 Gradle 同步完成。
3. 连接 Android 手机，开启“开发者选项”和“USB 调试”。
4. 点击 Android Studio 的 Run，或使用命令行构建并安装 Debug APK。
5. 打开 App 后点击“选择图片”。
6. 选择一张图片，等待 App 输出评分结果。
7. 用多类测试样本记录人工判断、App 分数和是否一致。

命令行构建：

```powershell
$env:JAVA_HOME="D:\Program Files\Android\Android Studio\jbr"
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat testDebugUnitTest assembleDebug
```

命令行安装到手机：

```powershell
adb devices
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

## 运行前提

- Android Studio 已安装。
- Android SDK 路径：`D:\Android\Sdk`。
- `ANDROID_HOME` 和 `ANDROID_SDK_ROOT` 指向 `D:\Android\Sdk`。
- JDK 使用 Android Studio 自带 JDK：`D:\Program Files\Android\Android Studio\jbr`。
- 当前工程使用 `compileSdk 35`、`minSdk 26`、`targetSdk 35`。

## 算法指标说明

- 清晰度 / 模糊检测：灰度图 Laplacian 方差。方差越高，边缘和纹理变化越强，通常越清晰。
- 曝光 / 亮度分布：统计亮度均值、过曝像素比例、欠曝像素比例。
- 噪点估计：在低梯度区域计算像素与邻域均值的残差标准差，用于估计随机噪声或压缩伪影。
- 对比度：结合亮度标准差和 P95-P5 动态范围。
- 色偏：统计 RGB 三个通道的均值差异，判断整体白平衡是否明显偏移。
- 色彩丰富度 / 艳丽度：参考 Hasler & Suesstrunk 色彩丰富度思想，衡量颜色分布是否丰富。
- 细节丰富度 / 信息熵：统计亮度直方图的信息熵，衡量画面信息量和纹理复杂度。

综合评分权重：

- 清晰度 25%
- 曝光 20%
- 噪点 15%
- 对比度 15%
- 色偏 10%
- 色彩丰富度 8%
- 信息熵 7%

## 边界与风险

- 大图分析前会缩放到最长边约 1024 px，结果适合快速筛查，不适合像素级质检。
- GIF 仅分析静态首帧，不分析动画全过程。
- HEIC/HEIF 和 AVIF 支持与系统版本、设备解码器有关。
- 低纹理图片、夜景、艺术滤镜、景深虚化照片可能与人工主观判断不完全一致。
- 当前算法无法区分“拍糊了”和“背景被有意虚化”，需要通过反例分析说明局限。

## 结果位置

- Debug APK：`app/build/outputs/apk/debug/app-debug.apk`
- 源码入口：`app/src/main/java/com/example/imagequality/MainActivity.java`
- 核心算法：`app/src/main/java/com/example/imagequality/QualityAnalyzer.java`
- 解码逻辑：`app/src/main/java/com/example/imagequality/ImageLoader.java`
- 验证材料：`验证证据/`
- 可复现说明：`可复现代码仓库说明.md`

## 提交建议

提交时建议包含源码、README、约束与决策说明、AI 协作说明、验证证据和可复现代码仓库说明。若现场答辩时间有限，优先展示 App 选择图片、输出评分、样本对比表和一个反例原因分析。
