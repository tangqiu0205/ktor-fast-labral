package com.tangqiu.cloud.ktor.db

import org.ktorm.entity.Entity
import org.ktorm.schema.Table
import org.ktorm.schema.boolean
import org.ktorm.schema.datetime
import java.time.LocalDateTime

/**
 * 公共字段
 */
interface BaseEntity<E : BaseEntity<E>> : Entity<E> {
    var createTime: LocalDateTime
    var updateTime: LocalDateTime
    var disabled: Boolean
    var deleted: Boolean
}

abstract class BaseTable<E : BaseEntity<E>>(tableName: String) : Table<E>(tableName) {
    val createTime = datetime("create_time").bindTo { it.createTime }
    val updateTime = datetime("update_time").bindTo { it.updateTime }
    val disabled = boolean("disabled").bindTo { it.disabled }
    val deleted = boolean("deleted").bindTo { it.deleted }
}