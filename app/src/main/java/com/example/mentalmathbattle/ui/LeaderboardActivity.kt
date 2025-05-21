package com.example.mentalmathbattle.ui

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.mentalmathbattle.R
import okhttp3.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

data class LeaderboardItem(
    val rank: Int,        // 根据数组索引生成
    val username: String, // 与 JSON 字段名一致
    val avatar: String,   // 使用 avatar 而非 avatarUrl
    val score: Int
)

class LeaderboardAdapter(
    context: Context,
    private val items: List<LeaderboardItem>
) : ArrayAdapter<LeaderboardItem>(context, R.layout.item_leaderboard, items) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.item_leaderboard, parent, false)
        val item = items[position]

        view.findViewById<TextView>(R.id.rank).text = "#${item.rank}"
        view.findViewById<TextView>(R.id.username).text = item.username
        view.findViewById<TextView>(R.id.score).text = "${item.score} 分"

        // 加载完整 URL 头像
        Glide.with(context)
            .load(item.avatar)
            .placeholder(R.drawable.ic_default_avatar)
            .error(R.drawable.ic_default_avatar)
            .circleCrop()
            .into(view.findViewById(R.id.avatar))

        return view
    }
}

class LeaderboardActivity : AppCompatActivity() {
    private lateinit var listView: ListView
    private val client = OkHttpClient()
    private val items = mutableListOf<LeaderboardItem>()
    private val BASE_URL = "https://72bc-113-57-44-160.ngrok-free.app" // 服务器基础地址

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_leaderboard)
        listView = findViewById(R.id.leaderboard_list)
        loadLeaderboard()
    }

    private fun loadLeaderboard() {
        val request = Request.Builder()
            .url("$BASE_URL/api/leaderboard")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                showToast("网络请求失败: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                response.body?.string()?.let { body ->
                    try {
                        parseResponse(body)
                    } catch (e: Exception) {
                        showToast("数据解析错误: ${e.message}")
                    }
                } ?: showToast("服务器返回空数据")
            }
        })
    }

    private fun parseResponse(body: String) {
        val rootObject = JSONObject(body)
        when (rootObject.getInt("code")) {
            200 -> {
                val dataArray = rootObject.getJSONArray("data")
                items.clear()
                val sortedList = mutableListOf<JSONObject>().apply {
                    for (i in 0 until dataArray.length()) {
                        add(dataArray.getJSONObject(i))
                    }
                    // 按分数降序排序
                    sortByDescending { it.getInt("score") }
                }

                // 生成正确排名
                sortedList.forEachIndexed { index, item ->
                    items.add(
                        LeaderboardItem(
                            rank = index + 1, // 根据排序后的位置生成排名
                            username = item.getString("username"),
                            avatar = "$BASE_URL${item.getString("avatar")}",
                            score = item.getInt("score")
                        )
                    )
                }

                updateListView()
            }
            else -> showToast("服务器错误: ${rootObject.getString("message")}")
        }
    }

    private fun updateListView() {
        runOnUiThread {
            listView.adapter = LeaderboardAdapter(this@LeaderboardActivity, items)
        }
    }

    private fun showToast(message: String) {
        runOnUiThread {
            Toast.makeText(this@LeaderboardActivity, message, Toast.LENGTH_SHORT).show()
        }
    }
}