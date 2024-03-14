# ktor框架全局配置中心

ktor框架全局配置中心封装库

## 安装

### Gradle
1.项目目录下build.gradle，添加:
```groovy
implementation 'com.tangqiu.cloud:ktor_config:1.0.0-SNAPSHOT'
```
2.在项目Application.kt文件内添加
  ```kotlin
//配置中心
install(ConfigFeature) {
    configType = ConfigFeature.ConfigType.CONF //配置读取文件类型
     gitAddr = "https://xxx.xxx.xx/xxxx.git"
     filePath = "/src/main/resources/application-develop.conf"
     branch= "xxx"
     userName = "xxx"
     passWord = "xxx"
    
    refreshApi = true  //是否可刷新
    refreshPath = "/config/refresh" //配置刷新url
    
//    urlAddr = "http://localhost:8087/static/application.conf" //还可以通过url地址获取文件
    register(DBFeature){
        //其他功能的配置文件
        this.driverClassName = ""
    }
    register(DiscoveryServerFeature, DiscoveryServerFeature.Configuration::properties)
}
```

安装跨域功能
```kotlin
install(CORS) {
    method(HttpMethod.Options)
    method(HttpMethod.Get)
    method(HttpMethod.Post)
    method(HttpMethod.Put)
    method(HttpMethod.Delete)
    method(HttpMethod.Patch)
    header(HttpHeaders.Authorization)
    header(HttpHeaders.XForwardedProto)
    anyHost()
    allowCredentials = true
    allowNonSimpleContentTypes = true
}
```