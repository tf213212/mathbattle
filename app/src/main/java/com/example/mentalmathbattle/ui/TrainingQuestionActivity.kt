package com.example.mentalmathbattle.ui

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.mentalmathbattle.MainActivity
import com.example.mentalmathbattle.R
//import com.example.mentalmathbattle.ui.HandwritingView
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

class TrainingQuestionActivity : AppCompatActivity() {

    private lateinit var progressText: TextView      // 进度显示
    private lateinit var questionText: TextView      // 题目文本
    private lateinit var answerInput: EditText       // 答案输入框
    private lateinit var resultText: TextView        // 结果提示
    private lateinit var submitButton: Button        // 提交按钮
    private lateinit var returnButton: Button        // 返回按钮



    private var currentQuestion: String = ""
    private var correctAnswer: Int = 0
    private var userId: Int = -1
    private var questionCount: Int = 5
    private var answeredCount = 0
    private var trainingSessionId: Int = -1

    private lateinit var difficulty: String

    private val client = OkHttpClient()

    // 回顾模式字段
    private var isReviewMode = false
    private var reviewQuestions: List<Question> = emptyList()
    private var reviewIndex = 0
    private var reviewCorrectCount = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_training_question)

        // 绑定控件
        progressText  = findViewById(R.id.progress_text)
        questionText  = findViewById(R.id.question_text)
        answerInput   = findViewById(R.id.answer_input)
        resultText    = findViewById(R.id.result_text)
        submitButton  = findViewById(R.id.submit_button)
        returnButton  = findViewById(R.id.return_to_main_button)

        // 从 SharedPreferences 获取数据
        val prefs = getSharedPreferences("user_prefs", MODE_PRIVATE)
        userId = prefs.getInt("userId", -1)
        questionCount = intent.getIntExtra("questionCount", 5)
        answeredCount = 0

        difficulty = intent.getStringExtra("difficulty") ?: "easy"


        // 先处理“回顾模式”，并直接 return
        if (intent.getStringExtra("mode") == "review") {
            val jsonStr = intent.getStringExtra("questionListJson") ?: "[]"
            reviewQuestions = parseQuestionsFromJson(jsonStr)
            startReviewMode()
            return
        }

        // —— 以下是正常练习模式 ——
        updateNormalProgress()
        startTrainingSession(userId.toString())
        fetchQuestion(userId.toString(),difficulty)

        submitButton.setOnClickListener {
            val input = answerInput.text.toString()
            if (input.isNotBlank()) {
                submitAnswer(input.toInt())
            }
        }

        returnButton.setOnClickListener {
            endTrainingSession() // 添加这行来结束 session
            // 返回主界面
            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra("userId", userId)
            startActivity(intent)
            finish()

        }
    }

    /*** 更新正常模式进度 ***/
    private fun updateNormalProgress() {
        progressText.text = "进度 $answeredCount/$questionCount"
    }

    /*** 回顾模式：逐题显示错题并让用户作答 ***/
    private fun startReviewMode() {
        if (reviewQuestions.isEmpty()) {
            Toast.makeText(this, "没有可回顾的题目", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        // 初始化
        reviewIndex = 0
        reviewCorrectCount = 0
        showCurrentReviewQuestion()

        // **只在这里绑定一次**
        submitButton.setOnClickListener {
            val input = answerInput.text.toString().trim()
            if (input.isEmpty()) {
                Toast.makeText(this, "请输入答案", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val userAns = input.toIntOrNull()
            val correctAns = reviewQuestions[reviewIndex].correctAnswer
            resultText.text = if (userAns == correctAns) {
                reviewCorrectCount++; "✅ 回答正确！"
            } else {
                "❌ 回答错误！正确答案：$correctAns"
            }

            // 延迟后下一题或结束
            Handler(mainLooper).postDelayed({
                reviewIndex++
                if (reviewIndex < reviewQuestions.size) {
                    showCurrentReviewQuestion()
                } else {
                    // 回顾完成
                    progressText.text = ""
                    questionText.text = "🎉 回顾完成！共 ${reviewQuestions.size} 题，答对 $reviewCorrectCount 题"
                    resultText.text = ""
                    submitButton.isEnabled = false
                }
            }, 2000)
        }

        // 返回也只需 finish()
        returnButton.setOnClickListener { finish() }
    }

    private fun showCurrentReviewQuestion() {
        val q = reviewQuestions[reviewIndex]
        progressText.text = "进度 ${reviewIndex + 1}/${reviewQuestions.size}"
        questionText.text = q.question
        resultText.text = ""
        answerInput.setText("")
    }

    private fun startTrainingSession(userId: String) {
        val url = "https://72bc-113-57-44-160.ngrok-free.app/api/training/start"
        val json = JSONObject().apply {
            put("userId", userId)
        }

        val requestBody = RequestBody.create(
            "application/json; charset=utf-8".toMediaTypeOrNull(), json.toString()
        )

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread { resultText.text = "创建训练 session 失败" }
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string()
                try {
                    val json = JSONObject(body ?: "")
                    trainingSessionId = json.getInt("sessionId")

                    runOnUiThread {
                        resultText.text = "训练开始，Session ID: $trainingSessionId"
                    }
                } catch (e: Exception) {
                    runOnUiThread { resultText.text = "数据解析失败" }
                }
            }
        })
    }

    private fun endTrainingSession() {
        val url = "https://72bc-113-57-44-160.ngrok-free.app/api/training/end"
        val json = JSONObject().apply {
            put("sessionId", trainingSessionId)
        }

        val requestBody = RequestBody.create(
            "application/json; charset=utf-8".toMediaTypeOrNull(), json.toString()
        )

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread { resultText.text = "结束训练 session 失败" }
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string()
                if (response.isSuccessful) {
                    runOnUiThread {
                        resultText.text = "训练 session 结束"
                    }
                } else {
                    runOnUiThread { resultText.text = "结束训练失败" }
                }
            }
        })
    }

    private fun fetchQuestion(userId: String, difficulty: String) {
        val url = "https://72bc-113-57-44-160.ngrok-free.app/api/generate?userId=$userId&difficulty=$difficulty"

        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread { resultText.text = "获取题目失败" }
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string()
                try {
                    val json = JSONObject(body ?: "")
                    val question = json.getString("question")
                    val answer = json.getString("answer")

                    runOnUiThread {
                        currentQuestion = question
                        correctAnswer = answer.toInt()

                        questionText.text = "题目：$question"
                        answerInput.setText("")
                        resultText.text = ""
                    }
                } catch (e: Exception) {
                    runOnUiThread { resultText.text = "数据解析失败" }
                }
            }
        })
    }

    private fun submitAnswer(userAnswer: Int) {
        val json = JSONObject().apply {
            put("userId", userId)
            put("question", currentQuestion)
            put("userAnswer", userAnswer)
            put("correctAnswer", correctAnswer)
            put("sessionId", trainingSessionId)  // 添加 sessionId
        }

        val requestBody = RequestBody.create(
            "application/json; charset=utf-8".toMediaTypeOrNull(), json.toString()
        )

        val request = Request.Builder()
            .url("https://72bc-113-57-44-160.ngrok-free.app/api/submit") // 替换为你的接口地址
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread { resultText.text = "提交失败" }
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string()

                try {
                    // 直接根据 correctAnswer 和 userAnswer 判断是否正确
                    val isCorrect = userAnswer == correctAnswer

                    runOnUiThread {
                        resultText.text = if (isCorrect) "✅ 回答正确！" else "❌ 回答错误！正确答案：$correctAnswer"
                        answeredCount++
                        updateNormalProgress()
                        if (answeredCount < questionCount) {
                            // 3秒后加载下一题
                            Handler(mainLooper).postDelayed({
                                fetchQuestion(userId.toString(),difficulty)  // 获取下一题
                            }, 3000)
                        } else {
                            // 显示完成信息
                            questionText.text = "🎉 练习完成！共答题 $questionCount 道"
                            submitButton.isEnabled = false

                            // 结束训练 session
                            endTrainingSession()
                        }
                    }
                } catch (e: Exception) {
                    runOnUiThread { resultText.text = "解析响应数据失败" }
                }
            }
        })
    }



    data class Question(
        val question: String,
        val correctAnswer: Int
    )
    private fun parseQuestionsFromJson(jsonStr: String): List<Question> {
        val arr = JSONArray(jsonStr)
        val list = mutableListOf<Question>()
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            list.add(
                Question(
                    question = obj.getString("question"),
                    correctAnswer = obj.getInt("correct_answer")
                )
            )
        }
        return list
    }


}

