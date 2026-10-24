# Testing and hosting status

## Current source

The public application targets Jenkins 2.555.3 and uses the minimum parent POM required by the official hosting checker. The existing parameter tests have been migrated to JUnit Jupiter without removing test cases. Strict bundled-artifact, legacy import and obsolete-override checks are enabled. Spotless formatting is enforced.

Run the commands in the README. Tests start isolated local controllers and loopback HTTP fixtures, never an existing deployment controller. Test tokens are deliberate dummy values, not usable credentials. Preserve that boundary in new tests.

GitHub CI covers Linux/Windows Java 21 and Linux Java 25; the root Jenkinsfile also declares Java 25 for ci.jenkins.io after hosting. An official Jenkins security scan workflow and future hosting-team CODEOWNERS are included. Workflow configuration is not proof that a remote run succeeded.

## Local validation history

The 2026-10-24 preparation snapshot passed 37 Java/Jenkins tests, 10 Python publication-check tests and Node syntax/interaction checks. Formatting was skipped in that earlier snapshot. Those results do not describe every later source change.

On 2026-10-25 the migrated test suite again passed all 37 tests without skips. The full Maven verification also passed: SpotBugs reported zero defects/errors and Spotless kept all maintained Java sources and the POM clean. Official findings are tracked in publishing/check-status.md. Runtime parameter behavior is retained; changes are required publication metadata, test framework, formatting and theme-aware popup shadow. Already deployed local compatibility builds are not changed.

## Official gate

The repository is public. The first official hosting-check run identified 16 Required findings. Code/configuration fixes are being rechecked; the remaining account requirements must be satisfied through actual community-account first logins to Jira and Artifactory. No password is kept in this repository, and hosting approval is not claimed.

The source-publication checker is only a local distribution guard, not the official hosting checker, exhaustive secret scanner, legal review or security certification.
