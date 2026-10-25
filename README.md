# Modular Machinery: Community Edition — 1.7.10

mmce1.7.10 非官方移植版。

把 [Modular Machinery: Community Edition](https://github.com/KasumiNova/ModularMachinery-Community-Edition) 2.3.2 移植到 Minecraft 1.7.10 Forge。用 JSON 定义多方块机器与配方，支持 NEI 配方页与结构预览、Waila，兼容 Angelica。

## 依赖

| 模组 | 版本 | 必需 |
|---|---|---|
| [GTNHLib](https://github.com/GTNewHorizons/GTNHLib) | ≥ 0.11.54 | 是 |
| [CraftTweaker](https://github.com/GTNewHorizons/CraftTweaker)（modid `MineTweaker3`） | 3.3.1 | 是 |



## 文档

详细的功能清单、兼容性与使用说明见 [食用方法.md](食用方法.md)。

## 构建

需要 JDK 25 运行 Gradle（见 `.java-version`），JDK 8 由 `JAVA8_HOME` 指定用于编译：

```bash
export JAVA_HOME=/path/to/jdk-25
export JAVA8_HOME=/path/to/jdk-8
./gradlew build
```

产物在 `build/libs/`，发布用 `modularmachinery-2.3.3-mc1.7.10.jar`。

## 许可证

GPLv3，与上游相同。随 jar 打包的 gugu-utils 部分来自 MIT 许可的 [GuGu Utils](https://github.com/ParaParty/gugu-utils)，许可证全文见 `src/main/resources/LICENSE-gugu-utils.txt`。
