package NoMathExpectation.NMEBoot.command.impl.command.common

import NoMathExpectation.NMEBoot.command.impl.PermissionAware
import NoMathExpectation.NMEBoot.command.impl.requiresPermission
import NoMathExpectation.NMEBoot.command.parser.argument.collectGreedyString
import NoMathExpectation.NMEBoot.command.parser.argument.getString
import NoMathExpectation.NMEBoot.command.parser.node.LiteralSelectionCommandNode
import NoMathExpectation.NMEBoot.command.parser.node.executes
import NoMathExpectation.NMEBoot.command.parser.node.literal
import NoMathExpectation.NMEBoot.util.sqlPlayground.executeSQLInPlaygroundDatabase
import io.github.oshai.kotlinlogging.KotlinLogging
import love.forte.simbot.ability.ReplySupport

private val logger = KotlinLogging.logger { }

suspend fun <T> LiteralSelectionCommandNode<T>.commandSql()
        where T : ReplySupport, T : PermissionAware = literal("sql")
    .requiresPermission("command.common.sql")
    .collectGreedyString("sql")
    .executes("执行SQL语句") {
        val sql = getString("sql") ?: error("SQL语句不能为空。")

        logger.info { "尝试在playground执行sql： $sql" }

        val result = kotlin.runCatching {
            executeSQLInPlaygroundDatabase(sql) ?: "无结果。"
        }.getOrElse {
            logger.warn(it) { "执行SQL语句时发生错误: $sql" }
            "发生错误：${it.message}"
        }

        it.reply(result)
    }