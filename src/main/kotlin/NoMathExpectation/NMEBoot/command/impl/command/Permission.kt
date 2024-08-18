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
        .requiresPermission("command.admin.permission")
        .literals {
            val setPermissionExecuteNode = literal("set")
                .collectPermissionId("permissionId")
                .collectString("permission")
                .optionallyCollectBoolean("allow")
                .executes {
                    val permissionId = getString("permissionId") ?: error("permissionId is null")
                    val permission = getString("permission") ?: error("permission is null")
                    val allow = getBoolean("allow")

                    PermissionService.setPermission(permission, permissionId, allow)
                    it.reply("已设置 $permissionId 的 $permission 权限为 ${allow ?: "空"}")
                }

            literal("grant", "give")
                .collectPermissionId("permissionId")
                .collectString("permission")
                .fork {
                    listOf(apply {
                        set("allow", true)
                    })
                }.forward(setPermissionExecuteNode)

            literal("decline", "deny")
                .collectPermissionId("permissionId")
                .collectString("permission")
                .fork {
                    listOf(apply {
                        set("allow", false)
                    })
                }.forward(setPermissionExecuteNode)

            literal("clear")
                .collectPermissionId("permissionId")
                .collectString("permission")
                .fork {
                    listOf(apply {
                        set("allow", null)
                    })
                }.forward(setPermissionExecuteNode)

            literal("copy")
                .collectString("from")
                .collectString("to")
                .optionallyCollectBoolean("deep")
                .executes {
                    val from = getString("from") ?: error("from is null")
                    val to = getString("to") ?: error("to is null")
                    val deep = getBoolean("deep") ?: false
                    PermissionService.copyPermission(from, to, deep)
                    it.reply("已复制 $from 的权限到 $to")
                }

            literal("move")
                .collectString("from")
                .collectString("to")
                .executes {
                    val from = getString("from") ?: error("from is null")
                    val to = getString("to") ?: error("to is null")
                    val deep = getBoolean("deep") ?: false
                    PermissionService.movePermission(from, to, deep)
                    it.reply("已移动 $from 的权限到 $to")
                }
        }