package com.example.mentalmathbattle.ui

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.os.CountDownTimer
import android.text.Editable
import android.text.TextWatcher
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.mentalmathbattle.R
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import org.json.JSONObject
import java.io.IOException

class EmailBindActivity : AppCompatActivity() {

    private lateinit var emailEditText: EditText
    private lateinit var codeEditText: EditText
    private lateinit var sendCodeButton: Button
    private lateinit var bindButton: Button
    private var countDownTimer: CountDownTimer? = null
    private val client = OkHttpClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_email_bind)

        emailEditText = findViewById(R.id.editTextEmail)
        codeEditText = findViewById(R.id.editTextCode)
        sendCodeButton = findViewById(R.id.buttonSendCode)
        bindButton = findViewById(R.id.buttonBindEmail)

        //  添加邮箱输入格式实时校验
        emailEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                val email = s.toString()
                if (email.isNotBlank() && !isValidEmail(email)) {
                    emailEditText.error = "邮箱格式无效"
                }
            }
        })

        sendCodeButton.setOnClickListener {
            val email = emailEditText.text.toString()
            if (!isValidEmail(email)) {
                Toast.makeText(this, "请输入有效的邮箱地址", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            // 立即禁用按钮
            sendCodeButton.isEnabled = false
            sendCodeButton.setTextColor(Color.GRAY)

            // 启动倒计时（10秒）
            countDownTimer = object : CountDownTimer(10000, 1000) {
                override fun onTick(millisUntilFinished: Long) {
                    sendCodeButton.text = "${millisUntilFinished / 6000}秒后重试"
                }

                override fun onFinish() {
                    sendCodeButton.isEnabled = true
                    sendCodeButton.setTextColor(Color.WHITE)
                    sendCodeButton.text = "发送验证码"
                }
            }.start()

            sendCode(email)
        }

        bindButton.setOnClickListener {
            val email = emailEditText.text.toString()
            val code = codeEditText.text.toString()

            if (!isValidEmail(email)) {
                Toast.makeText(this, "请输入有效的邮箱地址", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (code.isBlank()) {
                Toast.makeText(this, "请输入验证码", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            bindEmail(email, code)
        }
    }

    //  邮箱格式验证函数
    private fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    private fun sendCode(email: String) {
        val json = JSONObject()
        json.put("email", email)

        val requestBody = RequestBody.create(
            "application/json; charset=utf-8".toMediaTypeOrNull(),
            json.toString()
        )

        val request = Request.Builder()
            .url("https://72bc-113-57-44-160.ngrok-free.app/api/user/send-email-code")
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@EmailBindActivity, "发送失败: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                runOnUiThread {
                    Toast.makeText(this@EmailBindActivity, "验证码已发送", Toast.LENGTH_SHORT).show()
                }
            }
        })
    }

    private fun bindEmail(email: String, code: String) {
        val sharedPreferences = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val userId = sharedPreferences.getInt("userId", -1)

        val json = JSONObject()
        json.put("email", email)
        json.put("code", code)
        json.put("userId", userId)

        val requestBody = RequestBody.create(
            "application/json; charset=utf-8".toMediaTypeOrNull(),
            json.toString()
        )

        val request = Request.Builder()
            .url("https://72bc-113-57-44-160.ngrok-free.app/api/user/bind-email")
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@EmailBindActivity, "绑定失败: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseText = response.body?.string()
                runOnUiThread {
                    val json = JSONObject(responseText ?: "{}")
                    if (json.optBoolean("success")) {
                        Toast.makeText(this@EmailBindActivity, "绑定成功", Toast.LENGTH_SHORT).show()

                        // 保存邮箱到 SharedPreferences
                        val prefs = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
                        prefs.edit().putString("email", email).apply()

                        setResult(Activity.RESULT_OK)  // 通知 ProfileFragment 成功
                        finish()
                    } else {
                        Toast.makeText(this@EmailBindActivity, "绑定失败: ${json.optString("message")}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }
}
