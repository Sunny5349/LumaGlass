# 独立 API 消费工程

本工程从本地 Maven 仓库获取实际发行 JAR，经 ForgeGradle `fg.deobf` 后编译公共 API 示例，用于检查文档中的接入方式。

在 LumaGlass 项目根目录运行：

```powershell
.\gradlew.bat publish
.\gradlew.bat --project-dir=examples/api-consumer compileJava
```

需要 JDK 17。依赖缓存完整后可添加 `--offline`。这是编译检查工程，不是第二个可安装模组。
