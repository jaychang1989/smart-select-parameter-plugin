# Hosting application status (2026-10-25)

## Submitted

The official hosting application is [repository-permissions-updater #5409](https://github.com/jenkins-infra/repository-permissions-updater/issues/5409), submitted by jaychang1989. A standard /hosting re-check comment was posted to start the official bot workflow. This is an application, not approval, repository transfer or an update-center release.

## Official pre-submission check

The unmodified official checker at commit 84c6de2390972f1a6592ef0372e4d9e272780d1b was built and run with Java 25. After fixes and account-report synchronization it exited 0 and reported that the repository passes the automated hosting checks. All Required findings were resolved. The remaining README organization recommendations were then addressed, and the official bot applied bot-check-complete with no outstanding findings.

Jira and Artifactory reports both include the community identifier jaychang1989. No password was stored or included in the application.

## Validation

Local verification passed 37 Java/Jenkins tests without skips, zero SpotBugs defects/errors and enforced Spotless checks. The 10 Python publication tests and Node checks passed. Hosted CI run [40114364286](https://github.com/jaychang1989/smart-select-parameter-plugin/actions/runs/40114364286) passed Linux Java 21/25 and Windows Java 21. The [Jenkins security scan](https://github.com/jaychang1989/smart-select-parameter-plugin/actions/runs/40114364566) also passed.

The public baseline is Jenkins 2.555.3, with the required current parent POM, synchronized BOM, strict checks, maintained tests migrated to JUnit Jupiter, security scanning, dependency updates and future hosting-team CODEOWNERS. Existing deployed compatibility builds were not changed.

The first official run had 16 Required findings. Fourteen were code/configuration fixes; the two account first-login records became available after the maintainer logged in and the official reports synchronized. An initial Windows CI test issue involving temporary-path aliases was fixed and verified by the subsequent successful hosted run.

Human hosting review, canonical repository transfer and release permissions remain pending. Do not set hosting_approved merely because this issue is open or an automated check passed.

## Official bot result

The [official recheck](https://github.com/jenkins-infra/repository-permissions-updater/actions/runs/40142003370) completed successfully. The [bot comment](https://github.com/jenkins-infra/repository-permissions-updater/issues/5409#issuecomment-6390775484) confirms everything is in order for human review. The issue is open with hosting-request and bot-check-complete labels; needs-fix was removed. Human approval and release permissions remain pending.
