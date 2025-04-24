package com.example.mentalmathbattle.ui

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.TextView
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import com.example.mentalmathbattle.R
import com.example.mentalmathbattle.data.WebSocketManager
import org.json.JSONObject

class MatchActivity : AppCompatActivity() {
    private lateinit var matchStatusText: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var startMatchButton: Button
    private var userId: Int = -1
    private var isConnecting = false // 新增连接状态标志

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WebSocketManager.retainConnection()
        setContentView(R.layout.activity_match)
        userId = intent.getIntExtra("userId", -1)

        matchStatusText = findViewById(R.id.match_status)
        progressBar = findViewById(R.id.progress_bar)
        startMatchButton = findViewById(R.id.start_match_button)

        startMatchButton.setOnClickListener {
            if (isConnecting) return@setOnClickListener // 防止重复点击

            startMatchButton.isEnabled = false
            matchStatusText.text = "正在连接服务器..."
            progressBar.visibility = ProgressBar.VISIBLE
            isConnecting = true

            // 第一步：先建立连接
            WebSocketManager.connect(
                "ws://10.0.2.2:8080",
                onMessage = { json ->
                    when (json.getString("type")) {
                        "start" -> runOnUiThread {
                            matchStatusText.text = "匹配成功！"
                            progressBar.visibility = ProgressBar.GONE
                            val intent = Intent(this@MatchActivity, BattleActivity::class.java)
                            intent.putExtra("userId", userId)
                            startActivity(intent)
                            finish()
                        }
                        "waiting" -> runOnUiThread {
                            matchStatusText.text = "正在寻找对手..."
                            progressBar.visibility = ProgressBar.VISIBLE
                        }
                        "timeout" -> runOnUiThread {
                            handleConnectionFailure()
                        }
                    }
                },
                onFailure = {
                    runOnUiThread {
                        handleConnectionFailure()
                    }
                }
            )

            // 第二步：监听连接成功后再发match
            val connectionCheck = object : Runnable {
                override fun run() {
                    if (WebSocketManager.isConnected) {
                        // 连接成功后才发送匹配请求
                        WebSocketManager.sendMatchRequest(userId.toString())
                        matchStatusText.text = "正在寻找对手..."
                    } else if (isConnecting) {
                        // 每500ms检查一次连接状态，最多尝试5秒
                        Handler(Looper.getMainLooper()).postDelayed(this, 500)
                    }
                }
            }
            Handler(Looper.getMainLooper()).post(connectionCheck)

            // 设置超时（10秒未连接则报错）
            Handler(Looper.getMainLooper()).postDelayed({
                if (isConnecting && !WebSocketManager.isConnected) {
                    handleConnectionFailure()
                }
            }, 10000)
        }
    }

    private fun handleConnectionFailure() {
        isConnecting = false
        matchStatusText.text = "连接失败，请重试"
        progressBar.visibility = ProgressBar.GONE
        startMatchButton.isEnabled = true
        WebSocketManager.close()
    }

    override fun onDestroy() {
        WebSocketManager.releaseConnection()
        super.onDestroy()
    }
}