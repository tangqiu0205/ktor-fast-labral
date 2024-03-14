# ktor框架缓存库
 ktor框架缓存封装库

## 安装

### Gradle
1.项目目录下build.gradle，添加:
```groovy
implementation 'com.tangqiu.cloud:ktor_cache:1.0.0-SNAPSHOT'
```
2.在项目Application.kt文件内添加
```kotlin
install(CacheFeature) {
    //此处可配置的参数
    /**
    this.threads = 16 //默认值: 当前处理核数量 * 2
    this.nettyThreads = 32 //默认值: 当前处理核数量 * 2
    this.codec = CodecFactory().createCodec(CodecType.MARSHALLINGCODEC) //编解码方式

    this.idleConnectionTimeout = 10000 //空闲连接超时
    this.connectTimeout = 10000 //连接超时
    this.timeout = 3000 //超时
    this.retryAttempts = 3 //重试次数
    this.retryInterval = 1500 //重试间隔
    this.password = null //密码
    this.subscriptionsPerConnection = 5 //每个连接的订阅数
    this.clientName = null //客户端名称
    this.address = "redis://127.0.0.1:6379" //缓存环境连接地址
    this.subscriptionConnectionMinimumIdleSize = 1 //最小订阅连接空闲大小
    this.subscriptionConnectionPoolSize = 50 //订阅连接池大小
    this.connectionMinimumIdleSize = 32 //最小连接空闲大小
    this.connectionPoolSize = 64 //连接池大小
    this.database = 0
    this.dnsMonitoringInterval = 5000 //dns监视间隔
    */
}

//添加全局捕获异常
/**
 * 全局异常捕获
 */
install(StatusPages) {
    exception<CacheException> { cause ->
        call.respond(HttpStatusCode.InternalServerError, cause.message ?: "")
    }
}
```


## 使用
注：可以设置缓存 删除缓存 设置时间等等操作
```kotlin
//设置缓存
get("/set/cache") {
    try {
        val user = User("xxx", 2051, "xxxx")
        cache.set("user", user)
        call.respond("设置成功: $user")
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

//获取缓存
get("/get/cache") {
    call.respond("获取成功：" + cache.get<User>("user"))
}

//加锁
val lock = cache.getLock("lockKey")
try {
    lock.tryLock(3, TimeUnit.SECONDS) //加锁
    //......Do something
} catch (e: Exception) {
    e.printStackTrace()
    call.respond("删除失败")
} finally {
    lock.unlock()
}
```