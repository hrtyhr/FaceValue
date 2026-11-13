# 颜析 FaceValue

> 基于 Android 的颜值打分与面部属性分析 APP
>
> 本地人脸检测 · 133 轮廓点 · 三庭五眼 · 可解释评分 · 分性别权重 · 无网络权限


## 1. 项目简介

FaceValue 是一个完全离线的 Android 人脸几何分析应用。它使用 ML Kit 内置人脸模型检测 13 组、
共 133 个面部轮廓点，重采样为 68/106 点布局；在滚转校正后计算三庭、五眼、对称性、眉眼距、
鼻部比例、唇形、下颌轮廓、眼型，并从双颊/额头采样估计肤色均匀度与表情自然度。评分采用
高斯隶属函数与可解释权重，输出 1–10 分（可换算 1–5 分）、可信度、优势项、可改善项与逐项依据。

应用不申请网络权限，人脸图像、关键点与评分全部在设备本地完成；历史记录只保存评分文字与缩略图，
可随时清空。**分数只是人群主观偏好的统计近似，不代表客观美丑，也不构成任何医学或身份判断。**

## 2. 功能

| 功能要求 | 实现 |
|---|---|
| 相册选择 | 系统 Photo Picker，无需存储权限 |
| 摄像头拍照 | CameraX 预览 + ImageCapture，拍摄后自动分析 |
| 实时人脸检测 | ImageAnalysis + 关键点叠加 + EMA 平滑实时分（约 13–15 fps） |
| 人脸框 / 68/106 关键点 | ML Kit 133 轮廓点，等弧长重采样为 68/106 点，可切换叠加 |
| 三庭五眼 / 对称 / 眉眼 / 鼻梁 / 唇形 | 10 项几何与肤质指标，逐项展示测量值 |
| 1–5 或 1–10 分 | 两种计分制可切换，同一结果无损换算 |
| 简要依据 | 子分数 + 权重 + 测量值 + 文字解释 + 优势/建议 |
| 分性别依据 | 男性 / 女性 / 通用三套权重，切换即时重算；不做性别识别 |
| 本地处理 | 移除 INTERNET / ACCESS_NETWORK_STATE 权限，飞行模式可分析 |
| 历史记录 | 本机 JSON + 缩略图，最多 12 条，可删除/清空 |
| 伦理说明 | 独立页面：分数含义、隐私、医学与敏感属性边界 |

## 3. 技术栈

- Kotlin 2.0.21 / Jetpack Compose（BOM 2024.09.03）+ Material 3
- CameraX 1.3.4（core / camera2 / lifecycle / view）
- ML Kit `face-detection:16.1.7`（内置模型，离线）
- Coroutines、ViewModel、SharedPreferences
- minSdk 24 / targetSdk 35 / compileSdk 35，JDK 17+

## 4. 构建与运行

```bash
# 1. 在 local.properties 中写入 Android SDK 路径
#    sdk.dir=C:\\Users\\<user>\\AppData\\Local\\Android\\Sdk

# 2. 构建 debug APK
./gradlew :app:assembleDebug        # Windows: gradlew.bat :app:assembleDebug

# 3. 安装
adb install -r app/build/outputs/apk/debug/app-debug.apk

# 4. 单元测试
./gradlew :app:testDebugUnitTest

# 5. 批量实验（需要已连接设备/模拟器）
./gradlew :app:connectedDebugAndroidTest
```

首次打开建议先点“示例 1/示例 2”查看完整流程；示例为 StyleGAN 合成人脸，不是真实人物。

## 5. 测试与实验

- JVM 单元测试 12 项全部通过（几何比例、对称性单调性、权重归一化、关键点数量、计分制、肤质方向、眼角倾斜符号、画面缩放不变性、缺失轮廓拒绝）。
- 22 张合成成年人脸批量实验：检测成功率 22/22，单张检测平均 320.8 ms；通用权重评分均值 7.83、
  标准差 0.85；男性/女性权重均值 7.82/7.66。
- 飞行模式下可完成完整分析；`aapt2 dump permissions` 显示最终 APK 仅保留 `CAMERA`。

## 6. 隐私与伦理

- 不申请网络权限，移除 ML Kit 依赖默认声明的 INTERNET / ACCESS_NETWORK_STATE。
- 相册只读取用户主动选择的一张图片；相机预览不落盘。
- 不实现疾病诊断、阿尔茨海默分析、性取向识别；应用内均说明其边界。
- 实验只使用 StyleGAN 合成人脸，不使用真实人物照片，也不使用 SCUT-FBP5500 等数据集训练模型。
- “男性/女性/通用”只是权重预设，不做性别识别。


## 7. 目录结构

```
FaceValue/
├── app/src/main/java/com/course/facevalue/
│   ├── analysis/     # 检测、关键点、几何、肤质、评分
│   ├── camera/       # CameraX 实时分析
│   ├── data/         # 本机历史
│   ├── ui/           # Compose 页面与 ViewModel
│   └── util/         # 解码、EXIF、缩略图
├── app/src/test/     # 12 项 JVM 单元测试
├── app/src/androidTest/  # 22 张合成人脸的批量实验
├── docs/             # 图表、截图、实验数据
└── gradlew.bat
```

## 8. 已知限制

- 上庭以检测框顶边近似发际线，刘海/帽子会造成偏差。
- 单张 2D 图无法恢复 3D 结构，侧脸与遮挡会放大对称性/五眼误差。
- 规则评分未在真人评分数据上校准，与人群偏好的一致性未知。
- 肤色采样受光照、油光、白平衡影响，不是皮肤健康指标。
- 实验在模拟器与合成人脸上完成，缺少真机与真实用户研究。
