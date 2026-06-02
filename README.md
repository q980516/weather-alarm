# WeatherAlarm / 雨晴闹钟

Crafted with care by LinRui.

雨晴闹钟是一个 Android 原生天气闹钟 MVP。用户设置雨天时间、非雨天时间和最终死线时间，App 会在起床前检查当前位置天气，并根据天气选择当天实际闹钟。

## Features

- Kotlin + Jetpack Compose UI
- QWeather 逐小时天气接口
- 当前位置天气判断，不依赖各品牌自带天气 App
- 雨天/非雨天双闹钟时间
- 最终死线闹钟
- 普通闹钟后到死线前随机 3 次起床确认通知
- 前台守护服务和电池优化状态提示
- 开机后恢复闹钟
- 失败兜底：天气接口失败时使用较晚闹钟时间

## QWeather Config

在项目根目录新建 `local.properties`：

```properties
QWEATHER_API_KEY=your_qweather_api_key
QWEATHER_API_HOST=your_qweather_api_host
```

如果要打 release 包，再加入本地签名配置：

```properties
RELEASE_STORE_FILE=work/release-weather-alarm.jks
RELEASE_STORE_PASSWORD=your_store_password
RELEASE_KEY_ALIAS=your_key_alias
RELEASE_KEY_PASSWORD=your_key_password
```

`local.properties` 已加入 `.gitignore`，不要把 API Key 或 keystore 提交到仓库。

## Build

```bash
./gradlew assembleDebug
./gradlew assembleRelease
```

也可以用 Android Studio 打开项目后直接运行 `app`。
