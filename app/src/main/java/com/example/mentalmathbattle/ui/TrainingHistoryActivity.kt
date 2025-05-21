package com.example.mentalmathbattle.ui

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.mentalmathbattle.MainActivity
import com.example.mentalmathbattle.R
import kotlinx.coroutines.*
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL

class TrainingHistoryActivity : AppCompatActivity() {

    private lateinit var historyListView: ListView
    private lateinit var backToMainButton: Button
    private val baseUrl = "https://72bc-113-57-44-160.ngrok-free.app/api/training/sessions"
    private var userId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_training_history)

        historyListView = findViewById(R.id.history_list)
        backToMainButton = findViewById(R.id.back_to_main_button)

        backToMainButton.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra("userId", userId) // 正确地把 userId 放进去
            startActivity(intent)
            finish()
        }


        fetchTrainingHistory()

        // 设置列表项点击事件
        historyListView.setOnItemClickListener { _, _, position, _ ->
            val session = historyListView.adapter.getItem(position) as TrainingSession
            val intent = Intent(this, TrainingDetailActivity::class.java)
            intent.putExtra("sessionId", session.id) // 传递 sessionId
            intent.putExtra("userId", userId)
            startActivity(intent)
        }
    }

    private fun fetchTrainingHistory() {
        // 从 SharedPreferences 获取数据
        val prefs = getSharedPreferences("user_prefs", MODE_PRIVATE)
        userId = prefs.getInt("userId", -1)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = URL("$baseUrl?userId=$userId")  // ✅ 添加 userId 参数
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.connectTimeout = 5000
                conn.readTimeout = 5000

                val responseCode = conn.responseCode
                if (responseCode == 200) {
                    val inputStream = conn.inputStream
                    val responseText = inputStream.bufferedReader().use { it.readText() }
                    val sessions = parseTrainingSessions(responseText)

                    withContext(Dispatchers.Main) {
                        val adapter = TrainingSessionAdapter(
                            this@TrainingHistoryActivity,
                            sessions
                        )
                        historyListView.adapter = adapter
                    }
                } else {
                    showToast("获取失败，状态码：$responseCode")
                }
                conn.disconnect()
            } catch (e: Exception) {
                showToast("请求异常: ${e.message}")
            }
        }
    }

    private fun parseTrainingSessions(json: String): List<TrainingSession> {
        return try {
            val result = mutableListOf<TrainingSession>()
            val jsonArray = JSONArray(json)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                result.add(
                    TrainingSession(
                        id = obj.getInt("id"),
                        questionCount = obj.getInt("question_count"),
                        correctCount = obj.getInt("correct_count"),
                        accuracy = obj.getDouble("accuracy"),
                        startTime = obj.getString("start_time"),
                        endTime = obj.getString("end_time")
                    )
                )
            }
            result
        } catch (e: Exception) {
            showToast("解析数据失败: ${e.message}")
            emptyList()
        }
    }

    private fun showToast(msg: String) {
        runOnUiThread {
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
        }
    }

    data class TrainingSession(
        val id: Int,
        val questionCount: Int,
        val correctCount: Int,
        val accuracy: Double,
        val startTime: String,
        val endTime: String
    )

    // 自定义适配器
    private class TrainingSessionAdapter(
        context: android.content.Context,
        private val sessions: List<TrainingSession>
    ) : ArrayAdapter<TrainingSession>(context, android.R.layout.simple_list_item_1, sessions) {

        override fun getView(position: Int, convertView: android.view.View?, parent: android.view.ViewGroup): android.view.View {
            val view = convertView ?: android.view.LayoutInflater.from(context).inflate(android.R.layout.simple_list_item_1, parent, false)

            val session = sessions[position]
            val textView = view.findViewById<TextView>(android.R.id.text1)
            textView.text = "时间: ${session.startTime} - ${session.endTime}\n题数: ${session.questionCount} 正确: ${session.correctCount} 正确率: ${session.accuracy}%"
            return view
        }
    }
}