# Smart Select Parameter（中文）

当前为上架准备副本，版本 `1.0.2-SNAPSHOT`，尚未公开仓库或发布到 Jenkins 插件中心。要求 Jenkins 2.547、Java 21。

支持下拉框单选/多选、名称和值分离、搜索、REST、参数联动及分页，无额外运行时插件和第三方前端库依赖。目前界面及部分配置/错误提示为简体中文，英文界面尚未完整实现。

## 快速开始

```groovy
smartSelect(name: 'SERVICES', mode: 'MULTI', searchable: true,
    options: [[value: 'price', label: '报价服务'], [value: 'admin', label: '管理服务']],
    defaultValues: ['price'])
```

页面显示名称，任务收到实际值，多选以英文逗号分隔。旧版 choices 字符串列表继续有效。

执行 `mvn -B -ntp clean verify` 后安装生成的 HPI；插件中心安装入口需等待官方托管和首次发布。

## REST 与联动

管理员先在系统配置中授权来源地址，再填写任务的接口地址、数组路径和可选令牌。授权覆盖该协议、主机和端口下的所有路径，仅应加入可信服务。HTTP 是明文传输，敏感接口请使用 HTTPS。令牌由 Jenkins 加密保存，不要放入公开源码。

```groovy
smartSelect(name: 'ENV', mode: 'SINGLE', choices: ['dev', 'release'])
smartSelect(name: 'SERVICES', mode: 'MULTI', dependsOn: 'ENV',
    restUrl: 'https://services.example.com/options?env=${ENV}',
    restItemsPath: 'data.items', cacheSeconds: 30)
```

使用自己的已授权接口替换示例。上级变化后清空旧值，完成读取后重新选择。首次非交互 Pipeline 的无上下文子参数默认值为空；CLI 请同时传入上下级值。接口故障不会回退静态值。

最多 2000 项，每页最多挂载 100 项，全选针对所有搜索结果。每个值和展示名称最多 256 字符；值不能含英文逗号或换行。

[完整说明](README.md) · [安全边界](SECURITY.md) · [上架准备与待办](publishing/README.md)
