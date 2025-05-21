package com.example.mentalmathbattle.data

import okhttp3.*
import org.json.JSONObject
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

object WebSocketManager {
    private var webSocket: WebSocket? = null
    private val client = OkHttpClient.Builder()
        .retryOnConnectionFailure(false) // 禁用自动重连
        .build()

    @Volatile var isConnected = false
        private set

    // 消息队列和缓存
    private val messageQueue = mutableListOf<String>()
    var latestQuestion: JSONObject? = null
    var onMessageHandler: ((JSONObject) -> Unit)? = null

    // 连接参数持久化
    private var currentUrl: String? = null
    private var pendingReconnect = false
    private var connectionRefCount = 0
    private var isUserClosed = false // 新增：标识是否是用户主动关闭

    fun connect(
        url: String,
        onMessage: (JSONObject) -> Unit,
        onFailure: (() -> Unit)? = null
    ) {
        currentUrl = url
        onMessageHandler = onMessage

        if (isConnected) {
            flushMessageQueue() // 立即发送队列中的消息
            return
        }

        if (isUserClosed) {
            println("WebSocket 连接曾由用户关闭，重置标志以允许重新连接")
            isUserClosed = false // 自动清除标志，允许重新连接
        }


        val request = Request.Builder().url(url).build()
        client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                this@WebSocketManager.webSocket = webSocket
                isConnected = true
                pendingReconnect = false
                isUserClosed = false

                println("WebSocket 连接成功")
                flushMessageQueue()
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    val json = JSONObject(text).apply {
                        if (optString("type") == "newQuestion") {
                            latestQuestion = this
                        }
                    }
                    onMessageHandler?.invoke(json)
                } catch (e: Exception) {
                    println("消息解析失败: ${e.message}")
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                handleDisconnect(t.message ?: "Unknown error", onFailure)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                handleDisconnect(reason, onFailure)
            }
        })
    }

    fun retainConnection() {
        connectionRefCount++
    }

    fun releaseConnection() {
        if (--connectionRefCount <= 0) {
            close()
        }
    }

    private fun handleDisconnect(reason: String, onFailure: (() -> Unit)?) {
        println("WebSocket 断开: $reason")
        isConnected = false
        webSocket = null
        if (!pendingReconnect && !isUserClosed) {
            pendingReconnect = true
            onFailure?.invoke()
            attemptReconnect()
        }
    }

    private fun attemptReconnect() {
        currentUrl?.let { url ->
            println("尝试重新连接...")
            val executor = Executors.newSingleThreadScheduledExecutor()
            executor.schedule({
                if (!isConnected && !isUserClosed) {
                    connect(url, onMessageHandler ?: {}, null)
                }
                executor.shutdown() // 任务完成后关闭executor
            }, 2, TimeUnit.SECONDS)
        }
    }

    fun send(json: String) {
        if (isConnected) {
            webSocket?.send(json) ?: run {
                println("发送失败: WebSocket实例为空")
                messageQueue.add(json)
            }
        } else {
            println("消息进入队列（等待连接）")
            messageQueue.add(json)
            if (!pendingReconnect && !isUserClosed) {
                attemptReconnect()
            }
        }
    }

    private fun flushMessageQueue() {
        val failedMessages = mutableListOf<String>()
        messageQueue.forEach { json ->
            if (isConnected) {
                webSocket?.send(json) ?: run {
                    failedMessages.add(json)
                }
            } else {
                failedMessages.add(json)
            }
        }
        messageQueue.clear()
        messageQueue.addAll(failedMessages)
    }

    fun close(force: Boolean = false) {
        if (force || connectionRefCount <= 0) {
            isUserClosed = true // 标识为用户主动关闭
            webSocket?.close(1000, "User intentional close")
            println("WebSocket 已由用户主动关闭")
        }
    }

    fun sendMatchRequest(userId: String) {
        val json = JSONObject().apply {
            put("type", "match")
            put("userId", userId)
        }
        send(json.toString())
    }
}