# GitHub 推送拦截问题复盘报告

记录日期：2026 年 11 月 13 日  
项目：SmartAgent  
处理结果：用户反馈“可以了”，本次推送受阻问题已解决。

## 1. 问题现象

向 GitHub 仓库 `liguoyao/SmartAgent` 的 `main` 分支推送时，本地对象打包、上传以及远端增量解析均完成，但远端最终拒绝更新分支。

执行的核心命令：

```powershell
git push origin main
```

关键错误：

```text
remote: error: GH013: Repository rule violations found for refs/heads/main.
remote: - GITHUB PUSH PROTECTION
remote: - Push cannot contain secrets
remote: —— DeepSeek API Key ——
remote: commit: 683e7ef923729e360ea9007452b8b8a426fec0d0
remote: path: ai-service/src/main/resources/application.yml:24
```

## 2. 原因定位

GitHub 在待推送提交中识别到 DeepSeek API Key，触发了密钥推送保护。这是远端安全规则拦截，与 Nacos 配置加载无关，也不是此次日志所显示的网络连接、Git 认证或分支冲突问题。

本地检查确认：

- 报错中的 `683e7ef` 是当时本地最新提交。
- 本地 `origin/main..HEAD` 查询显示一个待推送提交，即 `683e7ef`。
- 被指出的配置文件包含 `api-key` 配置项。

本报告不收录密钥原文。

## 3. 排查与处理过程

| 阶段 | 发现或操作 | 结果与判断 |
| --- | --- | --- |
| 读取推送日志 | 查看远端最终拒绝原因 | 确认错误为 GH013，命中 DeepSeek API Key 检测 |
| 检查本地状态与提交 | 查看最新提交及待推送提交 | 确认问题提交为当时的最新提交 |
| 提出移除密钥方案 | 建议改为环境变量，并使用 `git commit --amend --no-edit` 替换最新提交 | 该方案已提供，未在本次会话中执行 |
| 按用户要求调整方向 | 用户要求关闭 GitHub 检测 | 转向推送保护设置与放行方式 |
| 再次排查 | 用户反馈“还是被拒” | 提示检查个人账号级推送保护，并提供报错中的单次放行入口 |
| 结果确认 | 用户反馈“可以了” | 本次阻塞解除 |

关闭仓库级保护后仍可能受到个人账号级保护影响，因此需要区分两类设置。但本次会话没有读取 GitHub 实际设置，不能据此断言第二次拒绝一定由个人账号级保护造成。

## 4. 本次提供的解决路径

### 路径一：检查个人账号级推送保护

进入 GitHub 个人安全设置：

[https://github.com/settings/security_analysis](https://github.com/settings/security_analysis)

找到 `Push protection for yourself`，按需选择 `Disable`，之后重新推送：

```powershell
git push origin main
```

官方说明：[Managing push protection for users](https://docs.github.com/en/code-security/how-tos/secure-your-secrets/prevent-future-leaks/manage-user-push-protection)

### 路径二：对本次检测进行单次放行

打开 GitHub 拒绝日志中提供的 `unblock-secret` 链接，按页面提示完成放行后，再重新推送。具体放行链接参见原始日志，本报告不重复收录。

本次最终确认来自用户反馈；用户未说明采用上述哪一种操作，也可能组合使用，因此不将某一种路径记录为已验证的唯一解决方式。

## 5. 结果与验证范围

- 已确认：用户反馈本次问题已解决。
- 未独立验证：GitHub 当前保护设置、远端 `main` 的最终提交，以及实际采用的放行方式。
- 未执行：修改项目配置、改写 Git 提交、替用户推送或调整 GitHub 设置。
- 未确认：密钥是否已移除、撤销或轮换。

本次解决的是推送拦截；关闭保护或放行不会删除提交中的密钥。

## 6. 后续配置建议

若该值为有效密钥，建议撤销并重新生成，通过运行环境或受控配置中心注入。配置文件可保留环境变量引用：

```yaml
api-key: ${DEEPSEEK_API_KEY}
```

若选择从尚未推送的最新提交中移除密钥，需要替换该提交；仅追加一个删除密钥的新提交，先前提交中的密钥仍会包含在待推送历史中。这是最初建议使用 `git commit --amend --no-edit` 的原因。

## 7. 排查经验

1. 推送日志中的对象上传完成不代表分支更新成功，应以末尾远端接收结果为准。
2. 出现 `GH013` 时，应继续阅读具体规则名称。本次的直接证据是 `Push cannot contain secrets` 及其文件、行号、提交位置。
3. 保护设置应区分仓库级与个人账号级；调整一个入口不一定覆盖其他保护来源。
4. 排障记录需要区分“建议的操作”“实际执行的操作”和“用户确认的结果”，避免把推断写成事实。
