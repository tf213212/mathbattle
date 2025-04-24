package com.example.mentalmathbattle.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.mentalmathbattle.MainActivity
import com.example.mentalmathbattle.R

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // 模拟检查登录状态
        // 实际项目中，这里应该从 SharedPreferences 或其他存储中读取登录状态
        val isLoggedIn = false // 假设用户未登录

        if (isLoggedIn) {
            // 如果已登录，跳转到主活动
            startActivity(Intent(this, MainActivity::class.java))
        } else {
            // 如果未登录，跳转到登录活动
            startActivity(Intent(this, LoginActivity::class.java))
        }
        finish()
    }
}