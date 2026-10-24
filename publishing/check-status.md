# Hosting check status (2026-10-25)

The maintainer repository is public. Official Jenkins hosting is not yet approved, and no update-center release has been published.

The official checker was built from jenkins-infra/repository-permissions-updater commit 84c6de2390972f1a6592ef0372e4d9e272780d1b and run using Java 25. Its first run reported 16 Required findings. This revision addresses the code/configuration findings: parent/minimum baseline, synchronized BOM, strict build checks, removal of obsolete developers metadata, canonical future homepage, ignored work directory, Jenkins security scanning and future hosting-team CODEOWNERS. Maintained tests now use JUnit Jupiter. Java 25 is declared in Jenkins CI, and the popup shadow is theme-aware.

Local verification passed all 37 Java/Jenkins tests without skips, zero SpotBugs defects/errors and enforced Spotless checks. The 10 Python source-publication tests and Node checks passed as well. Existing deployed compatibility builds were not modified.

The community identifier in the supplied account profile is jaychang1989. Jira and, for manual release, Artifactory first-login reports must include that identifier before the official application can truthfully attest that all Required findings are fixed. Reports can lag a successful login by about an hour. No password is stored in this repository.

A fresh official check must run against this pushed revision. Its result is not replaced by local build success, and hosted CI results must be read independently.
