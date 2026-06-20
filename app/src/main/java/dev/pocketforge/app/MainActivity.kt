package dev.pocketforge.app

import android.content.Context
import android.content.SharedPreferences
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
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val draftStore = getSharedPreferences(AGENT_DRAFT_PREFS, Context.MODE_PRIVATE)

        setContent {
            PocketForgeApp(
                initialDraft = draftStore.loadAgentTaskDraft(),
                initialRecentBriefs = draftStore.loadRecentAgentBriefs(),
                onDraftChanged = draftStore::saveAgentTaskDraft,
                onRecentBriefsChanged = draftStore::saveRecentAgentBriefs,
            )
        }
    }
}

@Composable
private fun PocketForgeApp(
    initialDraft: AgentTaskDraft = DefaultAgentTaskDraft,
    initialRecentBriefs: List<AgentTaskDraft> = emptyList(),
    onDraftChanged: (AgentTaskDraft) -> Unit = {},
    onRecentBriefsChanged: (List<AgentTaskDraft>) -> Unit = {},
) {
    var selectedTab by remember { mutableStateOf(WorkbenchTab.Today) }
    var selectedFilter by remember { mutableStateOf("Mobile") }
    var selectedTheme by remember { mutableStateOf(ForgeGreen) }
    var taskTitle by remember { mutableStateOf(initialDraft.title) }
    var taskGoal by remember { mutableStateOf(initialDraft.goal) }
    var taskRepo by remember { mutableStateOf(initialDraft.repo) }
    var taskConstraints by remember { mutableStateOf(initialDraft.constraints) }
    var taskOutput by remember { mutableStateOf(initialDraft.output) }
    var recentBriefs by remember { mutableStateOf(initialRecentBriefs) }

    fun currentDraft(
        title: String = taskTitle,
        goal: String = taskGoal,
        repo: String = taskRepo,
        constraints: String = taskConstraints,
        output: String = taskOutput,
    ): AgentTaskDraft {
        return AgentTaskDraft(
            title = title,
            goal = goal,
            repo = repo,
            constraints = constraints,
            output = output,
        )
    }

    fun saveDraft(draft: AgentTaskDraft = currentDraft()) {
        onDraftChanged(draft)
    }

    fun restoreDraft(draft: AgentTaskDraft) {
        taskTitle = draft.title
        taskGoal = draft.goal
        taskRepo = draft.repo
        taskConstraints = draft.constraints
        taskOutput = draft.output
        saveDraft(draft)
    }

    fun saveCurrentBrief() {
        val draft = currentDraft()
        val updatedBriefs = (listOf(draft) + recentBriefs.filterNot { it.isSameBriefAs(draft) })
            .take(MAX_RECENT_BRIEFS)
        recentBriefs = updatedBriefs
        onRecentBriefsChanged(updatedBriefs)
        saveDraft(draft)
    }

    fun clearDraft() {
        restoreDraft(EmptyAgentTaskDraft)
    }

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

                        WorkbenchTab.Build -> BlueprintScreen(
                            taskTitle = taskTitle,
                            onTaskTitleChange = {
                                taskTitle = it
                                saveDraft(currentDraft(title = it))
                            },
                            taskGoal = taskGoal,
                            onTaskGoalChange = {
                                taskGoal = it
                                saveDraft(currentDraft(goal = it))
                            },
                            taskRepo = taskRepo,
                            onTaskRepoChange = {
                                taskRepo = it
                                saveDraft(currentDraft(repo = it))
                            },
                            taskConstraints = taskConstraints,
                            onTaskConstraintsChange = {
                                taskConstraints = it
                                saveDraft(currentDraft(constraints = it))
                            },
                            taskOutput = taskOutput,
                            onTaskOutputChange = {
                                taskOutput = it
                                saveDraft(currentDraft(output = it))
                            },
                            recentBriefs = recentBriefs,
                            onSaveBrief = ::saveCurrentBrief,
                            onSelectRecentBrief = ::restoreDraft,
                            onClearDraft = ::clearDraft,
                        )
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
        title = "Code from your phone.",
        body = "Turn a loose note into a local agent task you can inspect, edit, run, and iterate toward on-device.",
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
        detail = "Local sandbox first, optional providers later",
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
private fun BlueprintScreen(
    taskTitle: String,
    onTaskTitleChange: (String) -> Unit,
    taskGoal: String,
    onTaskGoalChange: (String) -> Unit,
    taskRepo: String,
    onTaskRepoChange: (String) -> Unit,
    taskConstraints: String,
    onTaskConstraintsChange: (String) -> Unit,
    taskOutput: String,
    onTaskOutputChange: (String) -> Unit,
    recentBriefs: List<AgentTaskDraft>,
    onSaveBrief: () -> Unit,
    onSelectRecentBrief: (AgentTaskDraft) -> Unit,
    onClearDraft: () -> Unit,
) {
    DarkBlueprintPanel()

    DraftSavedRow()

    RecentBriefsPanel(
        recentBriefs = recentBriefs,
        onSaveBrief = onSaveBrief,
        onSelectBrief = onSelectRecentBrief,
        onClearDraft = onClearDraft,
    )

    AgentTaskComposer(
        title = taskTitle,
        onTitleChange = onTaskTitleChange,
        goal = taskGoal,
        onGoalChange = onTaskGoalChange,
        repo = taskRepo,
        onRepoChange = onTaskRepoChange,
        constraints = taskConstraints,
        onConstraintsChange = onTaskConstraintsChange,
        output = taskOutput,
        onOutputChange = onTaskOutputChange,
    )

    AgentBriefCard(
        title = taskTitle,
        goal = taskGoal,
        repo = taskRepo,
        constraints = taskConstraints,
        output = taskOutput,
    )

    RunPlanPreview(
        title = taskTitle,
        goal = taskGoal,
        repo = taskRepo,
        constraints = taskConstraints,
        output = taskOutput,
        onSaveAsBrief = onSaveBrief,
    )

    ProviderRouterPanel()

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
private fun RecentBriefsPanel(
    recentBriefs: List<AgentTaskDraft>,
    onSaveBrief: () -> Unit,
    onSelectBrief: (AgentTaskDraft) -> Unit,
    onClearDraft: () -> Unit,
) {
    FeatureCard(
        label = "Recent briefs",
        title = "Pocket memory for local builds.",
        body = "Save a spark here, then restore it when the phone-side sandbox is ready for another pass.",
        color = ForgeBlue,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    modifier = Modifier.weight(1f),
                    onClick = onSaveBrief,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ForgeInk,
                        contentColor = ForgePaper,
                    ),
                    shape = MaterialTheme.shapes.medium,
                ) {
                    Text(
                        text = "Save brief",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                    )
                }
                TextButton(
                    modifier = Modifier.weight(1f),
                    onClick = onClearDraft,
                ) {
                    Text(
                        text = "New brief",
                        color = ForgeRust,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                    )
                }
            }

            if (recentBriefs.isEmpty()) {
                Text(
                    text = "No saved briefs yet.",
                    color = ForgeMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            } else {
                recentBriefs.forEachIndexed { index, brief ->
                    RecentBriefRow(
                        brief = brief,
                        index = index,
                        onClick = { onSelectBrief(brief) },
                    )
                }
            }
        }
    }
}

