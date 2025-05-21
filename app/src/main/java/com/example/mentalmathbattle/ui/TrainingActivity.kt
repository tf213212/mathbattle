package com.example.mentalmathbattle.ui

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.mentalmathbattle.MainActivity
import com.example.mentalmathbattle.R


class TrainingActivity : AppCompatActivity() {

    private lateinit var questionCountSpinner: Spinner
    private lateinit var startPracticeButton: Button
    private lateinit var backToMainButton: Button
    private lateinit var userIdTextView: TextView

    private lateinit var difficultySpinner: Spinner

    private var userId: Int = -1 // 用于传递用户信息

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_training)

        // 从 SharedPreferences 获取数据
        val prefs = getSharedPreferences("user_prefs", MODE_PRIVATE)
        userId = prefs.getInt("userId", -1)

        questionCountSpinner = findViewById(R.id.spinner_question_count)
        startPracticeButton = findViewById(R.id.button_start_practice)
        backToMainButton = findViewById(R.id.button_back_main)
        userIdTextView = findViewById(R.id.text_user_id)
        difficultySpinner = findViewById(R.id.spinner_difficulty)

        userIdTextView.text = "当前用户ID：$userId"

        val questionOptions = arrayOf("5题", "10题", "20题")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, questionOptions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        questionCountSpinner.adapter = adapter


        val difficultyOptions = arrayOf("简单", "中等", "困难")
        val difficultyMap = mapOf(
            "简单" to "easy",
            "中等" to "medium",
            "困难" to "hard"
        )
        val difficultySpinner: Spinner = findViewById(R.id.spinner_difficulty)
        val difficultyAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, difficultyOptions)
        difficultyAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        difficultySpinner.adapter = difficultyAdapter

        startPracticeButton.setOnClickListener {
            val selectedQuestionCount = questionCountSpinner.selectedItem.toString().replace("题", "").toInt()
            val selectedDifficultyText = difficultySpinner.selectedItem.toString()
            val difficultyCode = difficultyMap[selectedDifficultyText] ?: "medium"  // 默认中等

            val intent = Intent(this, TrainingQuestionActivity::class.java)
            intent.putExtra("userId", userId)
            intent.putExtra("questionCount", selectedQuestionCount)
            intent.putExtra("difficulty", difficultyCode)
            startActivity(intent)
        }


        backToMainButton.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra("userId", userId) // 正确地把 userId 放进去
            startActivity(intent)
            finish()
        }
    }
}
