package NoMathExpectation.NMEBoot.command.impl.command.common

import NoMathExpectation.NMEBoot.command.impl.AnyExecuteContext
import NoMathExpectation.NMEBoot.command.impl.requiresPermission
import NoMathExpectation.NMEBoot.command.impl.requiresSubjectId
import NoMathExpectation.NMEBoot.command.impl.source.offline.OfflineCommandSource
import NoMathExpectation.NMEBoot.command.impl.source.offline.toOnlineOrNull
import NoMathExpectation.NMEBoot.command.impl.source.requiresBotModerator
import NoMathExpectation.NMEBoot.command.parser.argument.*
import NoMathExpectation.NMEBoot.command.parser.node.*
import NoMathExpectation.NMEBoot.message.toMessageWithCICode
import NoMathExpectation.NMEBoot.util.*
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.utils.io.*
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import love.forte.simbot.ability.SendSupport
import love.forte.simbot.message.buildMessages
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration.Companion.seconds

object MCChat : CoroutineScope {
    private val scope = CoroutineScope(Dispatchers.IO) + CoroutineName("mcchat")
    override val coroutineContext: CoroutineContext
        get() = scope.coroutineContext

    private val logger = KotlinLogging.logger { }

    @Serializable
    data class Connection(
        val name: String,
        val host: String,
        val port: Int,
        val token: String = "",
        var enabled: Boolean = true,
    ) {
        init {
            require(port in 1..65535) { "Port must be in range 1..65535" }
        }

        val address get() = "$host:$port"
    }

    @Serializable
    private data class SubjectConnections(
        val source: OfflineCommandSource,
        val connections: MutableMap<String, Connection> = mutableMapOf(),
        val ignoredSenders: MutableSet<String> = mutableSetOf(),
    )

    @Serializable
    private data class Config(
        val retryDelay: Long = 30,
        val connections: MutableMap<String, SubjectConnections> = mutableMapOf(),
    )

    private val configStorage = storageOf("config/mcchat.json", Config())
    private var retryDelay: Long = 30

    private val selectorManager = SelectorManager(Dispatchers.IO)

