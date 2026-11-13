package com.course.facevalue.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.course.facevalue.ui.AppViewModel
import com.course.facevalue.ui.components.SoftCard
import com.course.facevalue.ui.theme.Cream
import com.course.facevalue.ui.theme.Ink
import com.course.facevalue.ui.theme.InkMuted
import com.course.facevalue.ui.theme.Rose
import com.course.facevalue.ui.theme.Violet
import com.course.facevalue.ui.theme.VioletSoft

@Composable
fun EthicsScreen(vm: AppViewModel) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(Cream),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 4.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier.statusBarsPadding().fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { vm.goHome() }) {
                    Icon(Icons.Rounded.ArrowBack, contentDescription = "返回", tint = Ink)
                }
                Text(
                    "伦理边界与使用声明",
                    style = MaterialTheme.typography.titleLarge,
                    color = Ink,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(listOf(Color(0xFF5B3DF5), Color(0xFF9A5BF0))),
                        RoundedCornerShape(26.dp)
                    )
                    .padding(22.dp)
            ) {
                Column {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .background(Color.White.copy(alpha = 0.18f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.Shield, contentDescription = null, tint = Color.White)
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "颜值分数只是人群\n主观偏好的统计近似",
                        color = Color.White,
                        style = MaterialTheme.typography.headlineMedium,
                        lineHeight = 30.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "它不度量一个人的价值，也不构成任何医学、心理或身份判断。",
                        color = Color.White.copy(alpha = 0.82f),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        item {
            EthicsCard(
                icon = Icons.Rounded.Info,
                title = "分数意味着什么",
                paragraphs = listOf(
                    "本 App 用可解释的几何比例与肤质统计，近似模拟“人群平均审美偏好”的一个侧面。分数越高，表示测量指标越接近预设参考区间，仅此而已。",
                    "参考区间来自面部比例的人类测量学常用近似值（三庭五眼、鼻唇比例等）以及公开数据集对群体评分的统计结论，并非“客观美”的公式。",
                    "同一张脸在不同光线、角度、表情、妆容下会得到不同分数；分数波动是测量条件变化的结果，不是对个人的评价。"
                )
            )
        }

        item {
            EthicsCard(
                icon = Icons.Rounded.Science,
                title = "分性别依据的来源与边界",
                paragraphs = listOf(
                    "男性/女性/通用三种预设只改变指标权重，不做性别识别，也不要求用户声明身份。App 不会从图像推断性别。",
                    "权重参考 SCUT-FBP5500、MEBeauty、LiveBeauty 等公开研究对群体评分与面部特征的统计结论：男性更常与轮廓/下颌线、眉眼间距相关，女性更常与唇形、眼型、面部柔和度相关。",
                    "这些是数据集层面的统计相关性，存在文化、种族、年龄与时代偏见，不能代表所有个体或文化。切换预设只用于课堂演示“权重如何影响评分”。"
                )
            )
        }

        item {
            EthicsCard(
                icon = Icons.Rounded.Lock,
                title = "隐私与数据处理",
                paragraphs = listOf(
                    "ML Kit 人脸模型随 APK 内置，检测、关键点与评分全部在设备本地完成；应用未申请 INTERNET 权限，从系统层面保证人脸图像无法上传。",
                    "相册图片通过系统照片选择器读取，应用只获得你主动选择的那一张；相机画面仅用于实时预览与分析，不保存视频流。",
                    "历史记录只在本机保存评分、指标文字与缩略图，可随时删除或一键清空。卸载应用即清除全部本地数据。"
                )
            )
        }

        item {
            EthicsCard(
                icon = Icons.Rounded.Shield,
                title = "医学与身份判断边界",
                paragraphs = listOf(
                    "本 App 不包含也不宣称任何疾病识别能力：不能诊断唐氏综合征、阿尔茨海默病或其他疾病，也不能评估健康、认知或心理状态。",
                    "本 App 不实现性取向识别，不把任何面部特征用于推断性取向、人格、犯罪倾向或其他敏感属性。",
                    "不使用真实用户进行公开打分、排名、标签化或歧视性展示；课程演示仅使用 StyleGAN 合成的虚拟人脸。"
                )
            )
        }

        item {
            EthicsCard(
                icon = Icons.Rounded.Info,
                title = "已知局限",
                paragraphs = listOf(
                    "依赖单张正面 2D 图像，无法还原真实 3D 结构；侧脸、遮挡、刘海、眼镜会显著影响关键点。",
                    "“上庭”以面部轮廓顶端近似发际线，额头被头发遮挡时三庭测量会偏差。",
                    "肤色均匀度受光照、油光、相机白平衡影响，并非皮肤健康指标。",
                    "评分权重是人为设定的可解释规则，没有在大规模真人数据上做校准；与真人审美评分的一致性有待进一步实验。"
                )
            )
        }

        item {
            SoftCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    Text("技术与数据来源", style = MaterialTheme.typography.titleMedium, color = Ink)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "• Google ML Kit Face Detection（内置模型，13 组轮廓）\n" +
                            "• Android CameraX（预览、拍照、实时分析）\n" +
                            "• Jetpack Compose（界面）\n" +
                            "• 比例参考：SCUT-FBP5500、MEBeauty、LiveBeauty 等公开研究\n" +
                            "• 示例图片：StyleGAN 合成人脸（非真实人物）",
                        style = MaterialTheme.typography.bodyMedium,
                        color = InkMuted,
                        lineHeight = 22.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "本应用为课程设计作品，仅用于教学、技术探索与伦理讨论。",
                        style = MaterialTheme.typography.labelSmall,
                        color = Violet
                    )
                }
            }
        }

        item {
            Column(
                modifier = Modifier.fillMaxWidth().navigationBarsPadding(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("颜析 FaceValue · v1.1", style = MaterialTheme.typography.labelSmall, color = InkMuted)
                Text(
                    "基于 Android 的颜值打分与面部属性分析 APP",
                    style = MaterialTheme.typography.labelSmall,
                    color = InkMuted
                )
            }
        }
    }
}

@Composable
private fun EthicsCard(icon: ImageVector, title: String, paragraphs: List<String>) {
    SoftCard(modifier = Modifier.fillMaxWidth(), padding = 18) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .background(VioletSoft, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = Violet, modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.width(10.dp))
                Text(title, style = MaterialTheme.typography.titleMedium, color = Ink, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(10.dp))
            paragraphs.forEachIndexed { index, text ->
                Row {
                    Text("•", color = Rose, style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.width(8.dp))
                    Text(text, style = MaterialTheme.typography.bodyMedium, color = InkMuted, lineHeight = 21.sp)
                }
                if (index != paragraphs.lastIndex) Spacer(Modifier.height(8.dp))
            }
        }
    }
}
