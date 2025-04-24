package com.example.mentalmathbattle.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.mentalmathbattle.MainActivity
import com.example.mentalmathbattle.R

class ResultActivity : AppCompatActivity() {

    private lateinit var resultText: TextView
    private lateinit var scoreText: TextView
    private lateinit var accuracyText: TextView
    private lateinit var timeText: TextView
    private lateinit var backToMainButton: Button
    private lateinit var rematchButton: Button
    private lateinit var resultIcon: ImageView
    private lateinit var correctCountText: TextView
    private lateinit var totalQuestionsText: TextView
    private var userId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_result)

        resultText = findViewById(R.id.result_text)
        scoreText = findViewById(R.id.score_text)
        accuracyText = findViewById(R.id.accuracy_text)
        timeText = findViewById(R.id.time_text)
        backToMainButton = findViewById(R.id.back_to_main_button)
        rematchButton = findViewById(R.id.rematch_button)
        resultIcon = findViewById(R.id.result_icon)
        correctCountText = findViewById(R.id.correct_count_text)
        totalQuestionsText = findViewById(R.id.total_questions_text)

        userId = intent.getIntExtra("userId", -1)
        val correctCount = intent.getIntExtra("correctCount", 0)
        val totalQuestions = intent.getIntExtra("totalQuestions", 1)
        val result = intent.getStringExtra("result") ?: "fail"
        val timeSpent = intent.getIntExtra("timeSpent", 30)

        val score = correctCount * 10
        val accuracy = if (totalQuestions > 0) {
            (correctCount * 100 / totalQuestions)
        } else {
            0
        }

        // 更新结果文本和图标
        when (result) {
            "win" -> {
                resultText.text = "🎉 你赢了！"
                //resultIcon.setImageResource(R.drawable.ic_win) // 胜利图标
            }
            "lose" -> {
                resultText.text = "😢 你输了"
                //resultIcon.setImageResource(R.drawable.ic_lose) // 失败图标
            }
            "draw" -> {
                resultText.text = "👏 表现不错，继续努力！"
                //resultIcon.setImageResource(R.drawable.ic_draw) // 平局图标
            }
            else -> {
                resultText.text = "对战结束"
                //resultIcon.setImageResource(R.drawable.ic_default) // 默认图标
            }
        }

        scoreText.text = "得分：$score"
        accuracyText.text = "答题正确率：$accuracy% ($correctCount / $totalQuestions)"
        timeText.text = "用时：${timeSpent}秒"

        // 更新正确题数和总题数显示
        correctCountText.text = "正确题数: $correctCount"
        totalQuestionsText.text = "总题数: $totalQuestions"

        // 设置按钮点击事件
        backToMainButton.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra("userId", userId)
            startActivity(intent)
            finish()
        }

        rematchButton.setOnClickListener {
            val intent = Intent(this, MatchActivity::class.java)
            intent.putExtra("userId", userId)
            startActivity(intent)
            finish()
        }
    }
}