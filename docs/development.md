# Development and local testing

The current source baseline is Jenkins 2.555.3 with Java 21 or a newer Java version supported by the selected controller. Development checks use Maven 3.9+, Node.js 24 and Python 3.12. Official plugin-site metadata will describe requirements for published versions after hosting and a first release.

Run `mvn -B -ntp clean verify` to build and test. For a disposable development controller, install the generated `target/smart-select-parameter.hpi` through Manage Jenkins -> Plugins -> Advanced settings -> Deploy Plugin, then follow the controller's activation/restart instructions. This is local development guidance, not a claim that the component is available from the update center.

Tests start isolated temporary controllers and loopback fixtures. Never point these tests at an existing deployment controller or insert real tokens. The source baseline change does not upgrade or replace existing deployed compatibility builds.

Use `mvn spotless:apply` when formatting changes are needed, and rerun verification. Relevant commands and CI evidence are listed in the main README and testing guide.