@Composable
private fun RecentBriefRow(
    brief: AgentTaskDraft,
    index: Int,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        color = ForgePaper,
        border = BorderStroke(2.dp, ForgeInk),
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(ForgeMint),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = (index + 1).toString(),
                    color = ForgeInk,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = brief.title.ifBlank { "Untitled local task" },
                    color = ForgeInk,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = brief.goal.ifBlank { "Restore this brief and sharpen the next sandbox run." },
                    color = ForgeMuted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            StatusChip(text = "Load", color = ForgeTeal)
        }
    }
}

@Composable
private fun AgentTaskComposer(
    title: String,
    onTitleChange: (String) -> Unit,
    goal: String,
    onGoalChange: (String) -> Unit,
    repo: String,
    onRepoChange: (String) -> Unit,
    constraints: String,
    onConstraintsChange: (String) -> Unit,
    output: String,
    onOutputChange: (String) -> Unit,
) {
    FeatureCard(
        label = "Agent task",
        title = "Brief a coding agent from your phone.",
        body = "This is local-only for now. Edit the fields and PocketForge shapes a deterministic handoff brief below.",
        color = ForgeGold,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
            ForgeTextField(label = "Title", value = title, onValueChange = onTitleChange, singleLine = true)
            ForgeTextField(label = "Goal", value = goal, onValueChange = onGoalChange, minLines = 3)
            ForgeTextField(label = "Repo / branch", value = repo, onValueChange = onRepoChange, singleLine = true)
            ForgeTextField(label = "Constraints", value = constraints, onValueChange = onConstraintsChange, minLines = 3)
            ForgeTextField(label = "Desired output", value = output, onValueChange = onOutputChange, minLines = 2)
        }
    }
}