    private class RunningConnection(
        val connection: Connection,
        private val source: OfflineCommandSource,
    ) {
        private val mutex = Mutex()
        private var job: Job? = null
        private var sendChannel: ByteWriteChannel? = null

        private suspend fun routine() {
            val (_, host, port, token) = connection
            var failureNotified = false
            while (job?.isActive == true) {
                kotlin.runCatching {
                    val socket = aSocket(selectorManager).tcp().connect(host, port)
                    socket.use {
                        val readChannel = socket.openReadChannel()
                        val writeChannel = socket.openWriteChannel(autoFlush = true)

                        val authEvent = readChannel.readExchangeEvent() as? AuthenticateEvent
                            ?: error("Invalid authentication event.")
                        if (authEvent.required == true) {
                            writeChannel.writeExchangeEvent(AuthenticateEvent(token = token))
                            val authEvent2 = readChannel.readExchangeEvent() as? AuthenticateEvent
                                ?: error("Invalid authentication event.")
                            if (authEvent2.success != true) {
                                logger.error { "Authentication for ${connection.address} failed, disconnecting." }
                                source.toOnlineOrNull()?.send("与${connection.name}验证失败，断开连接。")
                                job?.cancel()
                                return@runCatching
                            }
                        }

                        if (failureNotified) {
                            source.toOnlineOrNull()?.send("已重新连接到${connection.name}。")
                            failureNotified = false
                        }
                        mutex.withLock {
                            sendChannel = writeChannel
                        }

                        receiveRoutine(readChannel)
                    }
                }.onFailure {
                    withContext(NonCancellable) {
                        mutex.withLock {
                            sendChannel = null
                        }
                    }
                    if (job?.isActive != true) {
                        return@onFailure
                    }
                    logger.warn(it) { "Connection lost for ${connection.address}, will retry in $retryDelay seconds." }
                    if (!failureNotified) {
                        source.toOnlineOrNull()?.send("与${connection.name}断开连接，将尝试恢复。")
                        failureNotified = true
                    }
                    delay(retryDelay.seconds)
                }.onSuccess {
                    withContext(NonCancellable) {
                        mutex.withLock {
                            sendChannel = null
                        }
                    }
                }
            }
            withContext(NonCancellable) {
                mutex.withLock {
                    job = null
                }
            }
        }

        private suspend fun SendSupport.sendAndLogEcho(msg: String) {
//            echoMessagesMutex.withLock {
//                echoMessages += msg
//                send(msg)
//            }
            send(msg.toMessageWithCICode())
        }

        private suspend fun receiveRoutine(readChannel: ByteReadChannel) {
            while (job?.isActive == true && !readChannel.isClosedForRead) {
                kotlin.runCatching {
                    val event = readChannel.readExchangeEvent()
                    handleEvent(event)
                }.onFailure {
                    if (job?.isActive != true || readChannel.isClosedForRead) {
                        return@onFailure
                    }
                    logger.error(it) { "Failed to receive message from ${connection.name}." }
                }
            }
        }

        private suspend fun handleEvent(event: ExchangeEvent) {
            when (event) {
                is MessageEvent -> source.toOnlineOrNull()
                    ?.sendAndLogEcho("[${connection.name}] ${event.from}: ${event.content}")

                is PlayerJoinEvent -> source.toOnlineOrNull()
                    ?.sendAndLogEcho("[${connection.name}] ${event.name} 加入了游戏。")

                is PlayerLeaveEvent -> source.toOnlineOrNull()
                    ?.sendAndLogEcho("[${connection.name}] ${event.name} 离开了游戏。")

                is PlayerDieEvent -> source.toOnlineOrNull()
                    ?.sendAndLogEcho("[${connection.name}] ${event.text}")

                is PlayerAdvancementEvent -> source.toOnlineOrNull()
                    ?.sendAndLogEcho("[${connection.name}] ${event.name} 获得了进度 [${event.advancement}]")

                is StatusEvent -> source.toOnlineOrNull()?.let {
                    val message = buildMessages {
                        +"${connection.name} 状态：\n"
                        +"版本：${event.brand} ${event.version}\n"
                        +"人数：${event.playerNumber}/${event.maxPlayerNumber}\n"

                        if (event.playerNumber <= 0) {
                            return@buildMessages
                        }
                        +"玩家："
                        +event.playerNames.joinToString(", ")
                        if (event.playerNames.size != event.playerNumber) {
                            +"..."
                        }
                    }
                    it.send(message)
                }

                else -> logger.warn { "Received unknown event: $event" }
            }
        }

        suspend fun launch() {
            mutex.withLock {
                if (job != null) {
                    return
                }

                job = scope.launch(start = CoroutineStart.LAZY) {
                    routine()
                }
                job?.start()
            }
        }

        suspend fun cancel() {
            mutex.withLock {
                job?.cancel()
            }
        }

        suspend fun sendEvent(event: ExchangeEvent) {
            mutex.withLock {
                sendChannel?.writeExchangeEvent(event)
            }
        }
    }

    private val runningConnections: MutableMap<Pair<String, String>, RunningConnection> = mutableMapOf()
    private val connectionMutex = Mutex()

    private suspend fun launchConnection(subjectId: String, source: OfflineCommandSource, connection: Connection) {
        if (subjectId to connection.name in runningConnections) {
            return
        }

        logger.info { "Starting connection to ${connection.address}" }
        val runningConnection = RunningConnection(connection, source)
        runningConnection.launch()
        runningConnections[subjectId to connection.name] = runningConnection
    }

    private suspend fun cancelConnection(subjectId: String, name: String) {
        val connection = runningConnections.remove(subjectId to name) ?: return
        logger.info { "Stopping connection to ${connection.connection.address}" }
        connection.cancel()
    }

    suspend fun listConnections(subjectId: String): List<Pair<String, Boolean>> =
        configStorage.get()
            .connections[subjectId]
            ?.connections
            ?.map { (name, connection) -> name to connection.enabled }
            ?: emptyList()

