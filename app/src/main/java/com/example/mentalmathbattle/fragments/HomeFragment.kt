package com.example.mentalmathbattle.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.mentalmathbattle.R
import com.example.mentalmathbattle.ui.LeaderboardActivity
import com.example.mentalmathbattle.ui.MatchActivity
import com.example.mentalmathbattle.ui.ErrorBookActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView

class HomeFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val userId = arguments?.getInt("userId") ?: -1

        // 初始化三个功能卡片（卡片和按钮双重点击）
        initCardWithButton(
            view = view,
            cardId = R.id.match_card,
            buttonId = R.id.match_battle_button,
            targetActivity = MatchActivity::class.java,
            userId = userId
        )

        initCardWithButton(
            view = view,
            cardId = R.id.error_card,
            buttonId = R.id.error_book_button,
            targetActivity = ErrorBookActivity::class.java,
            userId = userId
        )

        initCardWithButton(
            view = view,
            cardId = R.id.leaderboard_card,
            buttonId = R.id.leaderboard_button,
            targetActivity = LeaderboardActivity::class.java,
            userId = userId
        )
    }

    private fun initCardWithButton(
        view: View,
        cardId: Int,
        buttonId: Int,
        targetActivity: Class<*>,
        userId: Int
    ) {
        // 卡片点击事件
        view.findViewById<MaterialCardView>(cardId).setOnClickListener {
            navigateToActivity(targetActivity, userId)
        }

        // 按钮点击事件（保持原有逻辑）
        view.findViewById<MaterialButton>(buttonId).setOnClickListener {
            navigateToActivity(targetActivity, userId)
        }
    }

    private fun navigateToActivity(activityClass: Class<*>, userId: Int) {
        startActivity(Intent(requireContext(), activityClass).apply {
            putExtra("userId", userId)
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        })
    }
}