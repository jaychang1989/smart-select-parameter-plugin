# Smart Select Parameter 详细使用指南

这份指南从普通下拉框开始，逐步介绍搜索多选、接口选项和参数联动。完整脚本在 [examples 目录](../examples/README.md)，均只打印选择结果。安装及运行要求以所用版本的说明为准。

## 先选适合自己的方式

| 需要什么 | 使用方式 | 完整示例 |
| --- | --- | --- |
| 不写 Jenkinsfile，在任务页面配置 | 普通任务添加参数 | 场景 1 |
| 一次选择一个环境 | 单选 + 默认值 | [单选脚本](../examples/Jenkinsfile-single) |
| 一次选择多个服务 | 搜索多选，拆分结果 | [多选脚本](../examples/Jenkinsfile-multi) |
| 页面显示易读名称，脚本收到固定编号 | `options` 的 `label` 和 `value` | [名称和值分离](../examples/Jenkinsfile-labels) |
| 已有脚本式 Pipeline | `properties` 声明参数 | [脚本式 Pipeline](../examples/Jenkinsfile-scripted) |
| 选项经常变化，由接口维护 | REST 根数组或嵌套数组 | [根数组](../examples/Jenkinsfile-rest-independent)、[嵌套数组](../examples/Jenkinsfile-rest-nested) |
| 选择环境后只列出对应服务 | 两级联动 | [环境→服务](../examples/Jenkinsfile-rest) |
| 环境和地区一起决定服务 | 多个上级参数 | [双条件联动](../examples/Jenkinsfile-two-parents) |
| 先选环境，再选项目，最后选服务 | 多级联动 | [三级联动](../examples/Jenkinsfile-cascade) |
| 通过机器人或命令行发起构建 | API / CLI 提交实际值 | 场景 10 |

Pipeline 示例需要 Jenkins 安装 Pipeline。普通任务直接配置此参数时不需要 Pipeline。任务页面里的参数名称与脚本中 `params.参数名` 必须一致，名称区分大小写。

## 场景 1：不写代码，在普通任务里添加下拉框

进入任务 → **Configure / 配置** → 勾选 **This project is parameterized / 参数化构建** → **Add Parameter / 添加参数** → **Smart Select Parameter**。

以选择一个环境为例：

| 页面字段（英文 / 中文） | 填写内容 |
| --- | --- |
| Name / 参数名称 | `ENV` |
| Selection mode / 选择模式 | `SINGLE` |
| Choices / 选项列表 | 分三行填写 `dev`、`pre`、`release` |
| Default values / 默认选中项 | `pre` |
| Enable search / 启用搜索 | 选项较少时可关闭 |
| Display labels and values / 展示名称与实际值 | 留空 |
| REST options URL / REST 选项地址 | 留空 |

保存后，进入 **Build with Parameters / 参数化构建**，会默认选中 `pre`。构建步骤读取的 `ENV` 就是所选字符串。普通任务在 Linux shell 中可用 `$ENV`，Windows 批处理中可用 `%ENV%`；具体引用方式取决于所用构建步骤。

若要多选，将模式改为 `MULTI`，参数名改为 `SERVICES`，选项每行一个服务，例如 `price`、`vehicle`、`admin`。默认值填写 `price,vehicle`，构建收到的字符串也是 `price,vehicle`。

## 场景 2：Jenkinsfile 只选一个环境

复制 [完整单选脚本](../examples/Jenkinsfile-single)，核心配置是：

```groovy
smartSelect(name: 'ENV', mode: 'SINGLE', searchable: false,
    choices: ['dev', 'pre', 'release'], defaultValues: 'pre',
    description: 'Choose one environment')
```

这段声明放在声明式 Pipeline 的 `parameters { ... }` 内。默认值为 `pre`，选择 `release` 后，`params.ENV` 是字符串 `release`。独立单选参数没有设置默认值时，会采用第一个可用选项；显式配置的默认值必须存在于选项中。

新建 Pipeline 任务时，需要先执行一次 Jenkinsfile，让 Jenkins 登记参数，再刷新任务页面进入“参数化构建”。示例只打印结果；已有真实发布步骤的脚本在登记参数的这次运行中也可能执行，因此应先使用示例任务完成配置。

