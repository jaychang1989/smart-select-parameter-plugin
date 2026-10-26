# Smart Select Parameter usage guide

Start with static choices, then add labels, REST sources and cascading parameters as needed. The [example index](../examples/README.md) links to complete Jenkinsfiles. All examples only print selected values. Consult the development guide for the requirements of the source version you are using.

| Goal | Example |
| --- | --- |
| Configure a Freestyle job without a Jenkinsfile | Scenario 1 below |
| Select one environment | [Single selection](../examples/Jenkinsfile-single) |
| Select several services and handle empty selections | [Multiple selection](../examples/Jenkinsfile-multi) |
| Show friendly names while submitting stable identifiers | [Labels and values](../examples/Jenkinsfile-labels) |
| Add parameters to a Scripted Pipeline | [Scripted Pipeline](../examples/Jenkinsfile-scripted) |
| Maintain choices through an API | [Root array](../examples/Jenkinsfile-rest-independent), [nested array](../examples/Jenkinsfile-rest-nested) |
| Filter services by environment | [Two-level cascade](../examples/Jenkinsfile-rest) |
| Filter services by environment and region | [Two parents](../examples/Jenkinsfile-two-parents) |
| Select environment, project, then services | [Three-level cascade](../examples/Jenkinsfile-cascade) |
| Submit values from another tool | Scenario 10 below |

Pipeline examples require Pipeline plugins. Freestyle jobs can use this parameter without Pipeline. Parameter names are case-sensitive: `name: 'SERVICES'` corresponds to `params.SERVICES`.

## 1. Configure a Freestyle job in the UI

Open the job's **Configure** page, select **This project is parameterized**, then choose **Add Parameter → Smart Select Parameter**.

| Field | Example setting |
| --- | --- |
| Name | `ENV` |
| Selection mode | `SINGLE` |
| Choices | `dev`, `pre`, `release`, each on its own line |
| Default values | `pre` |
| Enable search | Disable for this short list if desired |
| Display labels and values | Empty |
| REST options URL | Empty |

Save the job and open **Build with Parameters**. `pre` is initially selected. The build receives the selected string through the `ENV` parameter. Freestyle shell steps can read `$ENV` on Unix or `%ENV%` in Windows batch; the exact syntax depends on the build step.

For multiple services, change the name to `SERVICES`, select `MULTI`, and put `price`, `vehicle`, and `admin` on separate lines. Defaults such as `price,vehicle` produce a comma-separated string in the build.

## 2. Select one environment in a Jenkinsfile

Use [the complete single-select example](../examples/Jenkinsfile-single). The parameter declaration goes inside Declarative Pipeline's `parameters { ... }` block:

```groovy
smartSelect(name: 'ENV', mode: 'SINGLE', searchable: false,
    choices: ['dev', 'pre', 'release'], defaultValues: 'pre',
    description: 'Choose one environment')
```

The default is `pre`; selecting `release` makes `params.ENV` equal to the string `release`. An independent single-select parameter without an explicit default uses its first available choice. An explicit default must be a valid choice.

A new Pipeline job must run the Jenkinsfile once to register parameters. Refresh the job page and use **Build with Parameters** for subsequent selections. These examples only echo values. If you add real deployment stages, those stages may also execute during the registration run; try the examples in a demonstration job first.

## 3. Select several services and consume the result

[The complete multi-select example](../examples/Jenkinsfile-multi) declares:

```groovy
smartSelect(name: 'SERVICES', mode: 'MULTI', searchable: true,
    choices: ['price', 'vehicle', 'admin'], defaultValues: ['price', 'vehicle'])
```

Selecting `price` and `vehicle` returns the **string** `price,vehicle`. Convert it inside `steps { script { ... } }`:

```groovy
def selected = (params.SERVICES ?: '').tokenize(',')
if (selected.isEmpty()) {
    echo 'No services selected; skipping this example stage.'
} else {
    selected.each { service -> echo "Selected service: ${service}" }
}
```

Multi-select allows an empty selection and returns an empty string after clearing. If a selection is required by your workflow, enforce it before doing work, for example: `if (selected.isEmpty()) { error('Select at least one service') }`.

Search filters the list, and **Select all results** selects all matching items across pages. Changing the search text does not clear earlier selections. Use **Clear** before replacing a selection if necessary.

## 4. Show labels, submit identifiers

Use [the label/value example](../examples/Jenkinsfile-labels):

```groovy
smartSelect(name: 'SERVICES', mode: 'MULTI', searchable: true,
    options: [[value: 'price', label: 'Quote service'],
              [value: 'vehicle', label: 'Vehicle service'],
              [value: 'admin', label: 'Administration service']],
    defaultValues: ['price'])
```