@Composable
private fun ForgeTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    singleLine: Boolean = false,
    minLines: Int = 1,
) {
    OutlinedTextField(
        modifier = Modifier.fillMaxWidth(),
        value = value,
        onValueChange = onValueChange,
        label = {
            Text(
                text = label,
                color = ForgeInk,
                fontWeight = FontWeight.ExtraBold,
            )
        },
        singleLine = singleLine,
        minLines = minLines,
        shape = MaterialTheme.shapes.medium,
    )
}

@Composable
private fun AgentBriefCard(
    title: String,
    goal: String,
    repo: String,
    constraints: String,
    output: String,
) {
    val brief = remember(title, goal, repo, constraints, output) {
        buildAgentBrief(
            title = title,
            goal = goal,
            repo = repo,
            constraints = constraints,
            output = output,
        )
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = ForgeInk,
        border = BorderStroke(2.dp, ForgePaper),
    ) {
        Column(
            modifier = Modifier.padding(15.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            LabelText(text = "Generated brief", dark = false)
            Text(
                text = brief,
                color = ForgePaper,
                fontSize = 12.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun ProviderRouterPanel() {
    FeatureCard(
        label = "Provider router",
        title = "Local agent first, clean integrations later.",
        body = "Future workflows should run in an on-device sandbox where you can inspect, edit, and execute code locally. Provider keys and cloud CI stay optional integration points with clear costs and limits.",
        color = ForgeMint,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PlanCell(modifier = Modifier.weight(1f), label = "Primary", value = "On-device")
            PlanCell(modifier = Modifier.weight(1f), label = "Mode", value = "Saved draft")
        }
    }
}

@Composable
private fun RunPlanPreview(
    title: String,
    goal: String,
    repo: String,
    constraints: String,
    output: String,
    onSaveAsBrief: () -> Unit,
) {
    val clipboardManager = LocalClipboardManager.current
    var manualReady by remember(title, goal, repo, constraints, output) { mutableStateOf(false) }
    var actionMessage by remember(title, goal, repo, constraints, output) { mutableStateOf("") }
    val runPlan = remember(title, goal, repo, constraints, output) {
        buildRunPlanPreview(
            title = title,
            goal = goal,
            repo = repo,
            constraints = constraints,
            output = output,
        )
    }
    val readinessLabel = if (manualReady && runPlan.missingDetails.isEmpty()) "Ready" else runPlan.readinessLabel
    val readinessColor = if (manualReady && runPlan.missingDetails.isEmpty()) ForgeTeal else runPlan.readinessColor

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = ForgeInk,
        border = BorderStroke(2.dp, ForgePaper),
    ) {
        Column(
            modifier = Modifier.padding(15.dp),
            verticalArrangement = Arrangement.spacedBy(11.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    LabelText(text = "Run plan preview", dark = false)
                }
                StatusChip(text = readinessLabel, color = readinessColor)
            }
            Text(
                text = "Phone sandbox draft",
                color = ForgePaper,
                fontSize = 23.sp,
                lineHeight = 25.sp,
                fontWeight = FontWeight.ExtraBold,
            )
            Text(
                text = "Preview only. This turns the brief into local workflow steps; it does not execute code yet.",
                color = ForgePaper.copy(alpha = 0.72f),
                fontSize = 12.sp,
                lineHeight = 17.sp,
                fontWeight = FontWeight.SemiBold,
            )
            if (runPlan.missingDetails.isNotEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    color = ForgePaper.copy(alpha = 0.08f),
                    border = BorderStroke(1.dp, ForgeGold.copy(alpha = 0.55f)),
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = "Needs detail",
                            color = ForgeGold,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                        )
                        Text(
                            text = "Add ${runPlan.missingDetails.joinToString()} before treating this as actionable phone work.",
                            color = ForgePaper.copy(alpha = 0.74f),
                            fontSize = 10.sp,
                            lineHeight = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PlanCell(modifier = Modifier.weight(1f), label = "Target", value = runPlan.target)
                PlanCell(modifier = Modifier.weight(1f), label = "Likely files", value = runPlan.likelyFiles)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PlanCell(modifier = Modifier.weight(1f), label = "Checks", value = runPlan.checks)
                PlanCell(modifier = Modifier.weight(1f), label = "Result", value = runPlan.result)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CompactPlanAction(
                    modifier = Modifier.weight(1f),
                    text = "Copy plan",
                    enabled = true,
                    onClick = {
                        clipboardManager.setText(AnnotatedString(buildRunPlanCopy(runPlan, readinessLabel)))
                        actionMessage = "Plan copied. Preview only; no code ran."
                    },
                )
                CompactPlanAction(
                    modifier = Modifier.weight(1f),
                    text = "Save as brief",
                    enabled = true,
                    onClick = {
                        onSaveAsBrief()
                        actionMessage = "Saved to Recent briefs. Matching briefs update the existing row."
                    },
                )
                CompactPlanAction(
                    modifier = Modifier.weight(1f),
                    text = if (readinessLabel == "Ready") "Ready" else "Mark ready",
                    enabled = runPlan.missingDetails.isEmpty(),
                    onClick = {
                        manualReady = true
                        actionMessage = "Marked ready for local sandbox planning."
                    },
                )
            }
            if (actionMessage.isNotBlank()) {
                Text(
                    text = actionMessage,
                    color = ForgePaper.copy(alpha = 0.72f),
                    fontSize = 10.sp,
                    lineHeight = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            runPlan.steps.forEachIndexed { index, step ->
                RunPlanStepRow(
                    number = index + 1,
                    step = step,
                )
            }
        }
    }
}

@Composable
private fun RunPlanStepRow(
    number: Int,
    step: RunPlanStep,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(29.dp)
                .clip(CircleShape)
                .background(step.color),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = number.toString(),
                color = ForgeInk,
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = step.title,
                color = ForgePaper,
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = step.detail,
                color = ForgePaper.copy(alpha = 0.68f),
                fontSize = 10.sp,
                lineHeight = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Box(
            modifier = Modifier.size(width = 58.dp, height = 29.dp),
            contentAlignment = Alignment.CenterEnd,
        ) {
            StatusChip(text = step.state, color = step.color)
        }
    }
}

@Composable
private fun CompactPlanAction(
    modifier: Modifier = Modifier,
    text: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    TextButton(
        modifier = modifier,
        enabled = enabled,
        onClick = onClick,
    ) {
        Text(
            text = text,
            color = if (enabled) ForgeGold else ForgePaper.copy(alpha = 0.38f),
            fontSize = 10.sp,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private fun buildAgentBrief(
    title: String,
    goal: String,
    repo: String,
    constraints: String,
    output: String,
): String {
    val normalizedTitle = title.ifBlank { "Untitled coding task" }
    val normalizedGoal = goal.ifBlank { "Clarify the desired change before editing code." }
    val normalizedRepo = repo.ifBlank { "Current repository and branch" }
    val normalizedConstraints = constraints.ifBlank { "Respect existing project conventions and avoid unrelated changes." }
    val normalizedOutput = output.ifBlank { "A concise implementation summary and validation notes." }

    return """
        Task: $normalizedTitle

        Context: Work in $normalizedRepo.

        Goal: $normalizedGoal

        Constraints: $normalizedConstraints

        Expected output: $normalizedOutput

        Agent checklist:
        1. Inspect the current branch and relevant files.
        2. Make the smallest complete code change.
        3. Validate with lightweight checks or CI as configured.
        4. Summarize touched files, risks, and next action.
    """.trimIndent()
}

private fun buildRunPlanPreview(
    title: String,
    goal: String,
    repo: String,
    constraints: String,
    output: String,
): RunPlanPreviewModel {
    val missingDetails = findMissingRunPlanDetails(
        title = title,
        goal = goal,
        repo = repo,
        output = output,
    )
    val normalizedTitle = title.ifBlank { "Untitled local task" }
    val normalizedGoal = goal.ifBlank { "Clarify the change before editing code." }
    val normalizedRepo = repo.ifBlank { "Current project" }
    val normalizedConstraints = constraints.ifBlank { "Use existing patterns and keep edits scoped." }
    val normalizedOutput = output.ifBlank { "Summary, validation notes, and next action." }
    val likelyFiles = inferLikelyFiles(normalizedGoal, normalizedOutput)
    val checks = inferLightweightChecks(normalizedConstraints)
    val readinessLabel = when {
        missingDetails.isNotEmpty() -> "Needs detail"
        constraints.isBlank() -> "Draft"
        else -> "Draft"
    }
    val readinessColor = if (missingDetails.isNotEmpty()) ForgeGold else ForgeBlue

    return RunPlanPreviewModel(
        target = normalizedRepo.takePreviewWords(maxWords = 4),
        likelyFiles = likelyFiles,
        checks = checks,
        result = "User summary",
        readinessLabel = readinessLabel,
        readinessColor = readinessColor,
        missingDetails = missingDetails,
        steps = listOf(
            RunPlanStep(
                title = "Inspect local repo",
                detail = "Open $normalizedRepo, read status, and find files tied to ${normalizedTitle.takePreviewWords(maxWords = 5)}.",
                state = "Local",
                color = ForgeMint,
            ),
            RunPlanStep(
                title = "Choose likely files",
                detail = "Start with $likelyFiles, then narrow by imports, UI state, and existing helpers.",
                state = "Map",
                color = ForgeBlue,
            ),
            RunPlanStep(
                title = "Make smallest edit",
                detail = normalizedGoal.takePreviewWords(maxWords = 14),
                state = "Patch",
                color = ForgeGold,
            ),
            RunPlanStep(
                title = "Run phone-safe checks",
                detail = checks,
                state = "Check",
                color = ForgeTeal,
            ),
            RunPlanStep(
                title = "Summarize for you",
                detail = normalizedOutput.takePreviewWords(maxWords = 14),
                state = "Review",
                color = ForgePeach,
            ),
        ),
    )
}

private fun buildRunPlanCopy(
    runPlan: RunPlanPreviewModel,
    readinessLabel: String,
): String {
    val missingDetails = if (runPlan.missingDetails.isEmpty()) {
        "None"
    } else {
        runPlan.missingDetails.joinToString()
    }

    return buildString {
        appendLine("PocketForge Run Plan")
        appendLine("Status: $readinessLabel")
        appendLine("Mode: Preview only, local phone sandbox planning")
        appendLine()
        appendLine("Target: ${runPlan.target}")
        appendLine("Likely files: ${runPlan.likelyFiles}")
        appendLine("Checks: ${runPlan.checks}")
        appendLine("Result: ${runPlan.result}")
        appendLine("Missing detail: $missingDetails")
        appendLine()
        appendLine("Steps:")
        runPlan.steps.forEachIndexed { index, step ->
            appendLine("${index + 1}. ${step.title} [${step.state}]")
            appendLine("   ${step.detail}")
        }
    }.trim()
}

@Composable
private fun PipelineScreen() {
    FeatureCard(
        label = "Optional cloud build",
        title = "APK artifact runs in GitHub Actions.",
        body = "Cloud CI is a release helper while the core coding workflow moves toward local phone-side sandboxes.",
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
                text = "Ready to brief.",
                color = ForgePaper,
                fontSize = 30.sp,
                lineHeight = 31.sp,
                fontWeight = FontWeight.ExtraBold,
            )
            Text(
                text = "Draft a phone-side coding task now; keep cloud services as optional handoff points.",
                color = ForgePaper.copy(alpha = 0.72f),
                fontSize = 12.sp,
                lineHeight = 17.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun DraftSavedRow() {
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
                    text = "Local draft persists",
                    color = ForgeInk,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                )
                Text(
                    text = "Agent task fields save locally on this device.",
                    color = ForgeMuted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            StatusChip(text = "Saved", color = ForgeTeal)
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

private data class AgentTaskDraft(
    val title: String,
    val goal: String,
    val repo: String,
    val constraints: String,
    val output: String,
)

private data class RunPlanPreviewModel(
    val target: String,
    val likelyFiles: String,
    val checks: String,
    val result: String,
    val readinessLabel: String,
    val readinessColor: Color,
    val missingDetails: List<String>,
    val steps: List<RunPlanStep>,
)

private data class RunPlanStep(
    val title: String,
    val detail: String,
    val state: String,
    val color: Color,
)

private fun AgentTaskDraft.isSameBriefAs(other: AgentTaskDraft): Boolean {
    return title == other.title &&
        goal == other.goal &&
        repo == other.repo &&
        constraints == other.constraints &&
        output == other.output
}

private fun findMissingRunPlanDetails(
    title: String,
    goal: String,
    repo: String,
    output: String,
): List<String> {
    return buildList {
        if (title.isBlank()) add("a task title")
        if (goal.trim().length < 24) add("a clearer goal")
        if (repo.isBlank()) add("repo or branch")
        if (output.trim().length < 12) add("desired output")
    }
}

private fun inferLikelyFiles(goal: String, output: String): String {
    val searchableText = "$goal $output".lowercase()

    return when {
        searchableText.contains("compose") ||
            searchableText.contains("ui") ||
            searchableText.contains("screen") ||
            searchableText.contains("tab") -> "MainActivity.kt"
        searchableText.contains("apk") ||
            searchableText.contains("ci") ||
            searchableText.contains("workflow") ||
            searchableText.contains("actions") -> ".github/workflows"
        searchableText.contains("readme") ||
            searchableText.contains("docs") ||
            searchableText.contains("plan") -> "README/plan"
        searchableText.contains("gradle") ||
            searchableText.contains("dependency") -> "Gradle files"
        else -> "repo matches"
    }
}

private fun inferLightweightChecks(constraints: String): String {
    val searchableText = constraints.lowercase()

    return if (
        searchableText.contains("no local android") ||
        searchableText.contains("no android builds") ||
        searchableText.contains("no local builds")
    ) {
        "Diff + static scan"
    } else {
        "Available local checks"
    }
}

private fun String.takePreviewWords(maxWords: Int): String {
    val words = trim().split(Regex("\\s+")).filter { it.isNotBlank() }
    if (words.isEmpty()) return ""
    return if (words.size <= maxWords) {
        words.joinToString(" ")
    } else {
        words.take(maxWords).joinToString(" ") + "..."
    }
}

private val DefaultAgentTaskDraft = AgentTaskDraft(
    title = "Add offline idea capture",
    goal = "Let me capture a coding idea on my phone, turn it into a scoped task, and hand it to a local agent later.",
    repo = "PocketForge / phase-1-ui",
    constraints = "Local-first. No local Android builds. Use GitHub Actions only as optional APK CI. Keep API providers legitimate.",
    output = "Implementation plan, touched files, validation steps, and a commit-ready branch.",
)

private val EmptyAgentTaskDraft = AgentTaskDraft(
    title = "",
    goal = "",
    repo = "",
    constraints = "",
    output = "",
)

private const val AGENT_DRAFT_PREFS = "agent_task_draft"
private const val PREF_TASK_TITLE = "task_title"
private const val PREF_TASK_GOAL = "task_goal"
private const val PREF_TASK_REPO = "task_repo"
private const val PREF_TASK_CONSTRAINTS = "task_constraints"
private const val PREF_TASK_OUTPUT = "task_output"
private const val PREF_RECENT_BRIEFS = "recent_briefs"
private const val MAX_RECENT_BRIEFS = 5
private const val JSON_TITLE = "title"
private const val JSON_GOAL = "goal"
private const val JSON_REPO = "repo"
private const val JSON_CONSTRAINTS = "constraints"
private const val JSON_OUTPUT = "output"

private fun SharedPreferences.loadAgentTaskDraft(): AgentTaskDraft {
    return AgentTaskDraft(
        title = getString(PREF_TASK_TITLE, DefaultAgentTaskDraft.title) ?: DefaultAgentTaskDraft.title,
        goal = getString(PREF_TASK_GOAL, DefaultAgentTaskDraft.goal) ?: DefaultAgentTaskDraft.goal,
        repo = getString(PREF_TASK_REPO, DefaultAgentTaskDraft.repo) ?: DefaultAgentTaskDraft.repo,
        constraints = getString(PREF_TASK_CONSTRAINTS, DefaultAgentTaskDraft.constraints)
            ?: DefaultAgentTaskDraft.constraints,
        output = getString(PREF_TASK_OUTPUT, DefaultAgentTaskDraft.output) ?: DefaultAgentTaskDraft.output,
    )
}

private fun SharedPreferences.saveAgentTaskDraft(draft: AgentTaskDraft) {
    edit()
        .putString(PREF_TASK_TITLE, draft.title)
        .putString(PREF_TASK_GOAL, draft.goal)
        .putString(PREF_TASK_REPO, draft.repo)
        .putString(PREF_TASK_CONSTRAINTS, draft.constraints)
        .putString(PREF_TASK_OUTPUT, draft.output)
        .apply()
}

private fun SharedPreferences.loadRecentAgentBriefs(): List<AgentTaskDraft> {
    val storedBriefs = getString(PREF_RECENT_BRIEFS, null) ?: return emptyList()

    return try {
        val jsonBriefs = JSONArray(storedBriefs)
        buildList {
            for (index in 0 until minOf(jsonBriefs.length(), MAX_RECENT_BRIEFS)) {
                val brief = jsonBriefs.optJSONObject(index) ?: continue
                add(
                    AgentTaskDraft(
                        title = brief.optString(JSON_TITLE),
                        goal = brief.optString(JSON_GOAL),
                        repo = brief.optString(JSON_REPO),
                        constraints = brief.optString(JSON_CONSTRAINTS),
                        output = brief.optString(JSON_OUTPUT),
                    ),
                )
            }
        }
    } catch (ignored: JSONException) {
        emptyList()
    }
}

private fun SharedPreferences.saveRecentAgentBriefs(briefs: List<AgentTaskDraft>) {
    val jsonBriefs = JSONArray()
    briefs.take(MAX_RECENT_BRIEFS).forEach { brief ->
        jsonBriefs.put(
            JSONObject()
                .put(JSON_TITLE, brief.title)
                .put(JSON_GOAL, brief.goal)
                .put(JSON_REPO, brief.repo)
                .put(JSON_CONSTRAINTS, brief.constraints)
                .put(JSON_OUTPUT, brief.output),
        )
    }

    edit()
        .putString(PREF_RECENT_BRIEFS, jsonBriefs.toString())
        .apply()
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
