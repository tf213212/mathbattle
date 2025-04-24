package com.example.mentalmathbattle.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.mentalmathbattle.R
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import org.json.JSONObject
import java.io.IOException

class RegisterActivity : AppCompatActivity() {

    private lateinit var usernameInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var registerButton: Button
    private lateinit var registerErrorText: TextView

    private val client = OkHttpClient()
    private val serverUrl = "http://10.0.2.2:8080/api/register"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        usernameInput = findViewById(R.id.username_input)
        passwordInput = findViewById(R.id.password_input)
        registerButton = findViewById(R.id.register_button)
        registerErrorText = findViewById(R.id.register_error_text)

        registerButton.setOnClickListener {
            val username = usernameInput.text.toString()
            val password = passwordInput.text.toString()
            register(username, password)
        }
    }

    private fun register(username: String, password: String) {
        val json = JSONObject()
        json.put("username", username)
        json.put("password", password)

        val requestBody = RequestBody.create(
            "application/json; charset=utf-8".toMediaTypeOrNull(),
            json.toString()
        )

        val request = Request.Builder()
            .url(serverUrl)
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    registerErrorText.text = "无法连接到服务器"
                    registerErrorText.visibility = TextView.VISIBLE
                    Toast.makeText(this@RegisterActivity, "注册失败：无法连接服务器", Toast.LENGTH_SHORT).show()
                }
                Log.e("Register", "网络请求失败", e)
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                runOnUiThread {
                    if (response.isSuccessful) {
                        Toast.makeText(this@RegisterActivity, "注册成功，请返回登录", Toast.LENGTH_SHORT).show()
                        val intent = Intent(this@RegisterActivity, LoginActivity::class.java)
                        startActivity(intent)
                        finish()
                    } else {
                        val message = try {
                            JSONObject(responseBody ?: "").getString("message")
                        } catch (e: Exception) {
                            "注册失败"
                        }
                        registerErrorText.text = message
                        registerErrorText.visibility = TextView.VISIBLE
                        Toast.makeText(this@RegisterActivity, message, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }
}
