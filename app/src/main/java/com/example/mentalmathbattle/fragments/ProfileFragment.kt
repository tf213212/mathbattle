package com.example.mentalmathbattle.fragments

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.example.mentalmathbattle.R
import com.example.mentalmathbattle.ui.LoginActivity
import com.example.mentalmathbattle.ui.ProfileSettingsActivity
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.Response
import okio.IOException
import org.json.JSONObject
import java.io.File
import java.io.InputStream
import com.bumptech.glide.Glide
import com.example.mentalmathbattle.ui.EmailBindActivity


class ProfileFragment : Fragment() {

    private lateinit var avatarImageView: ImageView
    private lateinit var usernameTextView: TextView
    private lateinit var emailTextView: TextView
    private lateinit var scoreTextView: TextView


    private val PICK_IMAGE_REQUEST = 1
    private val REQUEST_EMAIL_BIND = 1001
    private var userId: Int = -1


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        userId = arguments?.getInt("userId") ?: -1


        // 初始化视图
        avatarImageView = view.findViewById(R.id.profile_avatar)
        usernameTextView = view.findViewById(R.id.profile_username)
        emailTextView = view.findViewById(R.id.profile_email)
        scoreTextView=view.findViewById(R.id.profile_score)


        // 上传头像按钮
        view.findViewById<Button>(R.id.upload_avatar_button).setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(intent, PICK_IMAGE_REQUEST)
        }

        // 修改密码按钮
        view.findViewById<Button>(R.id.profile_settings_button).setOnClickListener {
            startActivity(Intent(requireContext(), ProfileSettingsActivity::class.java))
        }


        // 邮箱绑定按钮
        view.findViewById<Button>(R.id.profile_emailbind_button).setOnClickListener {
            val intent = Intent(requireContext(), EmailBindActivity::class.java)
            startActivityForResult(intent, REQUEST_EMAIL_BIND)
        }


        // 退出登录按钮
        view.findViewById<Button>(R.id.profile_logout_button).setOnClickListener {
            showLogoutConfirmationDialog()
        }

        // 加载用户信息
        loadUserInfo()
    }

    private fun uploadAvatarToServer(imageUri: Uri) {
        val inputStream = requireContext().contentResolver.openInputStream(imageUri)
        val file = File(requireContext().cacheDir, "avatar.jpg")
        inputStream?.use { input ->
            file.outputStream().use { output ->
                input.copyTo(output)
            }
        }

        val client = OkHttpClient()
        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("avatar", file.name, file.asRequestBody("image/jpeg".toMediaTypeOrNull()))

            .addFormDataPart("userId", userId.toString()) // 添加用户ID

            .build()


        val request = Request.Builder()
            .url("https://72bc-113-57-44-160.ngrok-free.app/api/user/upload-avatar")  // Android 模拟器访问本地服务器
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                requireActivity().runOnUiThread {
                    Toast.makeText(requireContext(), "上传失败: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string()
                requireActivity().runOnUiThread {
                    if (response.isSuccessful && body != null) {
                        // 提取图片 URL
                        val imageUrl = JSONObject(body).getString("url")
                        val fullUrl = "https://72bc-113-57-44-160.ngrok-free.app$imageUrl"

                        // 保存到 SharedPreferences
                        val prefs = requireActivity().getSharedPreferences("user_prefs", android.content.Context.MODE_PRIVATE)
                        prefs.edit().putString("avatarUrl", fullUrl).apply()

                        // 重新加载头像
                        Glide.with(this@ProfileFragment)
                            .load(fullUrl)
                            .placeholder(R.drawable.ic_default_avatar)
                            .into(avatarImageView)

                        Toast.makeText(requireContext(), "头像上传成功", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(requireContext(), "上传失败: $body", Toast.LENGTH_SHORT).show()
                    }
                }
            }

        })
    }



    private fun loadUserInfo() {
        val prefs = requireActivity().getSharedPreferences("user_prefs", android.content.Context.MODE_PRIVATE)
        val username = prefs.getString("username", "未登录")
        val email = prefs.getString("email", "未绑定")
        val score=prefs.getInt("score",0)
        val avatarUrl = prefs.getString("avatarUrl", null)
        val fullUrl = if (avatarUrl != null && avatarUrl.startsWith("/")) {
            "https://72bc-113-57-44-160.ngrok-free.app$avatarUrl"
        } else {
            avatarUrl
        }

        if (fullUrl != null) {
            Glide.with(this)
                .load(fullUrl)
                .placeholder(R.drawable.ic_default_avatar)
                .into(avatarImageView)
        }
        usernameTextView.text = "用户名: $username"
        emailTextView.text = "邮箱: $email"
        scoreTextView.text="积分:$score"

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_EMAIL_BIND && resultCode == Activity.RESULT_OK) {
            // 绑定邮箱成功，刷新用户信息
            loadUserInfo()
        }
        // 你已有的头像选择结果处理保持不变
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK) {
            val imageUri: Uri? = data?.data
            if (imageUri != null) {
                val inputStream: InputStream? = requireContext().contentResolver.openInputStream(imageUri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                avatarImageView.setImageBitmap(bitmap)

                uploadAvatarToServer(imageUri)  // 上传头像
            }
        }
    }



    private fun showLogoutConfirmationDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("确认退出")
            .setMessage("确定要退出登录吗？")
            .setPositiveButton("是") { _, _ ->
                val prefs = requireActivity().getSharedPreferences("user_prefs", android.content.Context.MODE_PRIVATE)
                prefs.edit().clear().apply()

                val intent = Intent(requireContext(), LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK

                startActivity(intent)
            }
            .setNegativeButton("否", null)
            .create()
            .show()
    }
}
