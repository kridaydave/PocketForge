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
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
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
    var selectedTab by remember { mutableStateOf(WorkbenchTab.Chat) }
    var selectedRepoIndex by remember { mutableStateOf(0) }
    var selectedBranch by remember { mutableStateOf(MockRepos.first().branches.first()) }
    var selectedFilePath by remember { mutableStateOf(MockFiles.first().path) }
    var taskTitle by remember { mutableStateOf(initialDraft.title) }
    var taskGoal by remember { mutableStateOf(initialDraft.goal) }
    var taskRepo by remember { mutableStateOf(initialDraft.repo) }
    var taskConstraints by remember { mutableStateOf(initialDraft.constraints) }
    var taskOutput by remember { mutableStateOf(initialDraft.output) }
    var taskMarkedReady by remember { mutableStateOf(initialDraft.markedReady) }
    var recentBriefs by remember { mutableStateOf(initialRecentBriefs) }
    var loadedQueuedBrief by remember {
        mutableStateOf(initialRecentBriefs.firstOrNull { it.isSameBriefAs(initialDraft) })
    }

    fun currentDraft(
        title: String = taskTitle,
        goal: String = taskGoal,
        repo: String = taskRepo,
        constraints: String = taskConstraints,
        output: String = taskOutput,
        markedReady: Boolean = taskMarkedReady,
    ): AgentTaskDraft {
        return AgentTaskDraft(
            title = title,
            goal = goal,
            repo = repo,
            constraints = constraints,
            output = output,
            markedReady = markedReady,
        )
    }

    fun saveDraft(draft: AgentTaskDraft = currentDraft()) {
        onDraftChanged(draft)
    }

    fun restoreDraft(
        draft: AgentTaskDraft,
        queuedBrief: AgentTaskDraft? = draft,
    ) {
        taskTitle = draft.title
        taskGoal = draft.goal
        taskRepo = draft.repo
        taskConstraints = draft.constraints
        taskOutput = draft.output
        taskMarkedReady = draft.markedReady
        loadedQueuedBrief = queuedBrief
        saveDraft(draft)
    }

    fun saveCurrentBrief() {
        val draft = currentDraft()
        val loadedBrief = loadedQueuedBrief
        val loadedIndex = loadedBrief?.let { loaded ->
            recentBriefs.indexOfFirst { it.isSameBriefAs(loaded) }
        } ?: -1
        val updatedBriefs = if (loadedIndex >= 0) {
            recentBriefs.mapIndexed { index, brief ->
                if (index == loadedIndex) draft else brief
            }.filterIndexed { index, brief ->
                index == loadedIndex || !brief.isSameBriefAs(draft)
            }.take(MAX_RECENT_BRIEFS)
        } else {
            (listOf(draft) + recentBriefs.filterNot { it.isSameBriefAs(draft) })
                .take(MAX_RECENT_BRIEFS)
        }
        recentBriefs = updatedBriefs
        loadedQueuedBrief = draft
        onRecentBriefsChanged(updatedBriefs)
        saveDraft(draft)
    }

    fun clearDraft() {
        restoreDraft(EmptyAgentTaskDraft, queuedBrief = null)
    }

    fun deleteRecentBrief(briefToDelete: AgentTaskDraft) {
        val updatedBriefs = recentBriefs.filterNot { it.isSameBriefAs(briefToDelete) }
        recentBriefs = updatedBriefs
        onRecentBriefsChanged(updatedBriefs)

        val isLoadedBrief = loadedQueuedBrief?.isSameBriefAs(briefToDelete) == true
        if (isLoadedBrief || briefToDelete.isSameBriefAs(currentDraft())) {
            clearDraft()
        }
    }

    fun updateCurrentReadyState() {
        val readyDraft = currentDraft(markedReady = true)
        taskMarkedReady = true
        saveDraft(readyDraft)

        val loadedBrief = loadedQueuedBrief
        val loadedIndex = loadedBrief?.let { loaded ->
            recentBriefs.indexOfFirst { it.isSameBriefAs(loaded) }
        } ?: -1
        val updatedBriefs = if (loadedIndex >= 0) {
            recentBriefs.mapIndexed { index, brief ->
                if (index == loadedIndex) readyDraft else brief
            }.filterIndexed { index, brief ->
                index == loadedIndex || !brief.isSameBriefAs(readyDraft)
            }
        } else {
            recentBriefs.map { brief ->
                if (brief.isSameBriefAs(readyDraft)) readyDraft else brief
            }
        }
        recentBriefs = updatedBriefs
        loadedQueuedBrief = readyDraft
        onRecentBriefsChanged(updatedBriefs)
    }

    PocketForgeTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = ForgeCanvas,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding(),
            ) {
                SessionTopBar(selectedTab = selectedTab)

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 18.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                ) {
                    when (selectedTab) {
                        WorkbenchTab.Chat -> AgentChatScreen(
                            onOpenBrief = { selectedTab = WorkbenchTab.Build },
                        )

                        WorkbenchTab.Repos -> ReposScreen(
                            selectedRepoIndex = selectedRepoIndex,
                            selectedBranch = selectedBranch,
                            onRepoSelected = { repoIndex ->
                                selectedRepoIndex = repoIndex
                                selectedBranch = MockRepos[repoIndex].branches.first()
                            },
                            onBranchSelected = { selectedBranch = it },
                        )

                        WorkbenchTab.Files -> FilesScreen(
                            selectedFilePath = selectedFilePath,
                            onFileSelected = { selectedFilePath = it },
                        )

                        WorkbenchTab.Build -> BlueprintScreen(
                            taskTitle = taskTitle,
                            onTaskTitleChange = {
                                taskTitle = it
                                taskMarkedReady = false
                                saveDraft(currentDraft(title = it, markedReady = false))
                            },
                            taskGoal = taskGoal,
                            onTaskGoalChange = {
                                taskGoal = it
                                taskMarkedReady = false
                                saveDraft(currentDraft(goal = it, markedReady = false))
                            },
                            taskRepo = taskRepo,
                            onTaskRepoChange = {
                                taskRepo = it
                                taskMarkedReady = false
                                saveDraft(currentDraft(repo = it, markedReady = false))
                            },
                            taskConstraints = taskConstraints,
                            onTaskConstraintsChange = {
                                taskConstraints = it
                                taskMarkedReady = false
                                saveDraft(currentDraft(constraints = it, markedReady = false))
                            },
                            taskOutput = taskOutput,
                            onTaskOutputChange = {
                                taskOutput = it
                                taskMarkedReady = false
                                saveDraft(currentDraft(output = it, markedReady = false))
                            },
                            currentBrief = currentDraft(),
                            loadedQueuedBrief = loadedQueuedBrief,
                            recentBriefs = recentBriefs,
                            onSaveBrief = ::saveCurrentBrief,
                            onSelectRecentBrief = ::restoreDraft,
                            onDeleteRecentBrief = ::deleteRecentBrief,
                            onClearDraft = ::clearDraft,
                            onMarkReady = ::updateCurrentReadyState,
                        )

                        WorkbenchTab.Settings -> SettingsScreen()
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
private fun AgentChatScreen(onOpenBrief: () -> Unit) {
    ProjectContextStrip()

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "How should PocketForge change this local repo?",
            color = ForgeInk,
            fontSize = 32.sp,
            lineHeight = 34.sp,
            fontWeight = FontWeight.Medium,
        )
        Text(
            modifier = Modifier.fillMaxWidth(0.9f),
            text = "Chat is the home surface. Repos, files, checks, and permissions stay tucked into the composer and manifest until the local sandbox exists.",
            color = ForgeMuted,
            fontSize = 13.sp,
            lineHeight = 19.sp,
            fontWeight = FontWeight.Medium,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            StatusChip(text = "LOCAL", color = ForgeSlate)
            StatusChip(text = "PREVIEW", color = ForgeRust)
            StatusChip(text = "NO RUN", color = ForgeGold)
        }
    }

    ChatMessageBubble(
        speaker = "You",
        body = "Make the app feel like I can inspect a repo and hand a scoped task to a phone-side agent, without pretending it ran.",
        mine = true,
    )
    ChatMessageBubble(
        speaker = "PocketForge",
        body = "I can draft a local plan manifest, name likely files, and stop at a permission gate before any sandbox action.",
        mine = false,
    )
    ToolActivityPanel()
    ChatMessageBubble(
        speaker = "PocketForge",
        body = "Next handoff: promote this chat into a Build brief with repo, constraints, checks, blocked details, and a clear stop point.",
        mine = false,
    )

    ChatComposer(onOpenBrief = onOpenBrief)
}

