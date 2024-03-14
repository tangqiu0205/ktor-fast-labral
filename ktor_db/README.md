# ktor框架 数据库
  ktor框架 数据库增删改查封装库

## 安装

### Gradle

1.项目目录下build.gradle，添加:

```groovy
implementation 'com.tangqiu.cloud:ktor_db:1.0.0-SNAPSHOT'
```

2.在项目Application.kt文件内添加

  ```kotlin
//配置数据库
install(DBFeature) {
    jdbcUrl = "jdbc:mysql://localhost:3306/xxx?characterEncoding=utf-8"
    driverClassName = "com.mysql.cj.jdbc.Driver"
    username = "xxx"
    password = "xxx"
    maximumPoolSize = 10
}
```

## 使用

```kotlin
//需要创建映射ktorm实体类
object User : Table<Nothing>("user") {
    val id = int("id").primaryKey()
    val name = varchar("name")
    val age = int("age")
    val money = int("money")
}
```

```kotlin
//查询
get("/get") {
    for (row in DB.from(User).select()) {
        println(row[User.age])
    }
    call.respond(mapOf("hello" to "world"))
}
//插入
get("/db/insert") {
    insertEntity(dbUser) {
        set(it.age, 222)
        set(it.money, 6666)
        set(it.name, "xxx")
    }
    call.respond(mapOf("插入" to "成功"))
}
//更新
get("/db/update") {
    insertEntity(dbUser) {
        set(it.age, 222)
        set(it.money, 6666)
        set(it.name, "xxx")
    }
    call.respond(mapOf("更新" to "成功"))
}
//删除
get("/db/del") {
    deleteEntity(dbUser) {
        it.age eq 222
    }
    call.respond(mapOf("删除" to "成功"))
}

//批量更新
get("/db/batchUpdate") {
    //批量更新money大于3000的  设置money为500
    DB.batchUpdate(dbUser) {
        item {
            set(dbUser.money, 500)
            where { it.money greater 3000 }
        }
    }
}

//批量插入
get("/db/bulkInsert") {
    //有就更新没有就插入
    DB.bulkInsert(dbUser) {
        for(i in 1..9999999999) {
            item {
                set(dbUser.id, i)
                set(dbUser.name, "zhang")
                set(dbUser.money, 500)
            }
            onDuplicateKey {
                set(it.money, values(it.money).plus(2))
            }
        }
    }
}

```