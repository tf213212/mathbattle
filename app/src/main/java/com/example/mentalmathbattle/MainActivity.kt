package com.example.mentalmathbattle

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.mentalmathbattle.fragments.HomeFragment
import com.example.mentalmathbattle.ui.LoginActivity
import com.example.mentalmathbattle.fragments.ProfileFragment
import com.example.mentalmathbattle.fragments.TrainingFragment



class MainActivity : AppCompatActivity() {

    private var userId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        userId = intent.getIntExtra("userId", -1)

        // 初始化导航栏点击事件
        findViewById<ImageView>(R.id.nav_home).setOnClickListener {
            showFragment(HomeFragment(), userId, R.anim.fade_in, R.anim.fade_out)
        }

        findViewById<ImageView>(R.id.nav_training).setOnClickListener {
            showFragment(TrainingFragment(), userId, R.anim.fade_in, R.anim.fade_out)
        }

        findViewById<ImageView>(R.id.nav_profile).setOnClickListener {
            showFragment(ProfileFragment(), userId, R.anim.fade_in, R.anim.fade_out)
        }

        // 默认显示主界面
        showFragment(HomeFragment(), userId, 0, 0)
    }

    private fun showFragment(fragment: Fragment, userId: Int, enterAnim: Int, exitAnim: Int) {
        val bundle = Bundle().apply {
            putInt("userId", userId)
        }
        fragment.arguments = bundle

        supportFragmentManager.beginTransaction()
            .setCustomAnimations(enterAnim, exitAnim)
            .replace(R.id.content_frame, fragment)
            .commit()
    }
}