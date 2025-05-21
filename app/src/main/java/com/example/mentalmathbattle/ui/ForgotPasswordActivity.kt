package com.example.mentalmathbattle.ui
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.mentalmathbattle.R
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okio.IOException
import org.json.JSONObject

class ForgotPasswordActivity : AppCompatActivity() {
    private lateinit var emailInput: EditText
    private lateinit var codeInput: EditText
    private lateinit var newPasswordInput: EditText
    private lateinit var sendCodeButton: Button
    private lateinit var resetPasswordButton: Button
    private val client = OkHttpClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_password)

        emailInput = findViewById(R.id.email_input)
        codeInput = findViewById(R.id.code_input)
        newPasswordInput = findViewById(R.id.new_password_input)
        sendCodeButton = findViewById(R.id.send_code_button)
        resetPasswordButton = findViewById(R.id.reset_password_button)

        emailInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                val email = s.toString()
                if (email.isNotBlank() && !isValidEmail(email)) {
                    emailInput.error = "邮箱格式无效"
                }
            }
        })

        sendCodeButton.setOnClickListener {
            val email = emailInput.text.toString()
            sendResetCode(email)
        }

        resetPasswordButton.setOnClickListener {
            val email = emailInput.text.toString()
            val code = codeInput.text.toString()
            val newPassword = newPasswordInput.text.toString()
            if (newPassword.isBlank()) {
                Toast.makeText(this, "新密码不能为空", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            resetPassword(email, code, newPassword)
        }
    }
    //  邮箱格式验证函数
    private fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }


    private fun sendResetCode(email: String) {
        val json = JSONObject().apply { put("email", email) }
        val body = json.toString().toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())

        val request = Request.Builder()
            .url("https://72bc-113-57-44-160.ngrok-free.app/api/send-reset-code")
            .post(body)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@ForgotPasswordActivity, "发送失败", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                runOnUiThread {
                    Toast.makeText(this@ForgotPasswordActivity, "验证码已发送", Toast.LENGTH_SHORT).show()
                }
            }
        })
    }

    private fun resetPassword(email: String, code: String, newPassword: String) {
        val json = JSONObject().apply {
            put("email", email)
            put("code", code)
            put("newPassword", newPassword)
        }

        val body = json.toString().toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
        val request = Request.Builder()
            .url("https://72bc-113-57-44-160.ngrok-free.app/api/reset-password")
            .post(body)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@ForgotPasswordActivity, "重置失败", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val jsonResponse = JSONObject(response.body?.string() ?: "")
                val success = jsonResponse.optBoolean("success", false)
                runOnUiThread {
                    if (success) {
                        Toast.makeText(this@ForgotPasswordActivity, "密码已重置", Toast.LENGTH_SHORT).show()
                        finish()
                    } else {
                        Toast.makeText(this@ForgotPasswordActivity, jsonResponse.optString("message"), Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }
}
