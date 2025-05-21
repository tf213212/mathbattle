package com.example.mentalmathbattle.ui

import android.content.Context
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.mentalmathbattle.R
import kotlinx.coroutines.*
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL

class TrainingDetailActivity : AppCompatActivity() {

    private lateinit var detailListView: ListView
    private lateinit var backToHistoryButton: Button
    private val baseUrl = "https://72bc-113-57-44-160.ngrok-free.app/api/training/detail"
    private var sessionId = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_training_detail) // 使用原布局

        // 获取视图
        detailListView = findViewById(R.id.detail_list)
        backToHistoryButton = findViewById(R.id.back_to_history_button)  // 获取按钮

        // 设置返回历史记录界面的按钮点击事件
        backToHistoryButton.setOnClickListener {
            // 返回到 TrainingHistoryActivity
            finish()  // 调用 finish() 方法可以结束当前活动并返回历史记录界面
        }


        // 获取 sessionId
        sessionId = intent.getIntExtra("sessionId", -1)
        if (sessionId != -1) {
            fetchTrainingDetails(sessionId)
        } else {
            showToast("缺少训练记录ID")
        }
    }

    private fun fetchTrainingDetails(sessionId: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = URL("$baseUrl?sessionId=$sessionId")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.connectTimeout = 5000
                conn.readTimeout = 5000

                val responseCode = conn.responseCode
                if (responseCode == 200) {
                    val inputStream = conn.inputStream
                    val responseText = inputStream.bufferedReader().use { it.readText() }
                    val details = parseTrainingDetails(responseText)

                    withContext(Dispatchers.Main) {
                        val adapter = TrainingDetailAdapter(
                            this@TrainingDetailActivity,
                            details
                        )
                        detailListView.adapter = adapter
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

    private fun parseTrainingDetails(json: String): List<TrainingDetail> {
        return try {
            val result = mutableListOf<TrainingDetail>()
            val jsonArray = JSONArray(json)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                result.add(
                    TrainingDetail(
                        question = obj.getString("question"),
                        userAnswer = obj.getInt("user_answer"),
                        correctAnswer = obj.getInt("correct_answer"),
                        isCorrect = obj.getInt("is_correct")
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

    data class TrainingDetail(
        val question: String,
        val userAnswer: Int,
        val correctAnswer: Int,
        val isCorrect: Int
    )

    // 自定义适配器
    private class TrainingDetailAdapter(
        context: Context,
        private val details: List<TrainingDetail> // 确保是 TrainingDetail 类型
    ) : ArrayAdapter<TrainingDetail>(context, android.R.layout.simple_list_item_1, details) {

        override fun getView(position: Int, convertView: android.view.View?, parent: android.view.ViewGroup): android.view.View {
            val view = convertView ?: android.view.LayoutInflater.from(context).inflate(android.R.layout.simple_list_item_1, parent, false)

            val detail = details[position]
            val textView = view.findViewById<TextView>(android.R.id.text1)

            // 显示题目、用户答案、正确答案和是否正确
            textView.text = """
            问题: ${detail.question}
            你的答案: ${detail.userAnswer}
            正确答案: ${detail.correctAnswer}
            ${if (detail.isCorrect == 1) "正确" else "错误"}
        """.trimIndent()

            return view
        }
    }

}


