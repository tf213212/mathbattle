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
    private var isConnecting = false // 防止重复点击

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        WebSocketManager.retainConnection()
        setContentView(R.layout.activity_match)

        // ✅ 从 SharedPreferences 获取 userId
        val prefs = getSharedPreferences("user_prefs", MODE_PRIVATE)
        userId = prefs.getInt("userId", -1)

        matchStatusText = findViewById(R.id.match_status)
        progressBar = findViewById(R.id.progress_bar)
        startMatchButton = findViewById(R.id.start_match_button)

        startMatchButton.setOnClickListener {
            if (isConnecting) return@setOnClickListener

            startMatchButton.isEnabled = false
            matchStatusText.text = "正在连接服务器..."
            progressBar.visibility = ProgressBar.VISIBLE
            isConnecting = true

            // 建立 WebSocket 连接
            WebSocketManager.connect(
                "ws://72bc-113-57-44-160.ngrok-free.app",
                onMessage = { json ->
                    when (json.getString("type")) {
                        "start" -> runOnUiThread {
                            matchStatusText.text = "匹配成功！"
                            progressBar.visibility = ProgressBar.GONE
                            val intent = Intent(this@MatchActivity, BattleActivity::class.java)
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

            // 等待连接成功再发送匹配请求
            val connectionCheck = object : Runnable {
                override fun run() {
                    if (WebSocketManager.isConnected) {
                        WebSocketManager.sendMatchRequest(userId.toString())
                        matchStatusText.text = "正在寻找对手..."
                    } else if (isConnecting) {
                        Handler(Looper.getMainLooper()).postDelayed(this, 500)
                    }
                }
            }
            Handler(Looper.getMainLooper()).post(connectionCheck)

            // 设置连接超时（10秒）
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
