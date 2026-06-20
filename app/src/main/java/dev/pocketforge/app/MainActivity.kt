package dev.pocketforge.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
    var selectedTab by remember { mutableStateOf(WorkbenchTab.Today) }
    var selectedFilter by remember { mutableStateOf("Mobile") }
    var selectedTheme by remember { mutableStateOf(ForgeGreen) }

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
                WorkbenchTopBar(selectedTab = selectedTab)

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 18.dp, vertical = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    when (selectedTab) {
                        WorkbenchTab.Today -> TodayScreen()
                        WorkbenchTab.Ideas -> IdeasScreen(
                            selectedFilter = selectedFilter,
                            onFilterSelected = { selectedFilter = it },
                        )

                        WorkbenchTab.Build -> BlueprintScreen()
                        WorkbenchTab.Ci -> PipelineScreen()
                        WorkbenchTab.Ship -> ShipScreen(
                            selectedTheme = selectedTheme,
                            onThemeSelected = { selectedTheme = it },
                        )
                    }
                }

                BottomWorkbenchTabs(
                    selectedTab = selectedTab,
                    onTabSelected = { selectedTab = it },
                )
            }
        }
    }
}

@Composable
private fun WorkbenchTopBar(selectedTab: WorkbenchTab) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(ForgePaper)
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        LogoMark()
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "PocketForge",
                color = ForgeInk,
                fontSize = 21.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = selectedTab.subtitle,
                color = ForgeMuted,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        StatusChip(text = "Phase 1", color = ForgeRust)
    }
}

@Composable
private fun LogoMark() {
    Box(
        modifier = Modifier
            .size(42.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(ForgeGreen),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "PF",
            color = ForgePaper,
            fontSize = 14.sp,
            fontWeight = FontWeight.ExtraBold,
        )
    }
}

@Composable
private fun TodayScreen() {
    HeroPanel(
        title = "Shape one app before lunch.",
        body = "Turn a loose note into a scoped Android project with screens, milestones, and a cloud build path.",
        primaryAction = "New Forge",
        secondaryAction = "Capture",
    )

    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        MetricTile(
            modifier = Modifier.weight(1f),
            label = "Active build",
            value = "68%",
            detail = "Phase 1 UI branch is ready for Actions.",
            color = ForgeMint,
        )
        MetricTile(
            modifier = Modifier.weight(1f),
            label = "Next decision",
            value = "AI keys",
            detail = "Add a legitimate provider router after UI.",
            color = ForgePeach,
        )
    }

    SectionHeader(title = "Forge queue", action = "View all")
    QueueRow(
        badge = "A",
        title = "Creator workbench",
        detail = "Today, Ideas, Build, CI, Ship",
        state = "Now",
        color = ForgeMint,
    )
    QueueRow(
        badge = "B",
        title = "AI provider router",
        detail = "OpenRouter first, fallback providers later",
        state = "Next",
        color = ForgeGold,
    )
}

@Composable
private fun IdeasScreen(
    selectedFilter: String,
    onFilterSelected: (String) -> Unit,
) {
    val filters = listOf("Mobile", "AI", "Utility", "Playful")

    SectionTitleBlock(title = "Idea Forge", subtitle = "5 raw notes")
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        filters.forEach { filter ->
            FilterChip(
                text = filter,
                selected = filter == selectedFilter,
                onClick = { onFilterSelected(filter) },
            )
        }
    }

    FeatureCard(
        label = "Recommended route",
        title = "Creator workbench for fast app plans",
        body = "Useful on day one, expandable into AI workflows once provider keys are configured cleanly.",
        color = ForgeGold,
    ) {
        ScoreBars(values = listOf(0.88f, 0.74f, 0.66f))
    }

    QueueRow(
        badge = "1",
        title = "PocketForge Studio",
        detail = "Projects, notes, features, exports",
        state = "88",
        color = ForgeGold,
    )
    QueueRow(
        badge = "2",
        title = "Build Tracker",
        detail = "Milestones, CI status, release notes",
        state = "81",
        color = ForgeBlue,
    )
    QueueRow(
        badge = "3",
        title = "Provider Console",
        detail = "Budgets, limits, task routing",
        state = "79",
        color = ForgeBerry,
    )

    DarkActionButton(text = "Generate template variants")
}

