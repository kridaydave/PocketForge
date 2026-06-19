# PocketForge

PocketForge is a private-first Android AI coding harness for driving a full coding loop from a phone: inspect a GitHub repo, ask an agent for help, review proposed edits, push branches, run CI, and iterate from GitHub Actions results.

## Builds

GitHub Actions is the default build path for this repository. Contributors should not run Android or Gradle builds locally by default; local work should stay focused on source edits, documentation, Git status/diff checks, and lightweight non-build inspection.

For Phase 0 there is no Gradle wrapper yet. The Android workflow installs/configures Gradle `9.4.1` through `gradle/actions/setup-gradle` and runs the system `gradle` command in GitHub Actions.

To get a debug APK:

1. Push a branch, open a pull request, or manually run the `Android APK` workflow from the Actions tab.
2. Wait for the workflow to complete.
3. Download the `pocketforge-debug-apk` artifact from the workflow run.
4. Install the APK on the target Android phone for smoke testing.

If a local build is ever needed, make it an explicit decision first so the cloud-build workflow remains the source of truth.
