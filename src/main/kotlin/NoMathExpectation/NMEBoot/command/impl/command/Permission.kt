package NoMathExpectation.NMEBoot.command.impl.command

import NoMathExpectation.NMEBoot.command.parser.argument.collectString
import NoMathExpectation.NMEBoot.command.parser.argument.ext.collectPermissionId
import NoMathExpectation.NMEBoot.command.parser.argument.getBoolean
import NoMathExpectation.NMEBoot.command.parser.argument.getString
import NoMathExpectation.NMEBoot.command.parser.argument.optionallyCollectBoolean
import NoMathExpectation.NMEBoot.command.parser.node.*
import NoMathExpectation.NMEBoot.command.source.CommandSource
import NoMathExpectation.NMEBoot.command.source.reply
import NoMathExpectation.NMEBoot.command.util.PermissionService
import NoMathExpectation.NMEBoot.command.util.requiresPermission

suspend fun LiteralSelectionCommandNode<CommandSource<*>>.commandPermission() =
    literal("permission", "perm")
        .requiresPermission("command.permission")
        .literals {
            val executeNode = literal("set")
                .collectPermissionId("permissionId")
                .collectString("permission")
                .optionallyCollectBoolean("allow")
                .executes {
                    val permissionId = getString("permissionId") ?: error("permissionId is null")
                    val permission = getString("permission") ?: error("permission is null")
                    val allow = getBoolean("allow")

                    PermissionService.setPermission(permission, permissionId, allow)
                    it.reply("已设置 $permissionId 的 $permission 为 ${allow ?: "空"}")
                }

            literal("grant", "give")
                .collectPermissionId("permissionId")
                .collectString("permission")
                .fork {
                    listOf(apply {
                        set("allow", true)
                    })
                }.forward(executeNode)

            literal("decline", "deny")
                .collectPermissionId("permissionId")
                .collectString("permission")
                .fork {
                    listOf(apply {
                        set("allow", false)
                    })
                }.forward(executeNode)

            literal("clear")
                .collectPermissionId("permissionId")
                .collectString("permission")
                .fork {
                    listOf(apply {
                        set("allow", null)
                    })
                }.forward(executeNode)
        }