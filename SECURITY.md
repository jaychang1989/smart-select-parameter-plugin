# Security policy and boundaries

## Reporting

Do not report exploits or credentials in public issues. Use the [Jenkins private security reporting process](https://www.jenkins.io/security/reporting/) and make the component's hosting status clear. This preparation does not claim hosting approval, a separate private maintainer inbox or an independent security certification.

## Trust model

- REST GET runs on the controller. No origins are approved by default. Only an administrator can approve exact scheme/host/effective-port origins. Approval includes all paths and any private-network target deliberately authorized by that administrator. This is not a blanket prohibition on private addresses.
- Job configurators choose paths, data paths and tokens within approved origins. Treat configurators of authenticated sources as trusted to use that token; do not share a sensitive origin with untrusted configurators.
- HTTP is supported but plaintext. Prefer HTTPS. Embedded URL credentials, fragments and redirects are rejected. Jenkins proxy settings and default TLS verification are retained.
- Bearer tokens use Jenkins Secret and are not returned to the build-selection page or options response. This does not create an independent secrets vault. Privileged configuration readers and administrators remain inside the Jenkins trust boundary. Pipeline re-declaration can replace UI-managed settings.
- The reload endpoint requires POST and job Build permission. Jenkins CSRF protection still applies, including its own API-token exemptions. Origin changes require Administer.
- Submitted form/API/CLI selections and actual cascade parent context are validated on the controller. Cycles are rejected and only query substitutions are supported. Sensitive/complex parent objects are rejected.
- Labels are escaped by Jelly or assigned through textContent, not evaluated as HTML or Groovy.

The origin allowlist limits controller-side requests made by this plugin when job configurators can edit parameters but do not have administrator approval to contact arbitrary origins. It is not a Jenkins-wide network sandbox: users who can run unrestricted Pipelines may make their own network requests, including through an input step. The control therefore protects this particular controller-side fetch path and its credentials, not against a trusted Pipeline author. Administrators must balance that boundary against the operational cost of approving new origins.

## Limits and failures

Bodies are capped at 1 MiB and 2,000 validated choices. Connect/read timeouts are 3 seconds each, plus a 5-second body-reading deadline. The browser stops waiting after 10 seconds. These are per-request limits, not a guaranteed total deadline across all dependency levels.

A parameter caches one recent context for up to 300 seconds. Cached data may remain valid during upstream changes inside that interval. Manual reload bypasses that parameter's cache. Revoking an origin prevents subsequent cache access. Expired fetch failures never fall back to static choices.

Stale browser responses are ignored and unavailable options pause browser submission. Client controls are not the security authority: controller validation is still required. Values injected directly through internal scheduling APIs are outside the public form/API/CLI boundary.

## Review scope

Publication checks cover recognizable secret/private-address patterns and distribution content, and run the existing regression suite. They cannot prove the absence of every possible secret, validate copyright ownership or substitute for an independent audit. Pending public-repository and official-hosting checks are listed in publishing/README.md.
