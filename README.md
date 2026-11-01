Tudur's Guns (https://github.com/Tuduraw/TudursGuns) のForge 1.20.1版

前提MOD: Tudur's Vehicle Mod のForge 1.20.1版 (https://github.com/Tuduraw/tudursvehiclemod-forge)

ソースは Fabric 1.21.11 版 (Mojang マッピング) から自動生成しています。仕組みと編集の手順は [PORTING.md](PORTING.md) を参照してください。

## ビルド

```sh
# 前提MOD (Forge 版) の jar を libs/ に置く
cp ../tudursvehiclemod-forge/build/libs/tudursvehiclemod-1.1.1-forge-1.20.1.jar libs/
JAVA_HOME=/path/to/jdk17 ./gradlew build   # -> build/libs/tudursguns-0.1.0-forge-1.20.1.jar
```
