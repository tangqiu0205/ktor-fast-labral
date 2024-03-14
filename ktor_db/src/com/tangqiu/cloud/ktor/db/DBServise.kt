package com.tangqiu.cloud.ktor.db

import io.ktor.application.*
import org.ktorm.database.Database
import org.ktorm.database.Transaction
import org.ktorm.database.TransactionIsolation
import org.ktorm.dsl.*
import org.ktorm.entity.EntitySequence
import org.ktorm.entity.sequenceOf
import org.ktorm.expression.BinaryExpression
import org.ktorm.schema.BaseTable
import org.ktorm.schema.Column
import org.ktorm.schema.ColumnDeclaring
import org.ktorm.schema.Table
import java.lang.reflect.Constructor
import java.math.BigDecimal
import java.sql.Date
import java.sql.Time
import java.sql.Timestamp
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

var df: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

interface DBService {
    val db: Database

    fun <T : Table<*>> T.query(): QuerySource = db.from(this)
    fun <T : BaseTable<*>> T.update(block: UpdateStatementBuilder.(T) -> Unit): Int = db.update(this, block)
    fun <T : BaseTable<*>> T.batchUpdate(
        block: BatchUpdateStatementBuilder<T>.() -> Unit
    ): IntArray = db.batchUpdate(this, block)

    fun <T : BaseTable<*>> T.insert(block: AssignmentsBuilder.(T) -> Unit): Int = db.insert(this, block)
    fun <T : BaseTable<*>> T.insertAndGenerateKey(block: AssignmentsBuilder.(T) -> Unit): Any =
        db.insertAndGenerateKey(this, block)

    fun <T : BaseTable<*>> T.batchInsert(
        block: BatchInsertStatementBuilder<T>.() -> Unit
    ): IntArray = db.batchInsert(this, block)

    fun <T : BaseTable<*>> T.delete(predicate: (T) -> ColumnDeclaring<Boolean>): Int = db.delete(this, predicate)
    fun <T : BaseTable<*>> T.deleteAll(): Int = db.deleteAll(this)

    fun <E : Any, T : BaseTable<E>> T.sequence(withReferences: Boolean = true): EntitySequence<E, T> =
        db.sequenceOf(this)
}

fun <T> DBService.useTransaction(isolation: TransactionIsolation? = null, func: (Transaction) -> T): T =
    db.useTransaction(isolation, func)


//插入实体
fun <T : BaseTable<*>> Application.insertEntity(table: T, block: AssignmentsBuilder.(T) -> Unit): Int =
    DB.insert(table, block)

//更新实体
fun <T : BaseTable<*>> Application.updateEntity(table: T, block: UpdateStatementBuilder.(T) -> Unit): Int =
    DB.update(table, block)

//删除实体
fun <T : BaseTable<*>> Application.deleteEntity(table: T, predicate: (T) -> ColumnDeclaring<Boolean>): Int =
    DB.delete(table, predicate)

inline fun <reified T : Any> Query.toOne(columnResolver: (String) -> String = { it }): T? = rowSet.to()

inline fun <reified T : Any> Query.toList(columnResolver: (String) -> String = { it }): List<T> = mapNotNull { it.to() }

inline fun <reified T : Any> QueryRowSet.to(columnResolver: (String) -> String = { it }): T? = T::class.java.run {
    val con = constructors.firstOrNull { it.parameterCount <= size() } as? Constructor<T> ?: return@run null
    val params = con.parameters.map { parameter ->
        val pName = columnResolver.invoke(parameter.name)
        when (parameter.type) {
            String::class.java -> getString(pName)
            Boolean::class.java -> getBoolean(pName)
            Byte::class.java -> getByte(pName)
            Short::class.java -> getShort(pName)
            Int::class.java -> getInt(pName)
            Long::class.java -> getLong(pName)
            Float::class.java -> getFloat(pName)
            Double::class.java -> getDouble(pName)
            BigDecimal::class.java -> getBigDecimal(pName)
            ByteArray::class.java -> getFloat(pName)
            Date::class.java -> getFloat(pName)
            Time::class.java -> getFloat(pName)
            Timestamp::class.java -> getFloat(pName)
            else -> getObject(pName, parameter.type)
        }
    }
    return@run con.newInstance(*params.toTypedArray())
}

inline fun <reified T : Any> BaseTable<*>.list(name: String): Column<List<T>> = registerColumn(name, ListType.obtain())

/**
 * 查询当日从00:00:00开始到23:59:59结束
 */
infix fun ColumnDeclaring<LocalDateTime>.whereDateConditions(date: String): BinaryExpression<Boolean> {
    return this greaterEq LocalDateTime.parse(date.substring(0, 10).plus(" 00:00:00"), df) and
            (this lessEq LocalDateTime.parse(date.substring(0, 10).plus(" 23:59:59"), df))
}

/**
 * 查询日期区间
 */
fun MutableList<ColumnDeclaring<Boolean>>.whereDateConditions(
    column: ColumnDeclaring<LocalDateTime>,
    start: String?,
    end: String?
): MutableList<ColumnDeclaring<Boolean>> {
    val db: MutableList<ColumnDeclaring<Boolean>> = ArrayList()
    start?.let {
        db.add(column greaterEq LocalDateTime.parse(it.substring(0, 10).plus(" 00:00:00"), df))
    }
    end?.let {
        db.add(column lessEq LocalDateTime.parse(it.substring(0, 10).plus(" 23:59:59"), df))
    }
    return db
}

/**
 * 分页
 */
fun Query.paginate(table: Table<*>, page: Int?, pageSize: Int?, orders: List<String>?): Query {
    var q = this.limit(((page ?: 1) - 1) * (pageSize ?: 20), pageSize)
    orders?.forEach {
        val list = it.split(":")
        val column = table[list[0]]
        val order = list[1]

        q = if (order == "asc") {
            q.orderBy(column.asc())
        } else {
            q.orderBy(column.desc())
        }
    }
    return q
}