@Composable
private fun BlueprintScreen() {
    DarkBlueprintPanel()

    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        PlanCell(
            modifier = Modifier.weight(1f),
            label = "Stack",
            value = "Compose",
        )
        PlanCell(
            modifier = Modifier.weight(1f),
            label = "Build",
            value = "Cloud APK",
        )
    }
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        PlanCell(
            modifier = Modifier.weight(1f),
            label = "Risk",
            value = "Key limits",
        )
        PlanCell(
            modifier = Modifier.weight(1f),
            label = "Cut first",
            value = "Real AI",
        )
    }

    RailCard(label = "Architecture sketch") {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ArchitectureNode(text = "Idea", modifier = Modifier.weight(1f))
            ArrowLine()
            ArchitectureNode(text = "Brief", modifier = Modifier.weight(1f))
            ArrowLine()
            ArchitectureNode(text = "APK", modifier = Modifier.weight(1f))
        }
    }

    QueueRow(
        badge = "!",
        title = "Do not build Android locally",
        detail = "Static validation here, GitHub Actions for APK.",
        state = "Rule",
        color = ForgeGold,
    )
}

@Composable
private fun PipelineScreen() {
    FeatureCard(
        label = "Cloud build",
        title = "APK artifact runs in GitHub Actions.",
        body = "Push this branch to trigger the debug APK workflow.",
        color = ForgePeach,
    ) {
        LinearProgressIndicator(
            progress = { 0.68f },
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .clip(CircleShape),
            color = ForgeGreen,
            trackColor = ForgePaper,
        )
    }

    BuildStep(title = "Repository scaffold", detail = "Compose skeleton committed", state = "Done", done = true)
    BuildStep(title = "GitHub workflow", detail = "SDK 36 is pinned in CI", state = "Done", done = true)
    BuildStep(title = "UI mockup approval", detail = "Bento Forge OS applied to app shell", state = "Live", live = true)
    BuildStep(title = "Install on phone", detail = "Download app-debug.apk artifact", state = "Next")

    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        MetricTile(
            modifier = Modifier.weight(1f),
            label = "Last run",
            value = "Pass",
            detail = "Phase 0 APK uploaded.",
            color = ForgeBlue,
        )
        MetricTile(
            modifier = Modifier.weight(1f),
            label = "Branch",
            value = "phase-1",
            detail = "Feature work stays isolated.",
            color = ForgeBerry,
        )
    }
}

@Composable
private fun ShipScreen(
    selectedTheme: Color,
    onThemeSelected: (Color) -> Unit,
) {
    FeatureCard(
        label = "Release pocket",
        title = "Your debug APK will appear here.",
        body = "Artifact metadata comes from GitHub Actions. For now this screen shows the release handoff shape.",
        color = selectedTheme,
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            color = ForgePaper,
            border = BorderStroke(2.dp, ForgeInk),
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "pocketforge-debug.apk",
                        color = ForgeInk,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                    )
                    Text(
                        text = "Artifact from GitHub Actions",
                        color = ForgeMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                StatusChip(text = "Get", color = ForgeInk)
            }
        }
    }

    SectionHeader(title = "Brand skin", action = "Save")
    Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
        listOf(ForgeGreen, ForgeRust, ForgeTeal, ForgeGold, ForgeBerry).forEach { color ->
            ThemeSwatch(
                color = color,
                selected = color == selectedTheme,
                onClick = { onThemeSelected(color) },
            )
        }
    }

    QueueRow(
        badge = "QR",
        title = "Phone install card",
        detail = "Share download instructions cleanly",
        state = "Share",
        color = ForgeMint,
    )
    QueueRow(
        badge = "RN",
        title = "Release notes",
        detail = "Generated from milestone changes",
        state = "Draft",
        color = ForgeGold,
    )
    DarkActionButton(text = "Create release checklist")
}

@Composable
private fun HeroPanel(
    title: String,
    body: String,
    primaryAction: String,
    secondaryAction: String,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = ForgeGreen,
        border = BorderStroke(2.dp, ForgeInk),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = title,
                color = ForgePaper,
                fontSize = 30.sp,
                lineHeight = 31.sp,
                fontWeight = FontWeight.ExtraBold,
            )
            Text(
                text = body,
                color = ForgePaper.copy(alpha = 0.78f),
                fontSize = 13.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.Medium,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {},
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ForgeGold,
                        contentColor = ForgeInk,
                    ),
                    shape = MaterialTheme.shapes.medium,
                ) {
                    Text(primaryAction, fontWeight = FontWeight.ExtraBold)
                }
                Button(
                    onClick = {},
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ForgePaper,
                        contentColor = ForgeInk,
                    ),
                    shape = MaterialTheme.shapes.medium,
                ) {
                    Text(secondaryAction, fontWeight = FontWeight.ExtraBold)
                }
            }
        }
    }
}

