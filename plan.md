# PocketForge Plan

Last updated: 2026-06-19

## Repo Initialization Status

- Local workspace: `C:\Users\NewAdmin\Desktop\PocketForge`
- GitHub remote: `https://github.com/kridaydave/PocketForge.git`
- Branch: `main`, tracking `origin/main`
- Remote starting point: Apache-2.0 `LICENSE` only
- This plan is the working roadmap and should be committed before app scaffolding begins.

## Execution Rules

- Do not run Android or Gradle builds on this dev box unless explicitly requested.
- Use local commands only for repository inspection, file edits, Git status/diff checks, and lightweight non-build validation.
- GitHub Actions is the build source of truth.
- APKs should be produced by GitHub Actions and downloaded as workflow artifacts.
- The first implementation branch should add the Android skeleton and the GitHub Actions APK workflow together so every later change has cloud validation.

## One-Line Idea

PocketForge is a personal Android AI coding harness for running real coding workflows from a phone: chat with an AI agent, connect GitHub repos, inspect/edit files, review diffs, trigger CI builds/tests, and continue coding sessions without needing a powerful laptop.

## Current Goal

Build a private MVP first. This is not commercial yet.

The first version should prove one thing:

> Can I use my phone to drive a full coding loop against a GitHub repo?

That loop is:

1. Pick a repo.
2. Ask the AI to inspect code or fix something.
3. Let the AI read/search files through approved tools.
4. Preview proposed edits.
5. Approve changes.
6. Commit to a branch.
7. Push to GitHub.
8. Run GitHub Actions.
9. Read CI logs.
10. Iterate until the branch is ready.

## Key Constraints

- Personal use first.
- Android app with a polished UI and good UX.
- Laptop may be weak for local Android/Gradle builds.
- Prefer cloud builds through GitHub Actions.
- Use curated tools instead of a full Termux/Linux backend for v1.
- API keys should be stored securely on-device.
- The model should not get unrestricted shell or filesystem access.
- Risky actions should require explicit approval.

## Working Architecture

```text
Android app
  -> Chat / agent session UI
  -> Repo picker
  -> File browser
  -> Markdown/code viewer
  -> Code editor
  -> Diff viewer
  -> Tool approval panels
  -> Settings and API key management

Agent runtime
  -> Model provider adapter
  -> API key manager
  -> Tool-call router
  -> Approval policy
  -> Session history

Curated tools
  -> GitHub repo/file search
  -> Read files
  -> Propose file edits
  -> Generate diffs
  -> Create branch
  -> Commit changes
  -> Push branch
  -> Open PR
  -> Trigger GitHub Actions
  -> Read CI logs

Storage
  -> Android Keystore for secrets
  -> Room/local database for sessions
  -> Local workspace cache for repo files and diffs
```

## Recommended Stack

- Android: Kotlin + Jetpack Compose
- Local database: Room
- Secure storage: Android Keystore plus encrypted preferences/storage
- Networking: Ktor or OkHttp
- GitHub integration: GitHub REST/GraphQL APIs first
- Markdown rendering: CommonMark-compatible renderer
- Syntax highlighting: Tree-sitter or TextMate grammar-based highlighter
- Builds: GitHub Actions produces APK artifacts

## Build Strategy

Do not rely on a powerful local machine.

```text
Laptop or phone editor
  -> push code to GitHub

GitHub Actions
  -> run Gradle
  -> build debug APK
  -> upload APK artifact

Phone
  -> download APK
  -> install
  -> test
```

The project should include GitHub Actions from the beginning so Gradle-heavy work happens in the cloud.

For Phase 0, there is no Gradle wrapper yet. GitHub Actions should install/configure Gradle with `gradle/actions/setup-gradle` and run the system `gradle` command.

## MVP Scope

### Must Have

- App shell with polished Compose UI.
- Secure API key entry and storage.
- Chat screen for agent sessions.
- GitHub authentication using a personal token or OAuth.
- Repo picker.
- File tree.
- File viewer with Markdown rendering and syntax highlighting.
- AI can read/search selected repo files.
- AI can propose edits.
- User can preview diffs before applying.
- User can approve commit and push.
- GitHub Actions workflow can build APKs.

### Should Have

- Session history.
- Per-repo context notes.
- Branch creation flow.
- CI status viewer.
- CI log viewer.
- "Fix failed build" workflow.
- PR creation.

### Later

