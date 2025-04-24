package com.example.mentalmathbattle.fragments

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import com.example.mentalmathbattle.R
import com.example.mentalmathbattle.ui.LoginActivity
import com.example.mentalmathbattle.ui.ProfileSettingsActivity
import com.example.mentalmathbattle.ui.ProfileStatisticsActivity

class ProfileFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 设置按钮点击事件
        view.findViewById<Button>(R.id.profile_settings_button).setOnClickListener {
            val intent = Intent(requireContext(), ProfileSettingsActivity::class.java)
            intent.putExtra("userId", arguments?.getInt("userId", -1))
            startActivity(intent)
        }

        // 我的统计按钮点击事件
        view.findViewById<Button>(R.id.profile_statistics_button).setOnClickListener {
            val intent = Intent(requireContext(), ProfileStatisticsActivity::class.java)
            intent.putExtra("userId", arguments?.getInt("userId", -1))
            startActivity(intent)
        }

        // 退出登录按钮点击事件
        view.findViewById<Button>(R.id.profile_logout_button).setOnClickListener {
            showLogoutConfirmationDialog()
        }
    }

    private fun showLogoutConfirmationDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("确认退出")
            .setMessage("确定要退出登录吗？")
            .setPositiveButton("是") { _, _ ->
                // 退出登录逻辑
                val prefs = requireActivity().getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
                prefs.edit().remove("isLoggedIn").apply()

                val intent = Intent(requireContext(), LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
            }
            .setNegativeButton("否", null)
            .create()
            .show()
    }
}