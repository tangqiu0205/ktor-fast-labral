# ktor框架代码生成库

ktor框架代码生成封装库

## 安装

### Gradle

1.项目目录下build.gradle，添加:

```groovy
apply plugin: 'kotlin-kapt'

implementation 'com.tangqiu.cloud:ktor_codegen:1.0.0-SNAPSHOT'
kapt 'com.tangqiu.cloud:ktor_codegen:1.0.0-SNAPSHOT' //生成代码
//kapt project(':ktor_codegen') //生成代码

```

2: 添加注解

```kotlin
/**
 * 可在 类(class) 方法(Function) 上添加如下注解
 * name: 类名 前缀
 * packageName: 包名
 * url：openAPI json 地址
 */
//@OpenApi(name = "User", packageName = "com.tangqiu.user", url = "http://127.0.0.1:8080/tangqiupenapi.json")

```

3：生成代码： 点击项目下build按钮会自动生成代码
![img.png](resources/img.png)

## 生成的代码使用

1:实现自动生成的service代码
```kotlin
class DemoServiceImpl : DemoService {
    //....
    //实现类内写如下代码
    companion object Factory : ServiceFactory<DemoService> {
        override fun create(app: Application): DemoService {
            return DemoServiceImpl()
        }
    }
}
```

2：在项目Application.kt文件内添加
  ```kotlin
  install(CodegenFeature) {
    /**
     * DemoController: 自动生成的Controller
     * DemoServiceImpl: 实现类 实现通过自动生成的DemoService
     */
    register(DemoController, DemoServiceImpl)
}
```