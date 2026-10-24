# Contributing

Source is public in the maintainer repository. The Maven homepage names the intended Jenkins-hosted repository, as required by hosting checks; that hosting is still pending.

Use Java 21, Maven 3.9+, Node.js 24 and Python 3.12. Tests start isolated Jenkins instances and loopback fixtures. Do not require private controller access or real deployment credentials.

Before a change, run the commands in the README. Add regression coverage for changed validation, permissions, REST authorization, token handling or dependency resolution. Preserve old choices configurations and comma-separated output, or document an explicit migration.

Describe user-visible behavior, limitations and actual validation. Use sanitized reproductions. Never attach a real controller configuration, credential, private endpoint, webhook URL or user-home path. Report vulnerabilities privately through SECURITY.md.

Maintain English and Chinese documentation together. Chinese labels are intentionally covered by tests. The current UI is predominantly Simplified Chinese; localization contributions must preserve text escaping, focus and keyboard behavior.

Formatting is enforced by Spotless. Run mvn spotless:apply when needed, then mvn clean verify; do not disable required checks to make a build pass. The build also enforces strict bundling and bans legacy JUnit, servlet/Stapler and commons-lang imports.

[Jenkins Code of Conduct](https://www.jenkins.io/project/conduct/) applies to Jenkins-hosted contributions. Contributors must have the right to share their changes under the MIT license.
