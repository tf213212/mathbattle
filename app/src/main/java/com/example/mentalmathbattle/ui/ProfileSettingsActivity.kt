package com.example.mentalmathbattle.ui

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.mentalmathbattle.R
import okhttp3.Call
import okhttp3.Callback
import okhttp3.FormBody
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okio.IOException
import org.json.JSONObject

class ProfileSettingsActivity : AppCompatActivity() {

    private lateinit var currentPasswordEditText: EditText
    private lateinit var newPasswordEditText: EditText
    private lateinit var confirmPasswordEditText: EditText
    private lateinit var saveButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile_settings)

        currentPasswordEditText = findViewById(R.id.current_password_edit_text)
        newPasswordEditText = findViewById(R.id.new_password_edit_text)
        confirmPasswordEditText = findViewById(R.id.confirm_password_edit_text)
        saveButton = findViewById(R.id.save_button)

        saveButton.setOnClickListener {
            val currentPassword = currentPasswordEditText.text.toString()
            val newPassword = newPasswordEditText.text.toString()
            val confirmPassword = confirmPasswordEditText.text.toString()

            if (newPassword == confirmPassword) {
                changePassword(currentPassword, newPassword)
            } else {
                Toast.makeText(this, "两次输入的密码不一致", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun changePassword(currentPassword: String, newPassword: String) {
        val sharedPrefs = getSharedPreferences("user_prefs", MODE_PRIVATE)
        val userId = sharedPrefs.getInt("userId", -1)

        if (userId == -1) {
            Toast.makeText(this, "未登录，请重新登录", Toast.LENGTH_SHORT).show()
            return
        }

        val client = OkHttpClient()

        val json = JSONObject().apply {
            put("userId", userId)
            put("oldPassword", currentPassword)
            put("newPassword", newPassword)
        }

        val requestBody = json.toString()
            .toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())

        val request = Request.Builder()
            .url("https://72bc-113-57-44-160.ngrok-free.app/api/user/change-password")
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@ProfileSettingsActivity, "修改失败: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string()
                runOnUiThread {
                    if (response.isSuccessful && body != null) {
                        val jsonResponse = JSONObject(body)
                        val message = jsonResponse.optString("message", "修改成功")
                        Toast.makeText(this@ProfileSettingsActivity, message, Toast.LENGTH_SHORT).show()
                        finish()
                    } else {
                        val errorMessage = try {
                            JSONObject(body ?: "").getString("message")
                        } catch (e: Exception) {
                            "密码修改失败"
                        }
                        Toast.makeText(this@ProfileSettingsActivity, errorMessage, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }

}