## 场景 3：搜索多选，并在构建中逐个读取

复制 [完整多选脚本](../examples/Jenkinsfile-multi)。声明：

```groovy
smartSelect(name: 'SERVICES', mode: 'MULTI', searchable: true,
    choices: ['price', 'vehicle', 'admin'],
    defaultValues: ['price', 'vehicle'])
```

选中 `price` 和 `vehicle` 后，`params.SERVICES` 是字符串 `price,vehicle`，不是数组。在 `steps { script { ... } }` 中这样读取：

```groovy
def selected = (params.SERVICES ?: '').tokenize(',')
if (selected.isEmpty()) {
    echo 'No services selected; skipping this example stage.'
} else {
    selected.each { service -> echo "Selected service: ${service}" }
}
```

多选可以为空，清空后收到空字符串；它不会自动要求用户至少选择一项。若业务必须选择服务，请在实际执行前增加检查，例如 `if (selected.isEmpty()) { error('Select at least one service') }`。

输入搜索词会过滤选项，“全选结果”选择全部匹配项。超过 100 项时，即使匹配项分布在多页，全选仍会覆盖所有匹配项。更换搜索词不会自动清空此前的选择；需要重选时先点击“清空”。

## 场景 4：显示中文服务名，提交固定服务编号

复制 [完整名称和值分离脚本](../examples/Jenkinsfile-labels)，将展示名称替换成自己的名称：

```groovy
smartSelect(name: 'SERVICES', mode: 'MULTI', searchable: true,
    options: [[value: 'price', label: '报价服务'],
              [value: 'vehicle', label: '车险服务'],
              [value: 'admin', label: '管理服务']],
    defaultValues: ['price'])
```

页面显示“报价服务”，构建收到 `price`。同时选中报价和管理时，收到 `price,admin`。搜索既匹配中文名称，也匹配实际值。界面语言切换不会翻译或改写你自己填写的选项名称。

普通任务在 **Display labels and values / 展示名称与实际值** 中填写 JSON：

```json
[{"value":"price","label":"报价服务"},{"value":"admin","label":"管理服务"}]
```

默认值填 `price`，不要填“报价服务”。填写了非空 `options` 后，它会代替普通 `choices`；两者不是合并关系。

## 场景 5：已有脚本式 Pipeline 怎么接入

声明式 Pipeline 使用 `parameters { ... }`；脚本式 Pipeline 使用下面的结构，完整文件见 [脚本式示例](../examples/Jenkinsfile-scripted)：

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

`properties(...)` 设置任务属性列表。合并到已有脚本时，要把需要保留的其他参数和任务属性一起保留，例如定时触发或构建保留策略。后续仍通过 `params.ENV`、`params.SERVICES` 读取结果。

## 场景 6：从接口读取选项，避免反复修改 Jenkinsfile

先由管理员进入 **Manage Jenkins → System → Smart Select REST source authorization / Smart Select REST 数据源授权**，添加可信接口的来源，例如 `https://services.example.com`。来源是协议、主机和端口，不包含 `/services` 这样的路径；每行一个。

接口必须由 Jenkins 控制器能够访问，并返回 HTTP 200 和 JSON 内容类型。下列域名只是示例，使用前必须替换为自己的接口并完成来源授权。

### 6.1 接口直接返回字符串数组

响应：

```json
["price", "vehicle", "admin"]
```

配置放进 `parameters`，完整文件见 [REST 根数组示例](../examples/Jenkinsfile-rest-independent)：

```groovy
smartSelect(name: 'SERVICES', mode: 'MULTI', searchable: true,
    restUrl: 'https://services.example.com/services', cacheSeconds: 30)
```

数组在最外层，所以 `restItemsPath` 留空。数组也可以由 `value`、`label` 对象组成，写法与场景 4 相同。

### 6.2 接口把数组放在返回对象中

响应：

```json
{"data":{"items":[{"value":"price","label":"报价服务"},{"value":"admin","label":"管理服务"}]}}
```