    suspend fun putConnection(subjectId: String, source: OfflineCommandSource, connection: Connection) {
        val oldConnection = configStorage.referenceUpdate {
            val subjectConnections = it.connections.getOrPut(subjectId) { SubjectConnections(source) }
            subjectConnections.connections.put(connection.name, connection)
        }
        connectionMutex.withLock {
            oldConnection?.let {
                cancelConnection(subjectId, oldConnection.name)
            }
            if (connection.enabled) {
                launchConnection(subjectId, source, connection)
            }
        }
    }

    suspend fun removeConnection(subjectId: String, name: String): Boolean {
        val connection = configStorage.referenceUpdate {
            val subjectConnections = it.connections[subjectId] ?: return@referenceUpdate null
            subjectConnections.connections.remove(name)
        }
        if (connection == null) {
            return false
        }
        connectionMutex.withLock {
            cancelConnection(subjectId, connection.name)
        }
        return true
    }

    suspend fun enableConnection(subjectId: String, name: String): Boolean {
        val (connection, source) = configStorage.referenceUpdate { config ->
            val subjectConnections = config.connections[subjectId] ?: return@referenceUpdate null
            val connection =
                subjectConnections.connections[name]?.also { it.enabled = true } ?: return@referenceUpdate null
            connection to subjectConnections.source
        } ?: return false
        connectionMutex.withLock {
            launchConnection(subjectId, source, connection)
        }
        return true
    }

    suspend fun disableConnection(subjectId: String, name: String): Boolean {
        val connection = configStorage.referenceUpdate { config ->
            config.connections[subjectId]
                ?.connections
                ?.get(name)
                ?.also { it.enabled = false }
        } ?: return false
        connectionMutex.withLock {
            cancelConnection(subjectId, connection.name)
        }
        return true
    }

    suspend fun sendMessage(subjectId: String, name: String, message: String) {
        connectionMutex.withLock {
            runningConnections.forEach { (pair, connection) ->
                if (pair.first != subjectId) {
                    return@forEach
                }
                kotlin.runCatching {
                    connection.sendEvent(MessageEvent(name, message))
                }.onFailure {
                    if (!scope.isActive) {
                        return
                    }
                    logger.error(it) { "Failed to send a message to ${connection.connection.address}" }
                }
            }
        }
    }

    suspend fun pingConnection(subjectId: String, name: String): Boolean {
        connectionMutex.withLock {
            runningConnections[subjectId to name]?.let { connection ->
                kotlin.runCatching {
                    connection.sendEvent(StatusPingEvent)
                }.onFailure {
                    logger.error(it) { "Failed to ping ${connection.connection.address}" }
                    return false
                }
                return true
            }
            return false
        }
    }

    private var inited = false
    suspend fun initConnections() {
        if (inited) {
            error("MCChat has already been initialized.")
        }
        inited = true

        configStorage.referenceUpdate {
            retryDelay = it.retryDelay

            connectionMutex.withLock {
                it.connections.forEach { (subjectId, subjectConnections) ->
                    subjectConnections.connections.forEach { (_, connection) ->
                        if (connection.enabled) {
                            launchConnection(subjectId, subjectConnections.source, connection)
                        }
                    }
                }
            }
        }
    }

    private val echoMessages: MutableList<String> = mutableListOf()
    private val echoMessagesMutex = Mutex()

    suspend fun checkEchoAndRemove(msg: String): Boolean = echoMessagesMutex.withLock {
        echoMessages.remove(msg)
    }

    suspend fun addIgnoredSender(subjectId: String, id: String, source: OfflineCommandSource) =
        configStorage.referenceUpdate {
            it.connections.getOrPut(subjectId) { SubjectConnections(source) }.ignoredSenders.add(id)
        }

    suspend fun removeIgnoredSender(subjectId: String, id: String) = configStorage.referenceUpdate {
        it.connections[subjectId]?.ignoredSenders?.remove(id) ?: false
    }

    suspend fun isIgnoredSender(subjectId: String, id: String) =
        configStorage.get().connections[subjectId]?.ignoredSenders?.contains(id) ?: false
}