@Composable
private fun MetricTile(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    detail: String,
    color: Color,
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = color,
        border = BorderStroke(2.dp, ForgeInk),
    ) {
        Column(
            modifier = Modifier.padding(13.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            LabelText(text = label, dark = true)
            Text(
                text = value,
                color = ForgeInk,
                fontSize = 23.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = detail,
                color = ForgeInk.copy(alpha = 0.68f),
                fontSize = 10.sp,
                lineHeight = 14.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun QueueRow(
    badge: String,
    title: String,
    detail: String,
    state: String,
    color: Color,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = ForgePaper),
        border = BorderStroke(2.dp, ForgeInk),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(color),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = badge,
                    color = ForgeInk,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = ForgeInk,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = detail,
                    color = ForgeMuted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            StatusChip(text = state, color = ForgeInk)
        }
    }
}

@Composable
private fun FeatureCard(
    label: String,
    title: String,
    body: String,
    color: Color,
    footer: @Composable () -> Unit = {},
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = color,
        border = BorderStroke(2.dp, ForgeInk),
    ) {
        Column(
            modifier = Modifier.padding(15.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            LabelText(text = label, dark = color != ForgeGreen && color != ForgeInk)
            Text(
                text = title,
                color = if (color == ForgeGreen || color == ForgeInk) ForgePaper else ForgeInk,
                fontSize = 25.sp,
                lineHeight = 26.sp,
                fontWeight = FontWeight.ExtraBold,
            )
            Text(
                text = body,
                color = if (color == ForgeGreen || color == ForgeInk) {
                    ForgePaper.copy(alpha = 0.78f)
                } else {
                    ForgeInk.copy(alpha = 0.68f)
                },
                fontSize = 12.sp,
                lineHeight = 17.sp,
                fontWeight = FontWeight.SemiBold,
            )
            footer()
        }
    }
}

@Composable
private fun ScoreBars(values: List<Float>) {
    Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        values.forEach { value ->
            LinearProgressIndicator(
                progress = { value },
                modifier = Modifier
                    .weight(1f)
                    .height(8.dp)
                    .clip(CircleShape),
                color = ForgeTeal,
                trackColor = ForgePaper,
            )
        }
    }
}

@Composable
private fun DarkBlueprintPanel() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = ForgeInk,
        border = BorderStroke(2.dp, ForgePaper),
    ) {
        Column(
            modifier = Modifier.padding(17.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = "Ready to scaffold.",
                color = ForgePaper,
                fontSize = 30.sp,
                lineHeight = 31.sp,
                fontWeight = FontWeight.ExtraBold,
            )
            Text(
                text = "Five tabs, one local design direction, and GitHub Actions owns the Android build.",
                color = ForgePaper.copy(alpha = 0.72f),
                fontSize = 12.sp,
                lineHeight = 17.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun PlanCell(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = ForgeInk,
        border = BorderStroke(2.dp, ForgePaper),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            LabelText(text = label, dark = false)
            Text(
                text = value,
                color = ForgePaper,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun RailCard(label: String, content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = ForgeInk,
        border = BorderStroke(2.dp, ForgePaper),
    ) {
        Column(
            modifier = Modifier.padding(13.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            LabelText(text = label, dark = false)
            content()
        }
    }
}

@Composable
private fun ArchitectureNode(text: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = ForgeInk,
        border = BorderStroke(1.dp, ForgePaper),
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = text,
                color = ForgePaper,
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
            )
        }
    }
}

@Composable
private fun ArrowLine() {
    Box(
        modifier = Modifier
            .size(width = 18.dp, height = 2.dp)
            .background(ForgePaper),
    )
}

@Composable
private fun BuildStep(
    title: String,
    detail: String,
    state: String,
    done: Boolean = false,
    live: Boolean = false,
) {
    val color = when {
        done -> ForgeTeal
        live -> ForgeGold
        else -> ForgePaper
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(27.dp)
                .clip(CircleShape)
                .background(color),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = ForgeInk,
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold,
            )
            Text(
                text = detail,
                color = ForgeMuted,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
        StatusChip(text = state, color = ForgeInk)
    }
}

@Composable
private fun ThemeSwatch(
    color: Color,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .size(48.dp)
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        color = color,
        border = BorderStroke(if (selected) 4.dp else 2.dp, if (selected) ForgePaper else ForgeInk),
    ) {}
}

@Composable
private fun SectionHeader(title: String, action: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            modifier = Modifier.weight(1f),
            text = title,
            color = ForgeInk,
            fontSize = 16.sp,
            fontWeight = FontWeight.ExtraBold,
        )
        TextButton(onClick = {}) {
            Text(
                text = action,
                color = ForgeRust,
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
            )
        }
    }
}