完整文件见 [REST 嵌套数组示例](../examples/Jenkinsfile-rest-nested)：

```groovy
smartSelect(name: 'SERVICES', mode: 'MULTI', searchable: true,
    restUrl: 'https://services.example.com/service-catalog',
    restItemsPath: 'data.items', cacheSeconds: 0)
```

`data.items` 表示从对象的 `data` 中取 `items` 数组，不需要 `$` 或 `[*]`。值必须是字符串，例如 `"1001"`，不能直接返回数字 `1001`。

### 6.3 缓存、认证和刷新怎么设置

| 设置 | 效果 |
| --- | --- |
| `cacheSeconds: 30` | 默认缓存 30 秒；短时间重复读取可使用缓存 |
| `cacheSeconds: 0` | 不复用有效缓存，每次需要取数时重新请求 |
| 页面点击 Reload options / 重新读取选项 | 跳过当前参数缓存，重新读取 |
| Bearer token / Bearer 令牌 | 可在任务配置页填写；Jenkins 加密保存并在控制器请求时使用 |

允许的缓存范围是 0–300 秒。接口故障不会回退到 `choices`；有未过期缓存时仍可使用缓存，过期后的读取失败会阻止提交。数据源优先级为 **REST 地址 → 非空 options → choices**。

不要把真实令牌写进 Jenkinsfile。Pipeline 重新声明同名参数可能覆盖页面中配置的参数及令牌，需要认证时应统一管理参数配置；仅在页面填一次令牌、随后让 Jenkinsfile 每次重建同名参数，并不是可靠的保存方式。

## 场景 7：先选环境，再显示该环境的服务

完整脚本见 [环境→服务](../examples/Jenkinsfile-rest)。核心配置：

```groovy
smartSelect(name: 'ENV', mode: 'SINGLE',
    choices: ['dev', 'pre'], defaultValues: 'pre')
smartSelect(name: 'SERVICES', mode: 'MULTI', dependsOn: 'ENV',
    restUrl: 'https://services.example.com/options?env=${ENV}',
    restItemsPath: 'data.items', cacheSeconds: 30)
```

选择 `pre` 时，插件请求 `https://services.example.com/options?env=pre`；接口返回的 `data.items` 就是此时可选的服务。改选 `dev` 后，旧服务选择会清空，并重新读取开发环境的服务。

REST URL 使用 **Groovy 单引号**，保留 `${ENV}` 给插件处理。不能用 `${ENV}` 动态改变域名或路径；占位符用于查询值，插件会对值进行 URL 编码。每个 `dependsOn` 中声明的名称都要有对应占位符。

页面首次读取可应用有效默认值；切换环境后需要重新选择子项。联动参数在首次没有上级请求上下文的 Pipeline 运行中没有子参数默认值，因此应先登记参数，再从构建页面或 API 提交选择。

## 场景 8：两个条件一起筛选服务

例如环境和地区共同决定服务，完整文件见 [双条件联动](../examples/Jenkinsfile-two-parents)：

```groovy
smartSelect(name: 'ENV', mode: 'SINGLE', choices: ['dev', 'pre'])
smartSelect(name: 'REGION', mode: 'SINGLE', choices: ['north', 'south'])
smartSelect(name: 'SERVICES', mode: 'MULTI', dependsOn: 'ENV,REGION',
    restUrl: 'https://services.example.com/services?env=${ENV}&region=${REGION}',
    restItemsPath: 'data.items')
```

`ENV=pre`、`REGION=north` 时，接口查询值为 `env=pre&region=north`。任一上级变化，都需要重新选择服务。多个上级名称用英文逗号分隔，最多 8 个。

## 场景 9：环境 → 项目 → 服务，三级联动

完整文件见 [三级联动](../examples/Jenkinsfile-cascade)。先声明静态环境，再按环境读取项目，最后按环境和项目读取服务：

```groovy
smartSelect(name: 'ENV', mode: 'SINGLE', choices: ['dev', 'pre'])
smartSelect(name: 'PROJECT', mode: 'SINGLE', dependsOn: 'ENV',
    restUrl: 'https://services.example.com/projects?env=${ENV}')
smartSelect(name: 'SERVICES', mode: 'MULTI', dependsOn: 'ENV,PROJECT',
    restUrl: 'https://services.example.com/services?env=${ENV}&project=${PROJECT}')
```