- Multiple model providers.
- Multiple API keys with safe failover/backoff.
- Local code execution for small scripts.
- Remote runner support.
- Optional Termux/proot integration.
- Offline repo cache.
- Rich editor features such as symbol outline and jump-to-definition.

## Safety Model

The AI should request actions through tools. The app decides whether to allow them.

Safe by default:

- Read selected repo files.
- Search selected repo files.
- Summarize files.
- Generate proposed edits.

Needs approval:

- Apply edits.
- Commit changes.
- Push branch.
- Open PR.
- Trigger workflow.
- Read broad/private user storage.

Avoid:

- Exposing API keys to the model.
- Letting the model run arbitrary shell commands in v1.
- Letting the model access all phone files.
- Auto-pushing changes without review.

## Development Phases

### Phase 0: Project Setup

- Create Android project.
- Add Compose.
- Add GitHub Actions APK build.
- Confirm APK artifact can be downloaded and installed on phone.

Exit criteria:

- A blank PocketForge app builds in GitHub Actions.
- APK can be installed on the phone.

Detailed Phase 0 sequence:

| Step | Depends on | Output | Verification |
|------|------------|--------|--------------|
| Track planning docs | Repo initialized | `plan.md` in Git | `git status` shows intended files only |
| Add Android skeleton | Planning docs | Minimal Kotlin + Compose app | Source review only in dev box |
| Add Gradle project files | Android skeleton | Gradle project entrypoint, wrapper deferred | Source review only in dev box |
| Add GitHub Actions APK workflow | Gradle project files | Workflow that runs `gradle assembleDebug --no-daemon` in GitHub Actions | Actions run succeeds on GitHub |
| Upload debug APK artifact | APK workflow | Downloadable APK artifact | Artifact appears on completed workflow |
| Phone install smoke test | APK artifact | Installed blank app | Manual install on Android phone |

Critical path: Android skeleton -> Gradle entrypoint -> GitHub Actions APK workflow -> APK artifact -> phone install.

Phase 0 cut line: if time is tight, skip UI polish and any AI/GitHub integration. The only non-negotiable outcome is a blank installable app built by GitHub Actions.

### Phase 1: UI Skeleton

- Main navigation.
- Chat screen.
- Repo screen.
- File browser screen.
- Settings screen.

Exit criteria:

- App feels like the real product even with mock data.

### Phase 2: GitHub Read-Only Flow

- Store GitHub token securely.
- List repos.
- Browse repo files.
- Open Markdown/code files.
- Search files.

Exit criteria:

- A user can inspect a real GitHub repo from the app.

### Phase 3: Agent Read Tools

- Connect model provider.
- Let the model call read/search tools.
- Show tool activity in the chat.
- Keep model access limited to selected repo context.

Exit criteria:

- User can ask questions about a repo and get source-backed answers.

### Phase 4: Editing Flow

- AI proposes file edits.
- App displays mobile-friendly diffs.
- User approves or rejects changes.
- App commits to a new branch.

Exit criteria:

- User can make a real code change from the phone.

### Phase 5: CI Loop

- Trigger/read GitHub Actions.
- Show status and logs.
- Feed failure logs back to the AI.
- Iterate on fixes.

Exit criteria:

- User can run a complete fix-build-fix loop from the phone.

### Phase 6: PR Flow

- Open PR.
- Generate PR summary.
- Track checks.
- Let AI respond to review comments later.

Exit criteria:

- User can take a task from issue/idea to PR without laptop builds.

## Open Decisions

- App name finalization: PocketForge is the current working name.
- GitHub auth approach: personal access token first or OAuth first.
- Model provider: one provider first, or provider-agnostic from day one.
- Editor depth: simple text editor first, or richer code editor first.
- Git strategy: GitHub API-only first, or local git implementation later.
- Build release style: debug APK artifacts first, signed release APK later.

## Suggested First Implementation Target

Start with the smallest real loop:

1. Blank Compose app.
2. GitHub Actions builds APK.
3. Settings screen stores a GitHub token.
4. Repo picker lists repositories.
5. File browser opens files.

This proves the build pipeline, phone install loop, GitHub auth, and basic UI foundation before adding AI complexity.

## Continuation Notes For New Chat

If continuing in a new chat/thread:

1. Open this folder:
   `C:\Users\NewAdmin\Desktop\PocketForge`
2. Read this `plan.md`.
3. Keep the scope private/personal first.
4. Prioritize GitHub Actions cloud builds because the laptop may not handle Gradle comfortably.
5. Build curated coding tools before any full Linux/Termux backend.
6. Do not put API keys or GitHub tokens in the repo.
