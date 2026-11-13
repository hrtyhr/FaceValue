package com.course.facevalue.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.course.facevalue.ui.screens.CameraScreen
import com.course.facevalue.ui.screens.EthicsScreen
import com.course.facevalue.ui.screens.HomeScreen
import com.course.facevalue.ui.screens.ResultScreen
import com.course.facevalue.ui.screens.StoredDetailScreen
import com.course.facevalue.ui.theme.Ink
import com.course.facevalue.ui.theme.InkMuted
import com.course.facevalue.ui.theme.Violet

@Composable
fun FaceValueRoot(vm: AppViewModel = viewModel()) {
    BackHandler(enabled = vm.analyzing || vm.screen !is Screen.Home) {
        if (!vm.analyzing) vm.goHome()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when (val screen = vm.screen) {
            is Screen.Home -> HomeScreen(vm)
            is Screen.Camera -> CameraScreen(vm, initialLive = screen.live)
            is Screen.Ethics -> EthicsScreen(vm)
            is Screen.Result -> ResultScreen(vm, screen.outcome)
            is Screen.StoredDetail -> StoredDetailScreen(vm, screen.record)
        }

        if (vm.analyzing) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.45f))
                    .pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                awaitPointerEvent(PointerEventPass.Initial)
                                    .changes.forEach { it.consume() }
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Color.White,
                    shadowElevation = 8.dp
                ) {
                    Column(
                        modifier = Modifier
                            .widthIn(min = 250.dp)
                            .padding(horizontal = 26.dp, vertical = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = Violet, strokeWidth = 3.dp, modifier = Modifier.size(38.dp))
                        Spacer(Modifier.height(16.dp))
                        Text(vm.analyzingLabel, style = MaterialTheme.typography.titleMedium, color = Ink)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "首次使用需加载内置人脸模型",
                            style = MaterialTheme.typography.labelSmall,
                            color = InkMuted
                        )
                    }
                }
            }
        }

        val error = vm.error
        if (error != null) {
            AlertDialog(
                onDismissRequest = { vm.dismissError() },
                title = { Text("无法完成分析", color = Ink) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(error, style = MaterialTheme.typography.bodyMedium, color = InkMuted)
                        Text(
                            "拍摄建议：正脸、光线均匀、露出额头和眉毛、避免遮挡。",
                            style = MaterialTheme.typography.labelSmall,
                            color = Violet
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = { vm.dismissError() }) { Text("知道了", color = Violet) }
                }
            )
        }
    }
}