这里两个接口均返回根数组，所以不填写 `restItemsPath`。服务接口同时接收环境和项目，避免不同环境中相同项目编号导致歧义。改变环境后，项目及服务会重新加载；加载过程中、上级未选择或读取失败时，页面暂停提交。不要让参数循环依赖，例如 A 依赖 B、B 又依赖 A。

## 场景 10：通过 API 或命令行提交

先让任务登记好参数。以下为 Bash 示例，使用 Jenkins API 令牌认证；将任务地址、账号及令牌占位内容替换为自己的设置：

```bash
curl --user 'USER:API_TOKEN' --request POST \
  'https://jenkins.example.com/job/example/buildWithParameters' \
  --data-urlencode 'ENV=pre' \
  --data-urlencode 'SERVICES=price,vehicle'
```

Windows 上也可使用 PowerShell 7；弹出的凭据窗口中，密码一栏填写 Jenkins API 令牌：

```powershell
$credential = Get-Credential -Message 'Jenkins username; use API token as password'
Invoke-WebRequest -Uri 'https://jenkins.example.com/job/example/buildWithParameters' `
  -Method Post -Authentication Basic -Credential $credential `
  -Body @{ ENV = 'pre'; SERVICES = 'price,vehicle' }
```

Jenkins CLI 使用下面的命令：

```bash
java -jar jenkins-cli.jar -s https://jenkins.example.com/ \
  -auth @/path/to/jenkins-auth.txt build example \
  -p ENV=pre -p SERVICES=price,vehicle -s
```

认证文件内容为 `USER:API_TOKEN`，应只允许本人读取，详见 [Jenkins 官方 CLI 说明](https://www.jenkins.io/doc/book/managing/cli/)。命令会发起构建；示例任务仅打印结果，真实任务会执行自己的构建步骤。Jenkins 权限和 CSRF 要求仍然适用。

提交 **value**，不要提交展示名称，也不要把多选值写成 JSON 数组。同一参数只提交一次，值之间用英文逗号分隔。联动时显式传入上下级值；三级示例还需提供 `PROJECT`。服务端会按本次上级值重新校验，不能提交另一个环境的服务。

## 常见问题与限制

| 现象 / 问题 | 检查方法 |
| --- | --- |
| 新任务没有“参数化构建”按钮 | 先运行 Jenkinsfile 登记参数，再刷新任务页面 |
| 中文名称显示了，脚本里却是英文编号 | 这是 label/value 分离的预期行为；脚本读取 value |
| 清空多选后仍继续执行任务 | 多选允许为空；业务必填时在执行前增加非空检查 |
| 接口没有选项 | 检查控制器可达性、来源授权、HTTP 200、JSON 内容类型及数组路径 |
| 明明接口正常，仍提示选项不可用 | 检查默认值是否还存在、是否重复 value 对应不同名称、是否超过长度或数量限制 |
| 换环境后原来选的服务消失 | 联动会清空旧选择，防止错误沿用另一个环境的服务 |
| 修改接口后页面仍是旧内容 | 点击重新读取；必要时减少缓存时间 |
| 默认值不生效 | 使用 value；单选只填一个；静态值或独立接口默认值必须属于可选集合 |
| 选项很多，担心全选只选当前页 | 搜索和全选覆盖全部匹配项，分页每页仅挂载最多 100 个按钮 |
| 选项值可否包含逗号 | 不可。多选结果用英文逗号分隔；应改用不含逗号的编号作为 value |

每个参数最多 2,000 个选项；每个值和名称最多 256 字符。值不能含英文逗号或换行，名称可以含逗号但不能换行。联动最多声明 8 个直接上级，依赖链有循环和深度检查。JSON 数组路径支持点分对象字段，不执行 JSONPath 表达式。

[返回说明首页](../README.zh-CN.md) · [全部完整脚本](../examples/README.md) · [English guide](usage-examples.md)
