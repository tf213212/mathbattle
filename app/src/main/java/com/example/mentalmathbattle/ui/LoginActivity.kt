package com.example.mentalmathbattle.ui


import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.mentalmathbattle.MainActivity
import com.example.mentalmathbattle.R
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

class LoginActivity : AppCompatActivity() {

    private lateinit var usernameInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var loginButton: Button
    private lateinit var loginErrorText: TextView
    private lateinit var registerButton: Button
    private lateinit var forgotPasswordButton: Button

    private val client = OkHttpClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        usernameInput = findViewById(R.id.username_input)
        passwordInput = findViewById(R.id.password_input)
        loginButton = findViewById(R.id.login_button)
        loginErrorText = findViewById(R.id.login_error_text)
        registerButton = findViewById(R.id.register_button)
        forgotPasswordButton = findViewById(R.id.forgot_password_button)


        registerButton.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }

        loginButton.setOnClickListener {
            val username = usernameInput.text.toString()
            val password = passwordInput.text.toString()
            login(username, password)
        }
        forgotPasswordButton.setOnClickListener {
            val intent = Intent(this, ForgotPasswordActivity::class.java)
            startActivity(intent)
        }
    }

    private fun login(username: String, password: String) {
        val json = JSONObject().apply {
            put("username", username)
            put("password", password)
        }

        val requestBody = json.toString().toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
        val serverUrl = "https://72bc-113-57-44-160.ngrok-free.app/api/login"

        val request = Request.Builder()
            .url(serverUrl)
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    loginErrorText.text = "无法连接到服务器，请检查网络连接"
                    loginErrorText.visibility = TextView.VISIBLE
                }
                Log.e("Login", "网络请求失败", e)
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                runOnUiThread {
                    if (response.isSuccessful) {
                        val jsonResponse = JSONObject(responseBody ?: "")
                        val userId = jsonResponse.optInt("userId", -1)

                        if (userId != -1) {
                            val username = jsonResponse.optString("username", "未登录")
                            val email = jsonResponse.optString("email", "未绑定")
                            val avatarUrl = jsonResponse.optString("avatarUrl", null)
                            var score=jsonResponse.optInt("score", 0)

                            val sharedPref = getSharedPreferences("user_prefs", MODE_PRIVATE)
                            with(sharedPref.edit()) {
                                putBoolean("is_logged_in", true)
                                putInt("userId", userId)
                                putInt("score", score)
                                putString("username", username)
                                putString("email", email)
                                putString("avatarUrl", avatarUrl)
                                apply()
                            }

                            Toast.makeText(this@LoginActivity, "登录成功！", Toast.LENGTH_SHORT).show()
                            val intent = Intent(this@LoginActivity, MainActivity::class.java)
                            intent.putExtra("userId", userId)
                            startActivity(intent)
                            finish()
                        }
                        else {
                            loginErrorText.text = "登录失败，请检查用户名和密码"
                            loginErrorText.visibility = TextView.VISIBLE
                            Toast.makeText(this@LoginActivity, "登录失败，请检查用户名和密码", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        val message = try {
                            JSONObject(responseBody ?: "").getString("message")
                        } catch (e: Exception) {
                            "登录失败，请检查用户名和密码"
                        }
                        loginErrorText.text = message
                        loginErrorText.visibility = TextView.VISIBLE
                        Toast.makeText(this@LoginActivity, message, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }
}