@Composable
private fun SessionTopBar(selectedTab: WorkbenchTab) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        StrataMark()
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "PocketForge",
                color = ForgeInk,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "LOCAL / sandbox preview",
                color = ForgeMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
            )
        }
        StatusChip(text = selectedTab.label.uppercase(), color = ForgeRust)
    }
}

@Composable
private fun StrataMark() {
    Column(
        modifier = Modifier.size(width = 30.dp, height = 24.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Box(
            modifier = Modifier
                .size(width = 25.dp, height = 3.dp)
                .background(ForgeInk),
        )
        Box(
            modifier = Modifier
                .size(width = 30.dp, height = 3.dp)
                .background(ForgeInk),
        )
        Box(
            modifier = Modifier
                .size(width = 22.dp, height = 3.dp)
                .background(ForgeRust),
        )
        Box(
            modifier = Modifier
                .size(width = 28.dp, height = 3.dp)
                .background(ForgeInk),
        )
    }
}

@Composable
private fun ProjectContextStrip() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = ForgePaper.copy(alpha = 0.62f),
        border = BorderStroke(1.dp, ForgeLine),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(ForgeRust),
            )
            Text(
                modifier = Modifier.weight(1f),
                text = "manifest: PocketForge / phase-1-ui",
                color = ForgeInk,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "PREVIEW CONTEXT",
                color = ForgeMuted,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
private fun ChatComposer(onOpenBrief: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = ForgePaper,
        border = BorderStroke(1.dp, ForgeLine),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                LabelText(text = "Local plan", dark = true)
                Box(modifier = Modifier.weight(1f))
                StatusChip(text = "NO RUN", color = ForgeRust)
            }
            Text(
                text = "Ask PocketForge to draft a manifest...",
                color = ForgeMuted,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.Medium,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ComposerChip(text = "+")
                ComposerChip(text = "repo")
                ComposerChip(text = "files")
                ComposerChip(text = "style: local")
                Button(
                    modifier = Modifier.weight(1f),
                    onClick = onOpenBrief,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ForgeRust,
                        contentColor = ForgePaper,
                    ),
                    shape = MaterialTheme.shapes.medium,
                ) {
                    Text(
                        text = "Build",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                    )
                }
            }
            Text(
                text = "Composer controls are preview affordances: attachments, context, and permission gate only.",
                color = ForgeMuted.copy(alpha = 0.78f),
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
private fun ComposerChip(text: String) {
    Surface(
        shape = CircleShape,
        color = ForgeCanvas,
        border = BorderStroke(1.dp, ForgeLine),
    ) {
        Text(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            text = text,
            color = ForgeInk,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
    }
}

@Composable
private fun ChatMessageBubble(
    speaker: String,
    body: String,
    mine: Boolean,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start,
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(if (mine) 0.86f else 0.96f),
            shape = RoundedCornerShape(18.dp),
            color = if (mine) ForgeInk else ForgePaper.copy(alpha = 0.72f),
            border = BorderStroke(1.dp, if (mine) ForgeInk else ForgeLine),
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 13.dp, vertical = 11.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Text(
                    text = speaker,
                    color = if (mine) ForgePaper.copy(alpha = 0.68f) else ForgeMuted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = body,
                    color = if (mine) ForgePaper else ForgeInk,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}

@Composable
private fun ToolActivityPanel() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = ForgePaper.copy(alpha = 0.68f),
        border = BorderStroke(1.dp, ForgeLine),
    ) {
        Column(
            modifier = Modifier.padding(13.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    modifier = Modifier.weight(1f),
                    text = "Proof object: local plan manifest",
                    color = ForgeInk,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                StatusChip(text = "NO RUN", color = ForgeRust)
            }
            ToolActivityRow(
                step = "Map repo context",
                detail = "Preview row only",
                state = "PREVIEW",
                color = ForgeSlate,
            )
            ToolActivityRow(
                step = "List likely files",
                detail = "Mock local plan, not file access",
                state = "MOCK",
                color = ForgeGold,
            )
            ToolActivityRow(
                step = "Prepare permission gate",
                detail = "No execution in Phase 1",
                state = "LOCAL PLAN",
                color = ForgeRust,
            )
            Text(
                text = "These rows describe a future local workflow. They do not claim that files were read, edited, or executed.",
                color = ForgeMuted,
                fontSize = 10.sp,
                lineHeight = 14.sp,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
private fun ToolActivityRow(
    step: String,
    detail: String,
    state: String,
    color: Color,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        Box(
            modifier = Modifier
                .size(9.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(color),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = step,
                color = ForgeInk,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = detail,
                color = ForgeMuted,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
            )
        }
        StatusChip(text = state, color = color)
    }
}

@Composable
private fun ReposScreen(
    selectedRepoIndex: Int,
    selectedBranch: String,
    onRepoSelected: (Int) -> Unit,
    onBranchSelected: (String) -> Unit,
) {
    val selectedRepo = MockRepos[selectedRepoIndex]

    SectionTitleBlock(title = "Repos", subtitle = "Local project picker")

    FeatureCard(
        label = "Selected workspace",
        title = selectedRepo.name,
        body = selectedRepo.detail,
        color = ForgeMint,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PlanCell(modifier = Modifier.weight(1f), label = "Branch", value = selectedBranch)
            PlanCell(modifier = Modifier.weight(1f), label = "Path", value = selectedRepo.path)
        }
    }

    SectionHeader(title = "On-device repos", action = "Mock data")
    MockRepos.forEachIndexed { index, repo ->
        RepoPickerRow(
            repo = repo,
            selected = index == selectedRepoIndex,
            onClick = { onRepoSelected(index) },
        )
    }

    RailCard(label = "Branch preview") {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            selectedRepo.branches.forEach { branch ->
                FilterChip(
                    text = branch,
                    selected = branch == selectedBranch,
                    onClick = { onBranchSelected(branch) },
                )
            }
        }
    }

    QueueRow(
        badge = "GH",
        title = "GitHub mirror stays optional",
        detail = "Phase 2 can add read-only sync; local sandbox remains the center.",
        state = "Later",
        color = ForgePeach,
    )
}

@Composable
private fun RepoPickerRow(
    repo: MockRepo,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        color = if (selected) ForgeInk else ForgePaper,
        border = BorderStroke(1.dp, if (selected) ForgeRust else ForgeLine),
    ) {
        Row(
            modifier = Modifier.padding(11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(repo.color),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = repo.initials,
                    color = ForgeInk,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = repo.name,
                    color = if (selected) ForgePaper else ForgeInk,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = repo.path,
                    color = if (selected) ForgePaper.copy(alpha = 0.68f) else ForgeMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            StatusChip(text = repo.state, color = if (selected) ForgeGold else ForgeInk)
        }
    }
}

@Composable
private fun FilesScreen(
    selectedFilePath: String,
    onFileSelected: (String) -> Unit,
) {
    val selectedFile = MockFiles.firstOrNull { it.path == selectedFilePath } ?: MockFiles.first()

    SectionTitleBlock(title = "Files", subtitle = "Inspect code from phone")

    FeatureCard(
        label = "File browser",
        title = "PocketForge / phase-1-ui",
        body = "Mock local tree for Phase 1. The surface should feel ready for real read-only browsing in Phase 2.",
        color = ForgeBlue,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            MockFiles.forEach { file ->
                FileBrowserRow(
                    file = file,
                    selected = file.path == selectedFile.path,
                    onClick = { onFileSelected(file.path) },
                )
            }
        }
    }

    FilePreviewPanel(file = selectedFile)

    QueueRow(
        badge = "RO",
        title = "Read-only first",
        detail = "Phase 2 should browse files before editing or running anything.",
        state = "Plan",
        color = ForgeMint,
    )
}

@Composable
private fun FileBrowserRow(
    file: MockFile,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        color = if (selected) ForgeInk else ForgePaper,
        border = BorderStroke(1.dp, if (selected) ForgeRust else ForgeLine),
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            Text(
                text = file.kind,
                color = if (selected) ForgeGold else ForgeRust,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = file.name,
                    color = if (selected) ForgePaper else ForgeInk,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = file.path,
                    color = if (selected) ForgePaper.copy(alpha = 0.62f) else ForgeMuted,
                    fontSize = 9.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            StatusChip(text = file.mode, color = if (selected) ForgeGold else ForgeInk)
        }
    }
}