suspend fun LiteralSelectionCommandNode<AnyExecuteContext>.commandMCChat() =
    literal("mcchat", "mc")
        .requiresPermission("command.common.mcchat")
        .requiresSubjectId()
        .literals {
            help = "分享 Minecraft 服务器聊天"

            literal("connect")
                .requiresBotModerator()
                .collectString("name")
                .collectString("host")
                .collectInt("port")
                .checkInRange(1, 65535)
                .optionallyCollectString("token")
                .optionallyCollectBoolean("enabled")
                .executes("添加服务器") {
                    val name = getString("name") ?: error("Name is required.")
                    val host = getString("host") ?: error("Host is required.")
                    val port = getInt("port") ?: error("Port is required.")
                    val token = getString("token") ?: ""
                    val enabled = getBoolean("enabled") ?: true

                    val subjectId = it.target.subjectPermissionId ?: error("无法获取SubjectId")
                    val connection = MCChat.Connection(name, host, port, token, enabled)
                    val source = it.target.toOffline()
                    MCChat.putConnection(subjectId, source, connection)

                    it.reply("已添加服务器 $name")
                }

            literal("disconnect")
                .requiresBotModerator()
                .collectString("name")
                .executes("移除服务器") {
                    val name = getString("name") ?: error("Name is required.")
                    val subjectId = it.target.subjectPermissionId ?: error("无法获取SubjectId")
                    if (MCChat.removeConnection(subjectId, name)) {
                        it.reply("已移除服务器 $name")
                    } else {
                        it.reply("未找到服务器 $name")
                    }
                }

            literal("enable")
                .requiresBotModerator()
                .collectString("name")
                .executes("启用服务器") {
                    val name = getString("name") ?: error("Name is required.")
                    val subjectId = it.target.subjectPermissionId ?: error("无法获取SubjectId")
                    if (MCChat.enableConnection(subjectId, name)) {
                        it.reply("已启用服务器 $name")
                    } else {
                        it.reply("未找到服务器 $name")
                    }
                }

            literal("disable")
                .requiresBotModerator()
                .collectString("name")
                .executes("禁用服务器") {
                    val name = getString("name") ?: error("Name is required.")
                    val subjectId = it.target.subjectPermissionId ?: error("无法获取SubjectId")
                    if (MCChat.disableConnection(subjectId, name)) {
                        it.reply("已禁用服务器 $name")
                    } else {
                        it.reply("未找到服务器 $name")
                    }
                }

            literal("list")
                .executes("列出服务器") {
                    val subjectId = it.target.subjectPermissionId ?: error("无法获取SubjectId")
                    it.reply(
                        MCChat.listConnections(subjectId)
                            .joinToString("\n") { (name, enabled) ->
                                if (enabled) {
                                    name
                                } else {
                                    "$name (已禁用)"
                                }
                            }.ifBlank { "没有已连接的服务器" }
                    )
                }

            literal("ping")
                .collectString("name")
                .executes("获取服务器状态") {
                    val subjectId = it.target.subjectPermissionId ?: error("无法获取SubjectId")
                    val name = getString("name") ?: error("无法获取名字")
                    if (!MCChat.pingConnection(subjectId, name)) {
                        it.reply("无法获取服务器 $name 的状态")
                    }
                }

            literal("broadcastme")
                .select {
                    help = "控制你的消息是否会被广播"
                    blockOptions = false

                    collectBoolean("toggle").executes("设置你的消息是否会被广播") {
                        val subjectId = it.target.subjectPermissionId ?: error("无法获取SubjectId")
                        val toggle = getBoolean("toggle") ?: error("Toggle is required.")
                        val source = it.target.toOffline()
                        if (toggle) {
                            MCChat.removeIgnoredSender(subjectId, it.target.id)
                            it.reply("你的消息将会被广播")
                        } else {
                            MCChat.addIgnoredSender(subjectId, it.target.id, source)
                            it.reply("你的消息将不会被广播")
                        }
                    }

                    executes("查询你的消息是否会被广播") {
                        val subjectId = it.target.subjectPermissionId ?: error("无法获取SubjectId")
                        if (MCChat.isIgnoredSender(subjectId, it.target.id)) {
                            it.reply("你的消息当前不会被广播")
                        } else {
                            it.reply("你的消息当前会被广播")
                        }
                    }
                }
        }