Users see `Quote service`; the build receives `price`. Selecting it together with `Administration service` returns `price,admin`. Search matches labels and values. Labels may be in your preferred language and are not rewritten when the UI locale changes.

For a Freestyle job, paste this into **Display labels and values**:

```json
[{"value":"price","label":"Quote service"},{"value":"admin","label":"Administration service"}]
```

Defaults use `price`, not `Quote service`. A nonempty `options` list replaces ordinary `choices`; the two sources are not merged.

## 5. Add parameters to a Scripted Pipeline

Use [the complete Scripted example](../examples/Jenkinsfile-scripted):

```groovy
properties([
    parameters([
        smartSelect(name: 'ENV', mode: 'SINGLE',
            choices: ['dev', 'pre', 'release'], defaultValues: 'pre'),
        smartSelect(name: 'SERVICES', mode: 'MULTI',
            choices: ['price', 'vehicle', 'admin'])
    ])
])
echo "ENV=${params.ENV}; SERVICES=${params.SERVICES}"
```

`properties(...)` sets the job's property list. When incorporating it into an existing Pipeline, preserve the other parameters and job properties you need, such as triggers and build retention. Read results through `params` as in Declarative Pipeline.

## 6. Load choices from a REST source

An administrator must first approve the source in **Manage Jenkins → System → Smart Select REST source authorization**. Enter one origin per line, for example `https://services.example.com`, without a path such as `/services`. Approving an origin allows all its paths; approve only trusted services.

The Jenkins controller must be able to reach the endpoint. Responses must use HTTP 200 and a JSON content type. Replace all example URLs and authorize your actual origin before using remote examples, including during parameter registration.

### Root array

Response:

```json
["price", "vehicle", "admin"]
```

Parameter declaration, with [a complete Jenkinsfile](../examples/Jenkinsfile-rest-independent):

```groovy
smartSelect(name: 'SERVICES', mode: 'MULTI', searchable: true,
    restUrl: 'https://services.example.com/services', cacheSeconds: 30)
```

Leave `restItemsPath` empty. A root array may also contain `value`/`label` objects as in scenario 4.

### Nested array

Response:

```json
{"data":{"items":[{"value":"price","label":"Quote service"},{"value":"admin","label":"Administration service"}]}}
```

Parameter declaration, with [a complete Jenkinsfile](../examples/Jenkinsfile-rest-nested):

```groovy
smartSelect(name: 'SERVICES', mode: 'MULTI', searchable: true,
    restUrl: 'https://services.example.com/service-catalog',
    restItemsPath: 'data.items', cacheSeconds: 0)
```

`data.items` selects the `items` array inside the `data` object. Do not use `$` or `[*]`; JSONPath is not evaluated. Values must be strings, so use `"1001"` rather than the number `1001`.

### Caching, authentication and reload

| Setting | Behavior |
| --- | --- |
| `cacheSeconds: 30` | The default; an unexpired cached response may be reused |
| `cacheSeconds: 0` | Do not reuse an unexpired cached response |
| **Reload options** | Bypass this parameter's cache |
| **Bearer token** | Optional job-configuration field; encrypted by Jenkins and used only on the controller |

The allowed cache range is 0–300 seconds. A source failure does not fall back to `choices`. Valid cached data may still be used; an expired cache plus a failed read blocks submission. Source precedence is **REST URL → nonempty options → choices**.

Never put real bearer tokens in a Jenkinsfile. Re-declaring the same parameter in a Pipeline can overwrite UI-managed settings, including the token. Authenticated sources need a consistent configuration-management approach; setting the token once in the UI while recreating that parameter on every run is not reliable.

## 7. Filter services by environment

See [the complete environment/services example](../examples/Jenkinsfile-rest). The essential declaration is:

```groovy
smartSelect(name: 'ENV', mode: 'SINGLE', choices: ['dev', 'pre'], defaultValues: 'pre')
smartSelect(name: 'SERVICES', mode: 'MULTI', dependsOn: 'ENV',
    restUrl: 'https://services.example.com/options?env=${ENV}',
    restItemsPath: 'data.items', cacheSeconds: 30)
```

Selecting `pre` requests `https://services.example.com/options?env=pre`. The response's `data.items` supplies the allowed services. Changing to `dev` clears the old selection and loads services for that environment.

Keep placeholder URLs **single-quoted in Groovy** so Smart Select, rather than Groovy, expands `${ENV}`. Placeholders belong in query values, not the host or path, and values are URL-encoded. Every declared parent needs a corresponding placeholder.

The initial browser load can apply valid defaults; changing a parent requires a new child selection. Dependent parameters have no context-free child defaults on a first noninteractive Pipeline run. Register the parameters first, then submit selections through the form or API.

