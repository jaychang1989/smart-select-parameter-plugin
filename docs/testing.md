# Testing scope

Use Java 21 and Maven 3.9+. Jenkins tests start temporary local controllers and loopback fixtures, not a deployment controller. Node smoke checks cover selection, labels, 2,000 choices, page bounds, and keyboard navigation.

The suite includes form/API/CLI submission, the actual example Jenkinsfile, administrator-only origin changes, reload permissions, encrypted token persistence, no fallback, rejected redirects/large/malformed/slow responses, submitted-parent validation, cycles and a legacy V0.1 XML fixture.

Tokens secret-for-test, bearer-test-secret and local-test-token are intentional loopback test fixtures, not usable credentials. Do not replace them with real tokens.

GitHub CI is configured for Linux/Windows Java 21 and Linux Java 25. The initial private upload intentionally skips those jobs; remote CI has not been verified. The root Jenkinsfile is for ci.jenkins.io after hosting. Format checking is inherited as skipped and must not be reported as passed.

Publication checks detect recognizable leaks and unsafe source content, not every possible secret or vulnerability. They do not verify copyright ownership. Refer to the local preparation report for this run's actual results.

## Local preparation result (2026-10-24)

On Windows with Java 21 and Maven 3.9.11, the 1.0.2-SNAPSHOT preparation copy passed all 37 Java/Jenkins tests without skips and reported zero SpotBugs defects or analysis errors. The 10 Python publication-check tests and Node interaction/syntax checks also passed. All five prepared YAML files parsed successfully. A local static browser example submitted price,admin and produced the documentation screenshot; it did not connect to a controller.

Production Java, JavaScript and CSS match the source snapshot. A global-configuration help example was changed from a generic intranet hostname to a reserved example.com hostname; no network/validation logic was changed. Logs and machine-specific evidence remain outside the public source archive.

The official hosting checker and remote CI have not run because the proposed repository is not public. A community identifier was subsequently supplied by the maintainer, but has not been independently authenticated. Release-mode local checks intentionally fail on those incomplete metadata/approval gates and on the snapshot version. These pending results are not counted as passing tests.

