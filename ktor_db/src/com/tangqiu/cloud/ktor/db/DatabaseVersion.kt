package com.tangqiu.cloud.ktor.db

import org.ktorm.entity.Entity
import org.ktorm.schema.Table
import org.ktorm.schema.datetime
import org.ktorm.schema.int
import java.time.LocalDateTime

interface DatabaseVersion : Entity<DatabaseVersion> {
    var version: Int
    var migrateTime: LocalDateTime

    companion object: Entity.Factory<DatabaseVersion>()
}

object DatabaseVersions: Table<DatabaseVersion>("database_version") {
    val version = int("version").primaryKey().bindTo { it.version }
    val migrateTime = datetime("migrate_time").bindTo { it.migrateTime }
}