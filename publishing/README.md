# Publication preparation

This is local source preparation, not approved hosting or a published plugin. GitHub lookup identified jaychang1989; the maintainer supplied Jenkins community identifier Jaychang1989. Only the identifier is stored, without any password or login verification. The repository was made public on 2026-10-25 at the maintainer's explicit request. Official hosting and an update-center release are still pending. The initial private upload skipped GitHub Actions; current CI results must be checked separately.

## Prepared

Naming/coordinates, explicit Maven source metadata, English/Chinese guides, security/contributor policies, examples, GitHub CI, a ci.jenkins.io entry point, a hosting draft and local publication/source-archive checks. Production Java/JavaScript behavior is preserved from the source snapshot. The main working directory and existing controllers are not modified by this preparation.

## Remaining gates

1. The maintainer explicitly authorized public disclosure on 2026-10-25 under the existing MIT license. This records the owner's instruction, not an independent legal assessment.
2. Confirm the supplied community identifier Jaychang1989 and activate its account email if required. Its identifier is recorded in maintainers.json; do not add any password/token. Official hosting checks will verify the account.
3. Public source is available at the repository URL in pom.xml. Keep the source, examples and documentation free of credentials and private controller records.
4. Run the official hosting checker on public source and fix Required findings. It reads GitHub rather than an unpublished local folder and requires Java 25+, Maven and GitHub authentication.
5. Submit the hosting request and address review, including baseline/LTS feedback.
6. After approval, change Maven metadata to the canonical jenkinsci repository, enable official CI and obtain release permissions before the first official release.

## Local checks

```bash
python scripts/publication_check.py
python -m unittest discover -s tests -v
node src/test/offline/widget-smoke.cjs
mvn -B -ntp clean verify
python scripts/build_source_archive.py --output ../smart-select-parameter-publication-source.zip
```

The checker with --release additionally requires a non-snapshot release version, rejects incomplete maintainer/ownership/hosting metadata and verifies that the repository is public. It is a local publishing guard, not the Jenkins hosting checker or a security/license certification. A snapshot source version is normal during preparation and must not be changed solely to bypass that guard.

After publication, run from a separate clone of jenkins-infra/repository-permissions-updater:

```bash
./bin/check-hosting.sh \
  --repository-url https://github.com/jaychang1989/smart-select-parameter-plugin \
  --new-repository-name smart-select-parameter-plugin \
  --github-user jaychang1989 \
  --jenkins-user Jaychang1989 \
  --no-cd
```

The initial plan uses manual release after permission is granted. --no-cd matches that choice. An optional CD template is stored outside the active workflow directory with a .yaml.disabled suffix. It needs Incrementals setup and a separate hosting-team-reviewed PR before enabling. Never store release tokens in this tree.

## Sources

- [Preparation](https://www.jenkins.io/doc/developer/publishing/preparation/)
- [Hosting](https://www.jenkins.io/doc/developer/publishing/requesting-hosting/)
- [Official checker](https://github.com/jenkins-infra/repository-permissions-updater#running-the-hosting-checks-locally)
- [Releases](https://www.jenkins.io/doc/developer/publishing/releasing/)
- [Optional CD](https://www.jenkins.io/doc/developer/publishing/releasing-cd/)

