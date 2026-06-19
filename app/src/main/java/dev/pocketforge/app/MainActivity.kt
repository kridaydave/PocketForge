package dev.pocketforge.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PocketForgeApp()
        }
    }
}

@Composable
private fun PocketForgeApp() {
    val workflowItems = listOf(
        WorkflowItem(
            step = "01",
            title = "Connect repo",
            detail = "Awaiting source",
            state = "Ready",
            accent = ForgeGreen,
        ),
        WorkflowItem(
            step = "02",
            title = "Inspect files",
            detail = "Workspace scan",
            state = "Queued",
            accent = ForgeAmber,
        ),
        WorkflowItem(
            step = "03",
            title = "Build in Actions",
            detail = "Remote Android build",
            state = "CI only",
            accent = ForgeCoral,
        ),
    )

    PocketForgeTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = ForgeCanvas,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding(),
            ) {
                HarnessTopBar()

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp, vertical = 22.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    HeaderPanel()
                    workflowItems.forEach { item ->
                        WorkflowRow(item = item)
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    FooterStrip()
                }
            }
        }
    }
}

@Composable
private fun HarnessTopBar() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 68.dp)
            .background(Color.White)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "PocketForge",
                color = ForgeInk,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "Mobile coding harness",
                color = ForgeMuted,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        StatusChip(text = "Phase 0", color = ForgeGreen)
    }
}

@Composable
private fun HeaderPanel() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = ForgeInk,
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = "Actions-first shell",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StatusChip(text = "SDK 37", color = ForgeGreen, dark = true)
                StatusChip(text = "Kotlin 2.3", color = ForgeAmber, dark = true)
                StatusChip(text = "Compose", color = ForgeCoral, dark = true)
            }
        }
    }
}

@Composable
private fun WorkflowRow(item: WorkflowItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, ForgeLine),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Surface(
                modifier = Modifier.size(42.dp),
                shape = MaterialTheme.shapes.medium,
                color = item.accent.copy(alpha = 0.14f),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = item.step,
                        color = item.accent,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    color = ForgeInk,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = item.detail,
                    color = ForgeMuted,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            StatusChip(text = item.state, color = item.accent)
        }
    }
}

@Composable
private fun FooterStrip() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = Color.White,
        border = BorderStroke(1.dp, ForgeLine),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(ForgeGreen, shape = MaterialTheme.shapes.small),
            )
            Text(
                text = "Local builds disabled",
                color = ForgeInk,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun StatusChip(
    text: String,
    color: Color,
    dark: Boolean = false,
) {
    val foreground = if (dark) Color.White else color
    val background = if (dark) color.copy(alpha = 0.28f) else color.copy(alpha = 0.12f)
    val border = if (dark) color.copy(alpha = 0.42f) else color.copy(alpha = 0.26f)

    Surface(
        shape = MaterialTheme.shapes.small,
        color = background,
        border = BorderStroke(1.dp, border),
    ) {
        Text(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
            text = text,
            color = foreground,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun PocketForgeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = ForgeGreen,
            secondary = ForgeAmber,
            tertiary = ForgeCoral,
            background = ForgeCanvas,
            surface = Color.White,
            onPrimary = Color.White,
            onSecondary = Color.White,
            onTertiary = Color.White,
            onBackground = ForgeInk,
            onSurface = ForgeInk,
        ),
        typography = Typography(),
        shapes = androidx.compose.material3.Shapes(
            small = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
            medium = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
            large = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
        ),
        content = content,
    )
}

private data class WorkflowItem(
    val step: String,
    val title: String,
    val detail: String,
    val state: String,
    val accent: Color,
)

private val ForgeInk = Color(0xFF18212A)
private val ForgeMuted = Color(0xFF65717A)
private val ForgeCanvas = Color(0xFFF5F7F6)
private val ForgeLine = Color(0xFFE1E7E4)
private val ForgeGreen = Color(0xFF1F7A5D)
private val ForgeAmber = Color(0xFF9B6200)
private val ForgeCoral = Color(0xFFB55243)

@Preview(showBackground = true, widthDp = 360, heightDp = 740)
@Composable
private fun PocketForgePreview() {
    PocketForgeApp()
}