@Composable
private fun SectionTitleBlock(title: String, subtitle: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = title,
            color = ForgeInk,
            fontSize = 25.sp,
            fontWeight = FontWeight.ExtraBold,
        )
        Text(
            text = subtitle,
            color = ForgeMuted,
            fontSize = 12.sp,
            fontWeight = FontWeight.ExtraBold,
        )
    }
}

@Composable
private fun FilterChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        shape = CircleShape,
        color = if (selected) ForgeTeal else ForgePaper,
        border = BorderStroke(2.dp, ForgeInk),
    ) {
        Text(
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 8.dp),
            text = text,
            color = if (selected) ForgePaper else ForgeInk,
            fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold,
        )
    }
}

@Composable
private fun DarkActionButton(text: String) {
    Button(
        modifier = Modifier.fillMaxWidth(),
        onClick = {},
        colors = ButtonDefaults.buttonColors(
            containerColor = ForgeInk,
            contentColor = ForgePaper,
        ),
        shape = MaterialTheme.shapes.medium,
    ) {
        Text(
            text = text,
            fontSize = 13.sp,
            fontWeight = FontWeight.ExtraBold,
        )
    }
}

@Composable
private fun StatusChip(
    text: String,
    color: Color,
) {
    Surface(
        shape = CircleShape,
        color = color.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.34f)),
    ) {
        Text(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
            text = text,
            color = color,
            fontSize = 10.sp,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun LabelText(text: String, dark: Boolean) {
    Text(
        text = text.uppercase(),
        color = if (dark) ForgeInk.copy(alpha = 0.58f) else ForgePaper.copy(alpha = 0.62f),
        fontSize = 9.sp,
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = 0.8.sp,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun BottomWorkbenchTabs(
    selectedTab: WorkbenchTab,
    onTabSelected: (WorkbenchTab) -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        shape = MaterialTheme.shapes.large,
        color = ForgePaper,
        border = BorderStroke(2.dp, ForgeInk),
    ) {
        Row(
            modifier = Modifier.padding(6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            WorkbenchTab.entries.forEach { tab ->
                val selected = tab == selectedTab
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(MaterialTheme.shapes.medium)
                        .clickable { onTabSelected(tab) }
                        .background(if (selected) ForgeInk else Color.Transparent)
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = 18.dp, height = 3.dp)
                            .clip(CircleShape)
                            .background(if (selected) ForgePaper else ForgeInk.copy(alpha = 0.45f)),
                    )
                    Text(
                        text = tab.label,
                        color = if (selected) ForgePaper else ForgeInk.copy(alpha = 0.58f),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun PocketForgeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = ForgeGreen,
            secondary = ForgeGold,
            tertiary = ForgeRust,
            background = ForgeCanvas,
            surface = ForgePaper,
            onPrimary = ForgePaper,
            onSecondary = ForgeInk,
            onTertiary = ForgePaper,
            onBackground = ForgeInk,
            onSurface = ForgeInk,
        ),
        typography = Typography(),
        shapes = androidx.compose.material3.Shapes(
            small = RoundedCornerShape(8.dp),
            medium = RoundedCornerShape(8.dp),
            large = RoundedCornerShape(8.dp),
        ),
        content = content,
    )
}

private enum class WorkbenchTab(
    val label: String,
    val subtitle: String,
) {
    Today("Today", "Workbench"),
    Ideas("Ideas", "Idea Forge"),
    Build("Build", "Blueprint"),
    Ci("CI", "Pipeline"),
    Ship("Ship", "Release Pocket"),
}

private val ForgeInk = Color(0xFF111214)
private val ForgeCanvas = Color(0xFFF3F0E8)
private val ForgePaper = Color(0xFFFFF9EA)
private val ForgeMuted = Color(0xFF6F6A60)
private val ForgeGreen = Color(0xFF16352D)
private val ForgeTeal = Color(0xFF1F8A66)
private val ForgeMint = Color(0xFFD8F1E8)
private val ForgeRust = Color(0xFFC76B3E)
private val ForgePeach = Color(0xFFF8DECF)
private val ForgeGold = Color(0xFFE2A336)
private val ForgeBlue = Color(0xFFDFE8FF)
private val ForgeBerry = Color(0xFFBF4B7C)

@Preview(showBackground = true, widthDp = 360, heightDp = 780)
@Composable
private fun PocketForgePreview() {
    PocketForgeApp()
}
