# ktor框架 服务注册与发现
 ktor框架 服务注册与发现封装库

## 安装

### Gradle

1.项目目录下build.gradle，添加:

```groovy
implementation 'com.tangqiu.cloud:ktor_discovery:1.0.0-SNAPSHOT'
```

2.在项目Application.kt文件内添加

  ```kotlin
//服务注册与发现
//install(DiscoveryServerFeature) {
//    //在配置文件里 配置相关参数
//}

//通过配置中心 设置参数  无需配置文件
install(ConfigFeature) {
    configType = ConfigFeature.ConfigType.CONF
    urlAddr = "http://127.0.0.1:8080/application.conf"
    register(DiscoveryServerFeature, DiscoveryServerFeature.Configuration::properties)
}
```

## 使用
使用前需运行服务注册与发现服务（建议使用spring boot构建一个服务）

```kotlin
routing {
    val discoveryClient = discover("ktor_discovery.service")
    get("/get/discovery") {
        val response = discoveryClient.get<Map<String, String>>("/discovery")
        call.respond(response)
    }
}
```