## 8. Filter by two parents

[The complete two-parent example](../examples/Jenkinsfile-two-parents) combines environment and region:

```groovy
smartSelect(name: 'ENV', mode: 'SINGLE', choices: ['dev', 'pre'])
smartSelect(name: 'REGION', mode: 'SINGLE', choices: ['north', 'south'])
smartSelect(name: 'SERVICES', mode: 'MULTI', dependsOn: 'ENV,REGION',
    restUrl: 'https://services.example.com/services?env=${ENV}&region=${REGION}',
    restItemsPath: 'data.items')
```

With `ENV=pre` and `REGION=north`, the query is `env=pre&region=north`. Either parent changing requires a new service selection. Separate parent names with commas; at most eight direct parents are supported.

## 9. Select environment, project, then services

See [the complete three-level example](../examples/Jenkinsfile-cascade):

```groovy
smartSelect(name: 'ENV', mode: 'SINGLE', choices: ['dev', 'pre'])
smartSelect(name: 'PROJECT', mode: 'SINGLE', dependsOn: 'ENV',
    restUrl: 'https://services.example.com/projects?env=${ENV}')
smartSelect(name: 'SERVICES', mode: 'MULTI', dependsOn: 'ENV,PROJECT',
    restUrl: 'https://services.example.com/services?env=${ENV}&project=${PROJECT}')
```

Both endpoints return root arrays, so no array path is configured. The service endpoint receives environment and project to avoid ambiguity if project identifiers repeat across environments. Changing the environment reloads projects and services. Missing parents, loading and errors pause browser submission. Cycles such as A depending on B while B depends on A are rejected.

## 10. Submit selections through API or CLI

Register the parameters first. These are Bash examples; replace the job URL, username and API token placeholders. Use Jenkins API-token authentication:

```bash
curl --user 'USER:API_TOKEN' --request POST \
  'https://jenkins.example.com/job/example/buildWithParameters' \
  --data-urlencode 'ENV=pre' \
  --data-urlencode 'SERVICES=price,vehicle'
```

On Windows, PowerShell 7 can also submit the parameters. In the credential prompt, use your Jenkins API token as the password:

```powershell
$credential = Get-Credential -Message 'Jenkins username; use API token as password'
Invoke-WebRequest -Uri 'https://jenkins.example.com/job/example/buildWithParameters' `
  -Method Post -Authentication Basic -Credential $credential `
  -Body @{ ENV = 'pre'; SERVICES = 'price,vehicle' }
```

For Jenkins CLI, use:

```bash
java -jar jenkins-cli.jar -s https://jenkins.example.com/ \
  -auth @/path/to/jenkins-auth.txt build example \
  -p ENV=pre -p SERVICES=price,vehicle -s
```

The authentication file contains `USER:API_TOKEN` and should only be readable by its owner. See the [official CLI documentation](https://www.jenkins.io/doc/book/managing/cli/). These commands trigger a build: example jobs only print values; real jobs execute their own build steps. Jenkins permissions and CSRF requirements still apply.

Send **values**, not labels or a JSON array. Submit each parameter once with comma-separated values. Explicitly provide parent and child values for cascades; the three-level example also needs `PROJECT`. The controller validates the actual submitted parent context, so services from a different environment are rejected.

## Troubleshooting and limits

| Symptom | What to check |
| --- | --- |
| A new Pipeline has no parameterized build page | Run its Jenkinsfile once to register parameters, then refresh |
| A friendly label becomes an identifier in the build | Expected: builds receive `value`, not `label` |
| Clearing multi-select still allows the job to run | Empty multi-select is allowed; enforce business-required selections before work |
| A remote list is unavailable | Controller connectivity, approved origin, HTTP 200, JSON content type, array path |
| A healthy endpoint still produces an error | Defaults must be valid; conflicting labels, value lengths and option limits are also checked |
| Services disappear after changing environment | Expected: old selections are cleared to avoid crossing contexts |
| Updated source data appears stale | Reload options or reduce cache lifetime |
| A default does not apply | Use actual values; one value for single-select; defaults must belong to the available choices |
| Many options span several pages | Search/select-all cover all matching choices, not just the current page |
| A value contains a comma | Use an identifier without commas, since commas delimit submitted values |

At most 2,000 options per parameter and 256 characters per label/value are supported. Values cannot contain commas or line breaks. Labels may contain commas but cannot contain line breaks. Cascades allow up to eight direct parents and enforce cycle/depth limits. Array paths use dotted object keys, not JSONPath expressions.

[Main README](../README.md) · [All complete examples](../examples/README.md) · [中文指南](usage-examples.zh-CN.md)
