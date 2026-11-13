package com.course.facevalue.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Face
import androidx.compose.material.icons.rounded.FlipCameraAndroid
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.course.facevalue.analysis.GenderProfile
import com.course.facevalue.analysis.KeypointLayout
import com.course.facevalue.analysis.model.Pt
import com.course.facevalue.camera.LiveFaceAnalyzer
import com.course.facevalue.camera.LiveFrame
import com.course.facevalue.ui.AppViewModel
import com.course.facevalue.ui.components.SelectPill
import com.course.facevalue.ui.theme.Cream
import com.course.facevalue.ui.theme.Ink
import com.course.facevalue.ui.theme.InkMuted
import com.course.facevalue.ui.theme.Rose
import com.course.facevalue.ui.theme.Violet
import com.course.facevalue.ui.theme.VioletSoft
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.math.max

private enum class CameraMode(val label: String) {
    PHOTO("拍照分析"),
    LIVE("实时检测")
}

@Composable
fun CameraScreen(vm: AppViewModel, initialLive: Boolean = false) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasPermission = granted }

    LaunchedEffect(Unit) {
        if (!hasPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    if (!hasPermission) {
        CameraPermissionPanel(
            onRequest = { permissionLauncher.launch(Manifest.permission.CAMERA) },
            onBack = { vm.goHome() }
        )
        return
    }

    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    val mainHandler = remember { Handler(Looper.getMainLooper()) }
    var provider by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var frame by remember { mutableStateOf<LiveFrame?>(null) }
    var lensFacing by remember { mutableIntStateOf(CameraSelector.LENS_FACING_FRONT) }
    var mode by remember { mutableStateOf(if (initialLive) CameraMode.LIVE else CameraMode.PHOTO) }
    var capturing by remember { mutableStateOf(false) }
    var cameraError by remember { mutableStateOf<String?>(null) }

    val analyzer = remember { LiveFaceAnalyzer { frame = it } }
    LaunchedEffect(vm.profile) { analyzer.profile = vm.profile }
    DisposableEffect(Unit) {
        onDispose {
            runCatching { provider?.unbindAll() }
            analyzer.close()
            cameraExecutor.shutdown()
        }
    }

    LaunchedEffect(lensFacing) {
        try {
            val p = context.awaitCameraProvider()
            provider = p
            p.unbindAll()
            val selector = CameraSelector.Builder().requireLensFacing(lensFacing).build()
            val preview = Preview.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .build()
                .also { it.setSurfaceProvider(previewView.surfaceProvider) }
            val analysis = ImageAnalysis.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also { it.setAnalyzer(cameraExecutor, analyzer) }
            val capture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .setTargetRotation(previewView.display.rotation)
                .build()
            p.bindToLifecycle(lifecycleOwner, selector, preview, analysis, capture)
            imageCapture = capture
            cameraError = null
        } catch (e: Exception) {
            cameraError = e.message ?: "相机启动失败"
        }
    }

    fun takePhoto() {
        val capture = imageCapture ?: return
        if (capturing) return
        capturing = true
        val file = File(context.cacheDir, "face_${System.currentTimeMillis()}.jpg")
        val options = ImageCapture.OutputFileOptions.Builder(file).build()
        capture.takePicture(options, cameraExecutor, object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                mainHandler.post {
                    capturing = false
                    vm.analyzePhoto(file)
                }
            }

            override fun onError(exception: ImageCaptureException) {
                mainHandler.post {
                    capturing = false
                    Toast.makeText(context, "拍照失败：${exception.message}", Toast.LENGTH_SHORT).show()
                }
            }
        })
    }

    val isFront = lensFacing == CameraSelector.LENS_FACING_FRONT
    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())

        val live = frame
        if (live?.landmarks != null) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val landmarks = live.landmarks ?: return@Canvas
                val imgW = live.imageWidth.toFloat()
                val imgH = live.imageHeight.toFloat()
                if (imgW <= 0f || imgH <= 0f) return@Canvas
                val scale = max(size.width / imgW, size.height / imgH)
                val drawW = imgW * scale
                val drawH = imgH * scale
                val left = (size.width - drawW) / 2f
                val top = (size.height - drawH) / 2f

                fun map(p: Pt): Offset {
                    var x = left + p.x * drawW
                    if (isFront) x = size.width - x
                    return Offset(x, top + p.y * drawH)
                }

                fun path(points: List<Pt>, close: Boolean): Path {
                    val path = Path()
                    points.forEachIndexed { i, p ->
                        val o = map(p)
                        if (i == 0) path.moveTo(o.x, o.y) else path.lineTo(o.x, o.y)
                    }
                    if (close) path.close()
                    return path
                }

                drawPath(path(landmarks.face, true), Color.White.copy(alpha = 0.9f), style = Stroke(1.6.dp.toPx()))
                drawPath(path(landmarks.leftEye, true), Color(0xFF8CF5FF), style = Stroke(1.2.dp.toPx()))
                drawPath(path(landmarks.rightEye, true), Color(0xFF8CF5FF), style = Stroke(1.2.dp.toPx()))
                drawPath(
                    path(landmarks.upperLipTop + landmarks.lowerLipBottom, true),
                    Color(0xFFFFB4C8),
                    style = Stroke(1.dp.toPx())
                )

                for (point in KeypointLayout.layout106(landmarks)) {
                    drawCircle(Color(0xFF6EF3FF), radius = 1.9.dp.toPx(), center = map(point.point))
                }

                val b = landmarks.bounds
                val tl = map(Pt(b.left, b.top))
                val br = map(Pt(b.right, b.bottom))
                val corner = 16.dp.toPx()
                val color = Rose
                // 前置摄像头会镜像 x 轴，映射后 tl/br 可能左右互换，因此先归一化四边。
                val boxLeft = minOf(tl.x, br.x)
                val boxRight = maxOf(tl.x, br.x)
                val boxTop = minOf(tl.y, br.y)
                val boxBottom = maxOf(tl.y, br.y)
                fun bracket(x: Float, y: Float, dx: Float, dy: Float) {
                    drawLine(color, Offset(x, y), Offset(x + dx * corner, y), 2.dp.toPx(), StrokeCap.Round)
                    drawLine(color, Offset(x, y), Offset(x, y + dy * corner), 2.dp.toPx(), StrokeCap.Round)
                }
                bracket(boxLeft, boxTop, 1f, 1f)
                bracket(boxRight, boxTop, -1f, 1f)
                bracket(boxLeft, boxBottom, 1f, -1f)
                bracket(boxRight, boxBottom, -1f, -1f)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.62f), Color.Transparent)))
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircleIconButton(icon = Icons.Rounded.Close, onClick = { vm.goHome() })
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("颜析相机", color = Color.White, style = MaterialTheme.typography.titleLarge)
                    Text(
                        if (mode == CameraMode.LIVE) "实时检测 · 画面不上传" else "拍照分析 · 本地处理",
                        color = Color.White.copy(alpha = 0.72f),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
                CircleIconButton(icon = Icons.Rounded.FlipCameraAndroid, onClick = {
                    lensFacing = if (lensFacing == CameraSelector.LENS_FACING_FRONT) {
                        CameraSelector.LENS_FACING_BACK
                    } else {
                        CameraSelector.LENS_FACING_FRONT
                    }
                })
            }
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (mode == CameraMode.LIVE) {
                    Row(
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.45f), RoundedCornerShape(50))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .background(
                                    if ((live?.faceCount ?: 0) > 0) Color(0xFF6EF3FF) else Rose,
                                    CircleShape
                                )
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            when {
                                live?.score != null -> "实时评分 ${vm.scale.format(live.score.raw)}"
                                live != null -> "未检测到人脸"
                                else -> "正在初始化…"
                            },
                            color = Color.White,
                            style = MaterialTheme.typography.labelLarge
                        )
                        if (live != null && live.fps > 0f) {
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "${live.fps.toInt()} fps",
                                color = Color.White.copy(alpha = 0.65f),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                } else {
                    Text(
                        "请正对镜头，保持光线均匀、露出额头",
                        color = Color.White.copy(alpha = 0.8f),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.82f))))
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier
                    .background(Color.White.copy(alpha = 0.12f), RoundedCornerShape(50))
                    .padding(4.dp)
            ) {
                CameraMode.entries.forEach { item ->
                    val selected = mode == item
                    Box(
                        modifier = Modifier
                            .background(if (selected) Color.White else Color.Transparent, RoundedCornerShape(50))
                            .clickable { mode = item }
                            .padding(horizontal = 18.dp, vertical = 8.dp)
                    ) {
                        Text(
                            item.label,
                            color = if (selected) Ink else Color.White,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GenderProfile.entries.forEach { item ->
                    SelectPill(
                        text = item.label,
                        selected = vm.profile == item,
                        onClick = { vm.selectProfile(item) }
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .size(74.dp)
                    .border(3.dp, Color.White, CircleShape)
                    .clickable(enabled = !capturing && imageCapture != null) { takePhoto() },
                contentAlignment = Alignment.Center
            ) {
                if (capturing) {
                    CircularProgressIndicator(color = Violet, modifier = Modifier.size(30.dp), strokeWidth = 2.5.dp)
                } else {
                    Box(
                        modifier = Modifier
                            .size(58.dp)
                            .background(Color.White, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Rounded.PhotoCamera,
                            contentDescription = "拍照",
                            tint = Violet,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                if (mode == CameraMode.LIVE) "实时评分每秒刷新，点击快门可保存高清分析" else "点击快门，拍摄后自动分析",
                color = Color.White.copy(alpha = 0.6f),
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center
            )
            if (cameraError != null) {
                Spacer(Modifier.height(8.dp))
                Text(cameraError!!, color = Color(0xFFFFB4C8), style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun CircleIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .background(Color.Black.copy(alpha = 0.35f), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun CameraPermissionPanel(onRequest: () -> Unit, onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Cream)
            .padding(28.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(84.dp)
                .background(VioletSoft, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.Face, contentDescription = null, tint = Violet, modifier = Modifier.size(40.dp))
        }
        Spacer(Modifier.height(20.dp))
        Text("需要相机权限", style = MaterialTheme.typography.headlineMedium, color = Ink)
        Spacer(Modifier.height(8.dp))
        Text(
            "相机画面只在本机用于人脸检测与关键点提取，不会上传或保存原始视频流。若已拒绝，请在系统设置中开启相机权限。",
            style = MaterialTheme.typography.bodyMedium,
            color = InkMuted,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(24.dp))
        Box(
            modifier = Modifier
                .background(Brush.horizontalGradient(listOf(Violet, Rose)), RoundedCornerShape(50))
                .clickable(onClick = onRequest)
                .padding(horizontal = 24.dp, vertical = 12.dp)
        ) {
            Text("授予相机权限", color = Color.White, style = MaterialTheme.typography.titleMedium)
        }
        Spacer(Modifier.height(10.dp))
        Text(
            "返回首页",
            color = InkMuted,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.clickable(onClick = onBack).padding(10.dp)
        )
    }
}

private suspend fun Context.awaitCameraProvider(): ProcessCameraProvider =
    suspendCancellableCoroutine { cont ->
        val future = ProcessCameraProvider.getInstance(this)
        future.addListener(
            {
                try {
                    cont.resume(future.get())
                } catch (e: Exception) {
                    cont.cancel(e)
                }
            },
            ContextCompat.getMainExecutor(this)
        )
    }
