package com.example.mentalmathbattle.fragments

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import com.example.mentalmathbattle.R
import com.example.mentalmathbattle.ui.TrainingExerciseActivity
import com.example.mentalmathbattle.ui.TrainingHistoryActivity
import com.example.mentalmathbattle.ui.TrainingStatisticsActivity

class TrainingFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_training, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 开始练习按钮点击事件
        view.findViewById<Button>(R.id.training_exercise_button).setOnClickListener {
            val intent = Intent(requireContext(), TrainingExerciseActivity::class.java)
            intent.putExtra("userId", arguments?.getInt("userId", -1))
            startActivity(intent)
        }

        // 练习历史按钮点击事件
        view.findViewById<Button>(R.id.training_history_button).setOnClickListener {
            val intent = Intent(requireContext(), TrainingHistoryActivity::class.java)
            intent.putExtra("userId", arguments?.getInt("userId", -1))
            startActivity(intent)
        }

        // 练习统计按钮点击事件
        view.findViewById<Button>(R.id.training_statistics_button).setOnClickListener {
            val intent = Intent(requireContext(), TrainingStatisticsActivity::class.java)
            intent.putExtra("userId", arguments?.getInt("userId", -1))
            startActivity(intent)
        }
    }
}