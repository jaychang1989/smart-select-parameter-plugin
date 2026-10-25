# Hosting request — submitted

Submitted application: https://github.com/jenkins-infra/repository-permissions-updater/issues/5409

Official hosting approval is pending. The draft below records the request context.

## Repository URL

https://github.com/jaychang1989/smart-select-parameter-plugin

This source repository is public. The maintainer authorized public disclosure on 2026-10-25. Official hosting approval is still pending.

## New Repository Name

smart-select-parameter-plugin

## Description

Smart Select Parameter offers compact searchable single/multiple selection, separate labels and values, optional administrator-approved REST GET sources and validated cascades. It uses no third-party frontend library and declares no additional runtime plugin dependency. Form/API/CLI submissions validate actual parent context on the controller. List rendering is paged; select-all includes all matching choices.

The scope is data-only choices, rather than a general Groovy/rich-HTML parameter engine. Values remain ordinary strings. English is the default for the complete UI; Simplified Chinese translations follow the Jenkins request locale. SECURITY.md documents important trust boundaries.

## GitHub and Jenkins project users

GitHub / Jenkins project users: @jaychang1989 jaychang1989

The Jenkins community identifier was supplied by the maintainer and has not been independently authenticated during this preparation. No password is stored here.

## Coordinates

- Artifact ID: smart-select-parameter
- Group ID: io.jenkins.plugins
- Name: Smart Select Parameter
- Proposed canonical repository: jenkinsci/smart-select-parameter-plugin, after approval only
- License: MIT; maintainer authorized publication on 2026-10-25

## Related plugins and distinction

[Active Choices](https://plugins.jenkins.io/uno-choice/) supports Groovy/Scriptler-generated reactive parameters and richer rendering. This component instead focuses on compact static/JSON sources and controller-side validation of submitted parent context. It is not a replacement for arbitrary scripting or rich HTML.

[Extended Choice Parameter](https://plugins.jenkins.io/extended-choice-parameter/) is marked deprecated by its maintainers. This component provides a smaller dropdown/REST model rather than its broader script/file-input features. Other alternatives can overlap; acceptance as a separate component is for the hosting team to assess.

## Validation

Attach only actual local results. Configured CI is not evidence that hosted jobs have run. After pushing public source, run the official hosting checker and resolve Required findings.

[Official hosting form](https://github.com/jenkins-infra/repository-permissions-updater/issues/new?template=1-hosting-request.yml)

