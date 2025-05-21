package com.example.mentalmathbattle.ui

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.mentalmathbattle.R
import com.example.mentalmathbattle.data.WebSocketManager
import org.json.JSONObject

class BattleActivity : AppCompatActivity() {

    private lateinit var questionText: TextView
    private lateinit var answerInput: EditText
    private lateinit var resultText: TextView
    private lateinit var submitButton: Button
    private lateinit var countdownText: TextView
    private lateinit var yourProgressBar: ProgressBar
    private lateinit var opponentProgressBar: ProgressBar
    private lateinit var yourStatusText: TextView
    private lateinit var opponentStatusText: TextView

    private var battleTimer: CountDownTimer? = null
    private var millisUntilFinished: Long = 30000

    private var userId: Int = -1
    private var correctAnswer: Int = 0
    private var currentQuestion: String = ""
    private var correctCount = 0
    private var totalQuestions = 0
    private var battleEnded = false
    private var opponentCorrectCount = 0
    private var opponentTotalQuestions = 0
    private var questionCount = 10

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WebSocketManager.retainConnection()
        setContentView(R.layout.activity_battle)

        // 从 SharedPreferences 获取数据
        val prefs = getSharedPreferences("user_prefs", MODE_PRIVATE)
        userId = prefs.getInt("userId", -1)
        questionCount = prefs.getInt("questionCount", 10)

        questionText = findViewById(R.id.question_text)
        answerInput = findViewById(R.id.answer_input)
        resultText = findViewById(R.id.result_text)
        submitButton = findViewById(R.id.submit_button)
        countdownText = findViewById(R.id.countdown_text)
        yourProgressBar = findViewById(R.id.your_progress_bar)
        opponentProgressBar = findViewById(R.id.opponent_progress_bar)
        yourStatusText = findViewById(R.id.your_status_text)
        opponentStatusText = findViewById(R.id.opponent_status_text)

        setupWebSocket()
        startBattleTimer(30)

