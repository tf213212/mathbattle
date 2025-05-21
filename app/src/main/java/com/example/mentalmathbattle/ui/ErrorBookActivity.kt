package com.example.mentalmathbattle.ui

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.mentalmathbattle.MainActivity
import com.example.mentalmathbattle.R
import okhttp3.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

class ErrorBookActivity : AppCompatActivity() {

    private lateinit var userInfo: TextView
    private lateinit var wrongList: ListView
    private lateinit var returnButton: Button  // 定义返回按钮
    private lateinit var repracticeButton: Button

    private var userId: Int = -1
    private val client = OkHttpClient()
    private val wrongQuestions = mutableListOf<JSONObject>()
    private val wrongListItems = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_error_book)

        // 从 SharedPreferences 获取数据
        val prefs = getSharedPreferences("user_prefs", MODE_PRIVATE)
        userId = prefs.getInt("userId", -1)

        userInfo = findViewById(R.id.user_info)
        wrongList = findViewById(R.id.wrong_question_list)

        // 获取用户信息并加载
        loadUserInfo()

        // 获取错题并加载
        loadWrongQuestions()

        // 初始化返回主界面按钮并设置点击事件
        returnButton = findViewById(R.id.return_to_main_button)
        returnButton.setOnClickListener {
            // 返回到主界面
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish() // 结束当前活动
        }

        repracticeButton = findViewById(R.id.repractice_button)
        repracticeButton.setOnClickListener {
            if (wrongQuestions.isEmpty()) {
                Toast.makeText(this, "当前没有错题可以重做", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            Log.d("WrongQuestions", wrongQuestions.toString())
            val intent = Intent(this, TrainingQuestionActivity::class.java)
            intent.putExtra("mode", "review")
            intent.putExtra("questionListJson", JSONArray(wrongQuestions).toString())
            intent.putExtra("userId", userId)
            startActivity(intent)
        }
    }

    private fun loadUserInfo() {
        val request = Request.Builder()
            .url("https://72bc-113-57-44-160.ngrok-free.app/api/user/$userId") // 使用正确的字符串插值
            .get()
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread { userInfo.text = "用户信息加载失败" }
            }

            override fun onResponse(call: Call, response: Response) {
                val json = JSONObject(response.body?.string() ?: "")
                val name = json.getString("username")
                val registerTime = json.getString("created_at")

                runOnUiThread {
                    userInfo.text = "用户名：$name\n"
                }
            }
        })
    }


    private fun loadWrongQuestions() {
        val request = Request.Builder()
            .url("https://72bc-113-57-44-160.ngrok-free.app/api/wrong-questions?userId=$userId")
            .get()
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@ErrorBookActivity, "错题加载失败", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string() ?: "[]"
                val jsonArray = JSONArray(body)

                // 关键修改2：同步清空两个数据源
                wrongQuestions.clear()
                wrongListItems.clear()

                for (i in 0 until jsonArray.length()) {
                    val item = jsonArray.getJSONObject(i)
                    wrongQuestions.add(item)
                    // 关键修改3：使用统一方法生成显示内容
                    wrongListItems.add(formatQuestionItem(item))
                }

                runOnUiThread {
                    // 关键修改4：使用成员变量初始化适配器
                    val adapter = ArrayAdapter(
                        this@ErrorBookActivity,
                        android.R.layout.simple_list_item_1,
                        wrongListItems
                    )
                    wrongList.adapter = adapter

                    wrongList.onItemLongClickListener = AdapterView.OnItemLongClickListener { _, _, position, _ ->
                        showDeleteDialog(position)
                        true
                    }
                }
            }
        })
    }

    // 新增方法：统一格式化显示内容
    private fun formatQuestionItem(item: JSONObject): String {
        val question = item.getString("question")
        val userAnswer = item.getInt("user_answer")
        val correctAnswer = item.getInt("correct_answer")
        return "题目：$question\n你的答案：$userAnswer，正确答案：$correctAnswer"
    }

    private fun deleteWrongQuestion(position: Int) {
        val questionId = wrongQuestions[position].getInt("id")

        val request = Request.Builder()
            .url("https://72bc-113-57-44-160.ngrok-free.app/api/wrong-questions/$questionId")
            .delete()
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@ErrorBookActivity, "删除失败，请检查网络", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    // 关键修改5：同步删除两个数据源
                    wrongQuestions.removeAt(position)
                    wrongListItems.removeAt(position) // 新增此行

                    runOnUiThread {
                        // 关键修改6：正确刷新列表
                        (wrongList.adapter as ArrayAdapter<String>).notifyDataSetChanged()
                        Toast.makeText(this@ErrorBookActivity, "删除成功", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    runOnUiThread {
                        Toast.makeText(this@ErrorBookActivity, "删除失败: ${response.code}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }



    private fun showDeleteDialog(position: Int) {
        AlertDialog.Builder(this)
            .setTitle("删除确认")
            .setMessage("确定要删除这条错题记录吗？")
            .setPositiveButton("删除") { dialog, which ->
                deleteWrongQuestion(position)
            }
            .setNegativeButton("取消", null)
            .show()
    }



}