# Contributing

The proposed repository URL is prefilled in pom.xml. Public creation and official hosting are still pending.

Use Java 21, Maven 3.9+, Node.js 24 and Python 3.12. Tests start isolated Jenkins instances and loopback fixtures. Do not require private controller access or real deployment credentials.

Before a change, run the commands in the README. Add regression coverage for changed validation, permissions, REST authorization, token handling or dependency resolution. Preserve old choices configurations and comma-separated output, or document an explicit migration.

Describe user-visible behavior, limitations and actual validation. Use sanitized reproductions. Never attach a real controller configuration, credential, private endpoint, webhook URL or user-home path. Report vulnerabilities privately through SECURITY.md.

Maintain English and Chinese documentation together. Chinese labels are intentionally covered by tests. The current UI is predominantly Simplified Chinese; localization contributions must preserve text escaping, focus and keyboard behavior.

Format checking is currently skipped by inherited configuration; do not report it as passed. Maven verification does run tests and SpotBugs.

[Jenkins Code of Conduct](https://www.jenkins.io/project/conduct/) applies to Jenkins-hosted contributions. Contributors must have the right to share their changes under the MIT license.