        submitButton.setOnClickListener {
            submitAnswer()
        }
        answerInput.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE ||
                (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER)) {
                submitAnswer()
                true
            } else {
                false
            }
        }
    }

    private fun setupWebSocket() {
        val messageHandler: (JSONObject) -> Unit = { jsonMessage ->
            val type = jsonMessage.optString("type")
            when (type) {
                "newQuestion" -> {
                    if (totalQuestions < questionCount) {
                        val newQuestion = jsonMessage.optString("question")
                        correctAnswer = jsonMessage.optInt("correctAnswer")
                        currentQuestion = newQuestion
                        runOnUiThread {
                            updateQuestion(newQuestion)
                            submitButton.isEnabled = true
                        }
                    }
                }
                "result" -> {
                    val isCorrect = jsonMessage.optBoolean("isCorrect")
                    val correctAnswer = jsonMessage.optInt("correctAnswer")
                    runOnUiThread {
                        showResult(isCorrect, correctAnswer)
                    }
                }
                "startBattle" -> {
                    val difficulty = jsonMessage.optString("difficulty")
                    val questionCount = jsonMessage.optInt("questionCount")
                    // 存入 SharedPreferences
                    val prefs = getSharedPreferences("user_prefs", MODE_PRIVATE)
                    prefs.edit().putInt("questionCount", questionCount).apply()
                    runOnUiThread {
                        setupBattleUI(difficulty, questionCount)
                    }
                }
                "endBattle" -> {
                    val result = jsonMessage.optString("result")
                    val correctCount = jsonMessage.optInt("correctCount")
                    val totalQuestions = jsonMessage.optInt("totalQuestions")
                    val timeSpent = jsonMessage.optInt("timeSpent")
                    val score = jsonMessage.optInt("score")
                    runOnUiThread {
                        navigateToResultActivity(result, correctCount, totalQuestions, timeSpent, score)
                    }
                }
                "statusUpdate" -> {
                    val opponentCorrectCount = jsonMessage.optInt("correctCount")
                    val opponentTotalQuestions = jsonMessage.optInt("totalQuestions")
                    runOnUiThread {
                        updateOpponentStatus(opponentCorrectCount, opponentTotalQuestions)
                    }
                }
                "waitingForOpponent" -> { // 新增：处理 waitingForOpponent 消息
                    runOnUiThread {
                        showWaitingForOpponent()
                    }
                }
            }
        }

        WebSocketManager.onMessageHandler = messageHandler
        WebSocketManager.latestQuestion?.let { messageHandler(it) }
    }

    private fun startBattleTimer(timeLimit: Int) {
        battleTimer?.cancel()
        battleTimer = object : CountDownTimer(timeLimit * 1000L, 1000) {
            override fun onTick(millis: Long) {
                millisUntilFinished = millis
                val seconds = (millis / 1000).toInt()
                countdownText.text = String.format("%02d:%02d", seconds / 60, seconds % 60)
            }

            override fun onFinish() {
                endBattle()
            }
        }
        battleTimer?.start()
    }

    private fun submitAnswer() {
        if (battleEnded) return
        submitButton.isEnabled = false

        val userAnswer = answerInput.text.toString().toIntOrNull()
        if (userAnswer == null) {
            Toast.makeText(this, "请输入有效的答案", Toast.LENGTH_SHORT).show()
            submitButton.isEnabled = true
            return
        }

        val answerJson = JSONObject().apply {
            put("type", "answer")
            put("question", currentQuestion)
            put("answer", userAnswer)
            put("userId", userId)
        }

        if (!WebSocketManager.isConnected) {
            Toast.makeText(this, "WebSocket未连接", Toast.LENGTH_SHORT).show()
            return
        }

        WebSocketManager.send(answerJson.toString())
    }

    private fun updateQuestion(newQuestion: String) {
        questionText.text = newQuestion
        answerInput.text.clear()
    }

    private fun showResult(isCorrect: Boolean, correctAnswer: Int) {
        if (battleEnded) return

        if (isCorrect) correctCount++
        totalQuestions++

        val resultMessage = if (isCorrect) "正确！" else "错误！正确答案是: $correctAnswer"
        resultText.text = resultMessage
        resultText.visibility = TextView.VISIBLE

        updateYourProgress(totalQuestions)
        sendStatusUpdate()

        if (totalQuestions >= questionCount) {
            endBattle()
        }
    }

    private fun endBattle() {
        battleEnded = true
        battleTimer?.cancel()
        sendBattleCompletedToServer() //发送 battleCompleted 消息
    }


    private fun sendBattleCompletedToServer() { //发送 battleCompleted 消息，包含必要信息
        val battleCompleted = JSONObject().apply {
            put("type", "battleCompleted")
            put("userId", userId)
            put("correctCount", correctCount)
            put("totalQuestions", questionCount)
            put("timeSpent", 30 - (millisUntilFinished.toInt() / 1000))
        }
        WebSocketManager.send(battleCompleted.toString())
    }

    private fun setupBattleUI(difficulty: String, questionCount: Int) {
        Toast.makeText(this, "对战设置：难度 $difficulty，题目数量 $questionCount", Toast.LENGTH_SHORT).show()
        this.questionCount = questionCount
    }

    private fun navigateToResultActivity(result: String, correctCount: Int, totalQuestions: Int, timeSpent: Int, score: Int) {
        val intent = Intent(this, ResultActivity::class.java).apply {
            putExtra("result", result)
            putExtra("correctCount", correctCount)
            putExtra("totalQuestions", totalQuestions)
            putExtra("timeSpent", timeSpent)
            putExtra("score", score) // 传递分数信息
        }
        startActivity(intent)
        finish()
    }

    private fun updateYourProgress(currentQuestionIndex: Int) {
        if (currentQuestionIndex > questionCount) return
        val progress = (currentQuestionIndex * 100 / questionCount).toFloat()
        yourProgressBar.progress = progress.toInt()
        yourStatusText.text = "进度: ${currentQuestionIndex}/$questionCount"
    }

    private fun updateOpponentStatus(opponentCorrectCount: Int, opponentTotalQuestions: Int) {
        if (opponentTotalQuestions > questionCount) return
        val progress = (opponentTotalQuestions * 100 / questionCount).toFloat()
        opponentProgressBar.progress = progress.toInt()
        opponentStatusText.text = "进度: ${opponentTotalQuestions}/$questionCount"
    }

    private fun sendStatusUpdate() {
        val statusUpdate = JSONObject().apply {
            put("type", "statusUpdate")
            put("correctCount", correctCount)
            put("totalQuestions", totalQuestions)
        }
        WebSocketManager.send(statusUpdate.toString())
    }

    private fun showWaitingForOpponent() { // 新增：显示等待对手完成的界面
        Toast.makeText(this, "等待对手完成对战...", Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        WebSocketManager.releaseConnection()
        battleTimer?.cancel()
        super.onDestroy()
    }
}