@Composable
private fun FilePreviewPanel(file: MockFile) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = ForgeInk,
        border = BorderStroke(1.dp, ForgeLineLight),
    ) {
        Column(
            modifier = Modifier.padding(15.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    LabelText(text = "Preview viewer", dark = false)
                    Text(
                        text = file.name,
                        color = ForgePaper,
                        fontSize = 21.sp,
                        lineHeight = 23.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                StatusChip(text = "Read", color = ForgeGold)
            }
            Text(
                text = file.path,
                color = ForgePaper.copy(alpha = 0.58f),
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                color = ForgeGraphite,
                border = BorderStroke(1.dp, ForgeLineLight),
            ) {
                Text(
                    modifier = Modifier.padding(12.dp),
                    text = file.preview,
                    color = ForgePaper,
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
            Text(
                text = "Preview only. Editing, saving, and command execution are intentionally absent in Phase 1.",
                color = ForgePaper.copy(alpha = 0.64f),
                fontSize = 10.sp,
                lineHeight = 14.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun SettingsScreen() {
    SectionTitleBlock(title = "Settings", subtitle = "Preview-only controls")

    FeatureCard(
        label = "Local safety",
        title = "No secrets are stored yet.",
        body = "These sections reserve the shape for Phase 2 without adding credential persistence or real integrations.",
        color = ForgePeach,
    ) {
        ScoreBars(values = listOf(0.25f, 0.5f, 0.75f))
    }

    SettingsSection(
        label = "GitHub token",
        title = "Read-only repo access",
        body = "Future optional integration for listing repos and opening files. Phase 1 shows the slot only.",
        status = "Not stored",
        color = ForgeBlue,
    )
    SettingsSection(
        label = "Model provider key",
        title = "Bring your own provider",
        body = "Reserved for legitimate API keys or on-device model routing. No key entry or storage is active.",
        status = "Preview",
        color = ForgeGold,
    )
    SettingsSection(
        label = "Sandbox safety",
        title = "Ask before write or run",
        body = "Future agent actions should require an explicit checkpoint before file edits, installs, or commands.",
        status = "Required",
        color = ForgeMint,
    )

    RailCard(label = "Phase boundary") {
        Text(
            text = "Phase 1 is UI skeleton and inspectability. Secure auth, repo browsing, file reads, edits, and execution belong to later phases.",
            color = ForgePaper,
            fontSize = 12.sp,
            lineHeight = 17.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun SettingsSection(
    label: String,
    title: String,
    body: String,
    status: String,
    color: Color,
) {
    FeatureCard(
        label = label,
        title = title,
        body = body,
        color = color,
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            color = ForgePaper.copy(alpha = 0.74f),
            border = BorderStroke(1.dp, ForgeInk.copy(alpha = 0.24f)),
        ) {
            Row(
                modifier = Modifier.padding(11.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                Text(
                    modifier = Modifier.weight(1f),
                    text = "Preview placeholder",
                    color = ForgeInk,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                )
                StatusChip(text = status, color = ForgeInk)
            }
        }
    }
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
    currentBrief: AgentTaskDraft,
    loadedQueuedBrief: AgentTaskDraft?,
    recentBriefs: List<AgentTaskDraft>,
    onSaveBrief: () -> Unit,
    onSelectRecentBrief: (AgentTaskDraft) -> Unit,
    onDeleteRecentBrief: (AgentTaskDraft) -> Unit,
    onClearDraft: () -> Unit,
    onMarkReady: () -> Unit,
) {
    DarkBlueprintPanel()

    DraftSavedRow()

    RecentBriefsPanel(
        currentBrief = currentBrief,
        loadedQueuedBrief = loadedQueuedBrief,
        recentBriefs = recentBriefs,
        onSaveBrief = onSaveBrief,
        onSelectBrief = onSelectRecentBrief,
        onDeleteBrief = onDeleteRecentBrief,
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
        markedReady = currentBrief.markedReady,
        onSaveAsBrief = onSaveBrief,
        onMarkReady = onMarkReady,
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
    currentBrief: AgentTaskDraft,
    loadedQueuedBrief: AgentTaskDraft?,
    recentBriefs: List<AgentTaskDraft>,
    onSaveBrief: () -> Unit,
    onSelectBrief: (AgentTaskDraft) -> Unit,
    onDeleteBrief: (AgentTaskDraft) -> Unit,
    onClearDraft: () -> Unit,
) {
    FeatureCard(
        label = "Ready queue",
        title = "Local briefs waiting on this phone.",
        body = "Save phone-side work here, then mark complete briefs ready for the future sandbox preview.",
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
                    text = "No queued briefs yet.",
                    color = ForgeMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            } else {
                recentBriefs.forEachIndexed { index, brief ->
                    RecentBriefRow(
                        brief = brief,
                        index = index,
                        isActive = loadedQueuedBrief?.isSameBriefAs(brief)
                            ?: brief.isSameBriefAs(currentBrief),
                        onClick = { onSelectBrief(brief) },
                        onDelete = { onDeleteBrief(brief) },
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
    isActive: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    val queueStatus = remember(brief) { brief.readinessStatus() }
    val missingDetails = remember(brief) {
        findMissingRunPlanDetails(
            title = brief.title,
            goal = brief.goal,
            repo = brief.repo,
            output = brief.output,
        )
    }
    val rowDetail = when {
        missingDetails.isNotEmpty() -> "Needs: ${missingDetails.joinToString()}."
        isActive -> "Active brief on this device."
        else -> brief.goal.ifBlank { "Restore this brief and sharpen the next sandbox run." }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        color = if (isActive) ForgeMint.copy(alpha = 0.72f) else ForgePaper.copy(alpha = 0.72f),
        border = BorderStroke(1.dp, if (isActive) ForgeTeal else ForgeLine),
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
                    .background(if (isActive) ForgeTeal else queueStatus.color.copy(alpha = 0.24f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = (index + 1).toString(),
                    color = if (isActive) ForgePaper else ForgeInk,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = brief.title.ifBlank { "Untitled local task" },
                    color = ForgeInk,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = rowDetail,
                    color = ForgeMuted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                StatusChip(text = queueStatus.label, color = queueStatus.color)
                if (isActive) {
                    StatusChip(text = "Active", color = ForgeTeal)
                }
                TextButton(onClick = onDelete) {
                    Text(
                        text = "Delete",
                        color = ForgeRust,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                    )
                }
            }
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
        shape = RoundedCornerShape(22.dp),
        color = ForgeInk,
        border = BorderStroke(1.dp, ForgeLineLight),
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
                fontWeight = FontWeight.Medium,
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
    markedReady: Boolean,
    onSaveAsBrief: () -> Unit,
    onMarkReady: () -> Unit,
) {
    val clipboardManager = LocalClipboardManager.current
    var actionMessage by remember(title, goal, repo, constraints, output) { mutableStateOf("") }
    var inspectedPlan by remember(title, goal, repo, constraints, output) { mutableStateOf(false) }
    var reviewedFiles by remember(title, goal, repo, constraints, output) { mutableStateOf(false) }
    var reviewedChecks by remember(title, goal, repo, constraints, output) { mutableStateOf(false) }
    val runPlan = remember(title, goal, repo, constraints, output) {
        buildRunPlanPreview(
            title = title,
            goal = goal,
            repo = repo,
            constraints = constraints,
            output = output,
        )
    }
    val readiness = remember(runPlan, markedReady) {
        readinessStatus(
            missingDetails = runPlan.missingDetails,
            markedReady = markedReady,
        )
    }
    val checklistComplete = inspectedPlan && reviewedFiles && reviewedChecks
    val canMarkReady = runPlan.missingDetails.isEmpty() && checklistComplete
    val canUseReadyAction = markedReady || canMarkReady

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = ForgeInk,
        border = BorderStroke(1.dp, ForgeLineLight),
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
                    LabelText(text = "Local plan manifest", dark = false)
                }
                StatusChip(text = readiness.label, color = readiness.color)
            }
            Text(
                text = "Proof object draft",
                color = ForgePaper,
                fontSize = 23.sp,
                lineHeight = 25.sp,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = "Preview only. This turns the brief into manifest fields, likely files, checks, and a permission gate. It does not execute code.",
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
                            text = "Blocked details",
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
                PlanCell(modifier = Modifier.weight(1f), label = "Manifest", value = runPlan.target)
                PlanCell(modifier = Modifier.weight(1f), label = "Likely files", value = runPlan.likelyFiles)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PlanCell(modifier = Modifier.weight(1f), label = "Checks", value = runPlan.checks)
                PlanCell(modifier = Modifier.weight(1f), label = "Result", value = runPlan.result)
            }

            RunPlanDetailsSection(runPlan = runPlan)
            RunPlanChecklistSection(
                inspectedPlan = inspectedPlan,
                onInspectedPlanChange = { inspectedPlan = it },
                reviewedFiles = reviewedFiles,
                onReviewedFilesChange = { reviewedFiles = it },
                reviewedChecks = reviewedChecks,
                onReviewedChecksChange = { reviewedChecks = it },
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CompactPlanAction(
                    modifier = Modifier.weight(1f),
                    text = "Copy plan",
                    enabled = true,
                    onClick = {
                        clipboardManager.setText(AnnotatedString(buildRunPlanCopy(runPlan, readiness.label)))
                        actionMessage = "Plan copied. Preview only; no code ran."
                    },
                )
                CompactPlanAction(
                    modifier = Modifier.weight(1f),
                    text = "Save as brief",
                    enabled = true,
                    onClick = {
                        onSaveAsBrief()
                        actionMessage = "Saved to the local ready queue. Matching briefs update the existing row."
                    },
                )
                CompactPlanAction(
                    modifier = Modifier.weight(1f),
                    text = if (markedReady) "Ready gate" else "Gate ready",
                    enabled = canUseReadyAction,
                    onClick = {
                        onMarkReady()
                        actionMessage = "Ready gate set for local sandbox planning."
                    },
                )
            }
            if (runPlan.missingDetails.isEmpty() && !checklistComplete) {
                Text(
                    text = "Tick the preview checks after you inspect the plan. This does not run code.",
                    color = ForgePaper.copy(alpha = 0.64f),
                    fontSize = 10.sp,
                    lineHeight = 14.sp,
                    fontWeight = FontWeight.SemiBold,
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
private fun RunPlanChecklistSection(
    inspectedPlan: Boolean,
    onInspectedPlanChange: (Boolean) -> Unit,
    reviewedFiles: Boolean,
    onReviewedFilesChange: (Boolean) -> Unit,
    reviewedChecks: Boolean,
    onReviewedChecksChange: (Boolean) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = ForgePaper.copy(alpha = 0.08f),
        border = BorderStroke(1.dp, ForgePaper.copy(alpha = 0.22f)),
    ) {
        Column(
            modifier = Modifier.padding(11.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = "Ready gate",
                color = ForgePaper,
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold,
            )
            Text(
                text = "Preview-only inspection before this brief can leave chat.",
                color = ForgePaper.copy(alpha = 0.66f),
                fontSize = 10.sp,
                lineHeight = 14.sp,
                fontWeight = FontWeight.SemiBold,
            )
            RunPlanChecklistRow(
                checked = inspectedPlan,
                onCheckedChange = onInspectedPlanChange,
                label = "Review manifest",
            )
            RunPlanChecklistRow(
                checked = reviewedFiles,
                onCheckedChange = onReviewedFilesChange,
                label = "Review likely files",
            )
            RunPlanChecklistRow(
                checked = reviewedChecks,
                onCheckedChange = onReviewedChecksChange,
                label = "Review local checks",
            )
        }
    }
}

@Composable
private fun RunPlanChecklistRow(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    label: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(
                checkedColor = ForgeTeal,
                uncheckedColor = ForgePaper.copy(alpha = 0.58f),
                checkmarkColor = ForgeInk,
            ),
        )
        Text(
            text = label,
            color = ForgePaper,
            fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold,
        )
    }
}

@Composable
private fun RunPlanDetailsSection(runPlan: RunPlanPreviewModel) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = ForgePaper.copy(alpha = 0.08f),
        border = BorderStroke(1.dp, ForgePaper.copy(alpha = 0.22f)),
    ) {
        Column(
            modifier = Modifier.padding(11.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Manifest fields",
                        color = ForgePaper,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                    )
                    Text(
                        text = "Likely files, checks, and gate before sandbox work.",
                        color = ForgePaper.copy(alpha = 0.66f),
                        fontSize = 10.sp,
                        lineHeight = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                StatusChip(text = "MANIFEST", color = ForgeGold)
            }

            RunPlanDetailRow(label = "Edit intent", value = runPlan.editIntent)
            RunPlanDetailRow(label = "Likely files", value = runPlan.fileDetails.joinToString(separator = "\n"))
            RunPlanDetailRow(label = "Validation checks", value = runPlan.validationChecks.joinToString(separator = "\n"))
            RunPlanDetailRow(label = "Expected output", value = runPlan.outputIntent)
            RunPlanDetailRow(
                label = "Blocking details",
                value = if (runPlan.missingDetails.isEmpty()) {
                    "None. This brief can pass the ready gate for local sandbox planning."
                } else {
                    "Add ${runPlan.missingDetails.joinToString()}."
                },
            )
        }
    }
}

@Composable
private fun RunPlanDetailRow(
    label: String,
    value: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(
            text = label,
            color = ForgePaper.copy(alpha = 0.58f),
            fontSize = 9.sp,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = value,
            color = ForgePaper,
            fontSize = 11.sp,
            lineHeight = 15.sp,
            fontWeight = FontWeight.SemiBold,
        )
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
    val fileDetails = inferFileDetails(normalizedGoal, normalizedOutput)
    val checks = inferLightweightChecks(normalizedConstraints)
    val validationChecks = inferValidationChecks(normalizedConstraints)
    return RunPlanPreviewModel(
        target = normalizedRepo.takePreviewWords(maxWords = 4),
        likelyFiles = likelyFiles,
        checks = checks,
        result = "User summary",
        editIntent = normalizedGoal.takePreviewWords(maxWords = 18),
        fileDetails = fileDetails,
        validationChecks = validationChecks,
        outputIntent = normalizedOutput.takePreviewWords(maxWords = 18),
        missingDetails = missingDetails,
        steps = listOf(
            RunPlanStep(
                title = "Plan local context",
                detail = "Preview opening $normalizedRepo, checking status, and finding files tied to ${normalizedTitle.takePreviewWords(maxWords = 5)}.",
                state = "PREVIEW",
                color = ForgeSlate,
            ),
            RunPlanStep(
                title = "Choose likely files",
                detail = "Start with $likelyFiles, then narrow by imports, UI state, and existing helpers.",
                state = "MANIFEST",
                color = ForgeGold,
            ),
            RunPlanStep(
                title = "Draft smallest edit",
                detail = normalizedGoal.takePreviewWords(maxWords = 14),
                state = "LOCAL PLAN",
                color = ForgeRust,
            ),
            RunPlanStep(
                title = "List phone-safe checks",
                detail = checks,
                state = "NO RUN",
                color = ForgeSlate,
            ),
            RunPlanStep(
                title = "Summarize for you",
                detail = normalizedOutput.takePreviewWords(maxWords = 14),
                state = "GATE",
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
        appendLine("PocketForge Local Plan Manifest")
        appendLine("Status: $readinessLabel")
        appendLine("Mode: Preview only, no code execution")
        appendLine()
        appendLine("Target: ${runPlan.target}")
        appendLine("Likely files: ${runPlan.likelyFiles}")
        appendLine("Checks: ${runPlan.checks}")
        appendLine("Result: ${runPlan.result}")
        appendLine("Missing detail: $missingDetails")
        appendLine()
        appendLine("Plan details:")
        appendLine("Edit intent: ${runPlan.editIntent}")
        appendLine("Files:")
        runPlan.fileDetails.forEach { fileDetail ->
            appendLine("- $fileDetail")
        }
        appendLine("Validation checks:")
        runPlan.validationChecks.forEach { validationCheck ->
            appendLine("- $validationCheck")
        }
        appendLine("Expected output: ${runPlan.outputIntent}")
        appendLine("Blocking details: $missingDetails")
        appendLine()
        appendLine("Steps:")
        runPlan.steps.forEachIndexed { index, step ->
            appendLine("${index + 1}. ${step.title} [${step.state}]")
            appendLine("   ${step.detail}")
        }
    }.trim()
}

@Composable
private fun QueueRow(
    badge: String,
    title: String,
    detail: String,
    state: String,
    color: Color,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = ForgePaper.copy(alpha = 0.68f),
        border = BorderStroke(1.dp, ForgeLine),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(color),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = badge,
                    color = ForgeInk,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = ForgeInk,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = detail,
                    color = ForgeMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            StatusChip(text = state, color = ForgeMuted)
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
        shape = MaterialTheme.shapes.medium,
        color = ForgePaper.copy(alpha = 0.72f),
        border = BorderStroke(1.dp, ForgeLine),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(11.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 24.dp, height = 2.dp)
                        .background(color),
                )
                LabelText(text = label, dark = true)
            }
            Text(
                text = title,
                color = ForgeInk,
                fontSize = 22.sp,
                lineHeight = 24.sp,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = body,
                color = ForgeMuted,
                fontSize = 13.sp,
                lineHeight = 19.sp,
                fontWeight = FontWeight.Medium,
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
        shape = RoundedCornerShape(22.dp),
        color = ForgeInk,
        border = BorderStroke(1.dp, ForgeLineLight),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(11.dp),
        ) {
            LabelText(text = "Build sheet", dark = false)
            Text(
                text = "Shape the next local run.",
                color = ForgePaper,
                fontSize = 27.sp,
                lineHeight = 29.sp,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = "Draft a phone-side task, inspect the generated plan, and mark it ready only after the preview checks make sense.",
                color = ForgePaper.copy(alpha = 0.72f),
                fontSize = 13.sp,
                lineHeight = 19.sp,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
private fun DraftSavedRow() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = ForgePaper.copy(alpha = 0.66f),
        border = BorderStroke(1.dp, ForgeLine),
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
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "Agent task fields save locally on this device.",
                    color = ForgeMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
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
        color = ForgePaper.copy(alpha = 0.72f),
        border = BorderStroke(1.dp, ForgeLine),
    ) {
        Column(
            modifier = Modifier.padding(11.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            LabelText(text = label, dark = true)
            Text(
                text = value,
                color = ForgeInk,
                fontSize = 15.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.SemiBold,
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
        border = BorderStroke(1.dp, ForgeLineLight),
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
            fontWeight = FontWeight.SemiBold,
        )
        TextButton(onClick = {}) {
            Text(
                text = action,
                color = ForgeRust,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun SectionTitleBlock(title: String, subtitle: String) {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        LabelText(text = subtitle, dark = true)
        Text(
            text = title,
            color = ForgeInk,
            fontSize = 28.sp,
            lineHeight = 30.sp,
            fontWeight = FontWeight.Medium,
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
            .padding(horizontal = 14.dp, vertical = 8.dp),
        shape = CircleShape,
        color = ForgePaper.copy(alpha = 0.9f),
        border = BorderStroke(1.dp, ForgeLine),
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
                        .clip(CircleShape)
                        .clickable { onTabSelected(tab) }
                        .background(if (selected) ForgeInk else Color.Transparent)
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = tab.label,
                        color = if (selected) ForgePaper else ForgeInk.copy(alpha = 0.58f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
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
) {
    Chat("Chat"),
    Repos("Repos"),
    Files("Files"),
    Build("Build"),
    Settings("Settings"),
}

private data class AgentTaskDraft(
    val title: String,
    val goal: String,
    val repo: String,
    val constraints: String,
    val output: String,
    val markedReady: Boolean = false,
)

private data class BriefReadinessStatus(
    val label: String,
    val color: Color,
)

private data class RunPlanPreviewModel(
    val target: String,
    val likelyFiles: String,
    val checks: String,
    val result: String,
    val editIntent: String,
    val fileDetails: List<String>,
    val validationChecks: List<String>,
    val outputIntent: String,
    val missingDetails: List<String>,
    val steps: List<RunPlanStep>,
)

private data class RunPlanStep(
    val title: String,
    val detail: String,
    val state: String,
    val color: Color,
)

private data class MockRepo(
    val name: String,
    val path: String,
    val detail: String,
    val branches: List<String>,
    val state: String,
    val initials: String,
    val color: Color,
)

private data class MockFile(
    val name: String,
    val path: String,
    val kind: String,
    val mode: String,
    val preview: String,
)

private fun AgentTaskDraft.isSameBriefAs(other: AgentTaskDraft): Boolean {
    return title == other.title &&
        goal == other.goal &&
        repo == other.repo &&
        constraints == other.constraints &&
        output == other.output
}

private fun AgentTaskDraft.readinessStatus(): BriefReadinessStatus {
    return readinessStatus(
        missingDetails = findMissingRunPlanDetails(
            title = title,
            goal = goal,
            repo = repo,
            output = output,
        ),
        markedReady = markedReady,
    )
}

private fun readinessStatus(
    missingDetails: List<String>,
    markedReady: Boolean,
): BriefReadinessStatus {
    return when {
        missingDetails.isNotEmpty() -> BriefReadinessStatus(label = "BLOCKED", color = ForgeGold)
        markedReady -> BriefReadinessStatus(label = "READY GATE", color = ForgeSlate)
        else -> BriefReadinessStatus(label = "LOCAL PLAN", color = ForgeRust)
    }
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

private fun inferFileDetails(goal: String, output: String): List<String> {
    val searchableText = "$goal $output".lowercase()

    return when {
        searchableText.contains("compose") ||
            searchableText.contains("ui") ||
            searchableText.contains("screen") ||
            searchableText.contains("tab") -> listOf(
                "Start in app/src/main/java/dev/pocketforge/app/MainActivity.kt.",
                "Follow existing Compose state, card, and preview helpers before adding new structure.",
            )
        searchableText.contains("apk") ||
            searchableText.contains("ci") ||
            searchableText.contains("workflow") ||
            searchableText.contains("actions") -> listOf(
                "Inspect .github/workflows and build.gradle.kts files.",
                "Keep cloud CI optional; the phone sandbox remains the primary product direction.",
            )
        searchableText.contains("readme") ||
            searchableText.contains("docs") ||
            searchableText.contains("plan") -> listOf(
                "Inspect README.md and plan.md before editing copy.",
                "Keep wording local, on-device, and sandbox oriented.",
            )
        searchableText.contains("gradle") ||
            searchableText.contains("dependency") -> listOf(
                "Inspect Gradle catalog and module build files.",
                "Avoid dependency changes unless the task clearly needs them.",
            )
        else -> listOf(
            "Search the local repo for matching feature, UI, and helper names.",
            "Open the smallest relevant file set before proposing edits.",
        )
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

private fun inferValidationChecks(constraints: String): List<String> {
    val searchableText = constraints.lowercase()

    return if (
        searchableText.contains("no local android") ||
        searchableText.contains("no android builds") ||
        searchableText.contains("no local builds")
    ) {
        listOf(
            "Inspect source changes and run git diff --check.",
            "Review status and diff only; leave Android or Gradle builds to the configured CI path.",
        )
    } else {
        listOf(
            "Run the safest available local static checks for the changed files.",
            "Review status and diff before handing the plan back to the user.",
        )
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
private const val PREF_TASK_MARKED_READY = "task_marked_ready"
private const val PREF_RECENT_BRIEFS = "recent_briefs"
private const val MAX_RECENT_BRIEFS = 5
private const val JSON_TITLE = "title"
private const val JSON_GOAL = "goal"
private const val JSON_REPO = "repo"
private const val JSON_CONSTRAINTS = "constraints"
private const val JSON_OUTPUT = "output"
private const val JSON_MARKED_READY = "markedReady"

private fun SharedPreferences.loadAgentTaskDraft(): AgentTaskDraft {
    return AgentTaskDraft(
        title = getString(PREF_TASK_TITLE, DefaultAgentTaskDraft.title) ?: DefaultAgentTaskDraft.title,
        goal = getString(PREF_TASK_GOAL, DefaultAgentTaskDraft.goal) ?: DefaultAgentTaskDraft.goal,
        repo = getString(PREF_TASK_REPO, DefaultAgentTaskDraft.repo) ?: DefaultAgentTaskDraft.repo,
        constraints = getString(PREF_TASK_CONSTRAINTS, DefaultAgentTaskDraft.constraints)
            ?: DefaultAgentTaskDraft.constraints,
        output = getString(PREF_TASK_OUTPUT, DefaultAgentTaskDraft.output) ?: DefaultAgentTaskDraft.output,
        markedReady = getBoolean(PREF_TASK_MARKED_READY, DefaultAgentTaskDraft.markedReady),
    )
}

private fun SharedPreferences.saveAgentTaskDraft(draft: AgentTaskDraft) {
    edit()
        .putString(PREF_TASK_TITLE, draft.title)
        .putString(PREF_TASK_GOAL, draft.goal)
        .putString(PREF_TASK_REPO, draft.repo)
        .putString(PREF_TASK_CONSTRAINTS, draft.constraints)
        .putString(PREF_TASK_OUTPUT, draft.output)
        .putBoolean(PREF_TASK_MARKED_READY, draft.markedReady)
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
                        markedReady = brief.optBoolean(JSON_MARKED_READY, false),
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
                .put(JSON_OUTPUT, brief.output)
                .put(JSON_MARKED_READY, brief.markedReady),
        )
    }

    edit()
        .putString(PREF_RECENT_BRIEFS, jsonBriefs.toString())
        .apply()
}

private val EpochInk = Color(0xFF171714)
private val EpochUiInk = Color(0xFF111412)
private val EpochBone = Color(0xFFF2EFE7)
private val EpochField = Color(0xFFE9E3D6)
private val EpochPaper = Color(0xFFFBF8EF)
private val EpochMuted = Color(0xFF616963)
private val EpochCopper = Color(0xFFA64B2A)
private val EpochMineral = Color(0xFF60727A)
private val EpochSignalGold = Color(0xFFD8B35D)
private val EpochMoss = Color(0xFF69755E)

private val ForgeInk = EpochUiInk
private val ForgeCanvas = EpochBone
private val ForgePaper = EpochPaper
private val ForgeMuted = EpochMuted
private val ForgeLine = Color(0x29171414)
private val ForgeLineLight = Color(0x30FBF8EF)
private val ForgeGreen = EpochMoss
private val ForgeGraphite = EpochInk
private val ForgeSlate = EpochMineral
private val ForgeTeal = EpochMineral
private val ForgeMint = Color(0xFFE7E8DE)
private val ForgeRust = EpochCopper
private val ForgePeach = Color(0xFFF1DED4)
private val ForgeGold = EpochSignalGold
private val ForgeBlue = Color(0xFFE1E8E7)

private val MockRepos = listOf(
    MockRepo(
        name = "PocketForge",
        path = "~/code/PocketForge",
        detail = "Android app shell for a personal local coding agent. Branch state is mocked for the phone UI skeleton.",
        branches = listOf("phase-1-ui", "main", "local-sandbox-spike"),
        state = "Selected",
        initials = "PF",
        color = ForgeGold,
    ),
    MockRepo(
        name = "Epoch Notes",
        path = "~/code/epoch-notes",
        detail = "Private markdown workspace used to test file browsing and brief handoffs from a phone.",
        branches = listOf("main", "drafts"),
        state = "Local",
        initials = "EN",
        color = ForgeSlate,
    ),
    MockRepo(
        name = "Tiny Tools",
        path = "~/code/tiny-tools",
        detail = "Small scripts and utilities, useful for showing a dense but readable repo picker.",
        branches = listOf("main", "mobile-inspect"),
        state = "Local",
        initials = "TT",
        color = ForgeMint,
    ),
)

private val MockFiles = listOf(
    MockFile(
        name = "MainActivity.kt",
        path = "app/src/main/java/dev/pocketforge/app/MainActivity.kt",
        kind = "KT",
        mode = "Code",
        preview = """
            @Composable
            private fun PocketForgeApp() {
                // Phone-first shell:
                // Chat -> Repos -> Files -> Build -> Settings
                // Preview only; no command execution in Phase 1.
            }
        """.trimIndent(),
    ),
    MockFile(
        name = "README.md",
        path = "README.md",
        kind = "MD",
        mode = "Docs",
        preview = """
            # PocketForge

            Personal-use Android coding agent shell.

            Phase 1 makes the app feel real with local-first mock flows.
            Phase 2 can add read-only GitHub and file browsing.
        """.trimIndent(),
    ),
    MockFile(
        name = "build-apk.yml",
        path = ".github/workflows/build-apk.yml",
        kind = "YML",
        mode = "CI",
        preview = """
            name: Build debug APK

            on:
              workflow_dispatch:

            # Optional artifact path; local phone sandbox remains the product center.
        """.trimIndent(),
    ),
    MockFile(
        name = "plan.md",
        path = "plan.md",
        kind = "MD",
        mode = "Plan",
        preview = """
            ## Phase 1

            Make the mock app feel like the real product:
            chat, repo picker, file browser, brief handoff, and settings.
        """.trimIndent(),
    ),
)

@Preview(showBackground = true, widthDp = 360, heightDp = 780)
@Composable
private fun PocketForgePreview() {
    PocketForgeApp()
}
