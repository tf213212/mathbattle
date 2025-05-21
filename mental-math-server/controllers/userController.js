const bcrypt = require('bcrypt');
const multer = require('multer');
const path = require('path');
const db = require('../db');
const nodemailer = require('nodemailer');

// 验证码缓存（内存存储，可替换为 Redis）
const codeStore = new Map(); // key: email/phone, value: { code, expires }

// ========== 上传头像 ========== //
const storage = multer.diskStorage({
  destination: function (req, file, cb) {
    cb(null, 'uploads/avatars/');
  },
  filename: function (req, file, cb) {
    const ext = path.extname(file.originalname);
    const filename = `avatar_${Date.now()}${ext}`;
    cb(null, filename);
  }
});

const upload = multer({
  storage: storage,
  limits: { fileSize: 2 * 1024 * 1024 },
  fileFilter: (req, file, cb) => {
    const allowedTypes = ['image/jpeg', 'image/png'];
    if (!allowedTypes.includes(file.mimetype)) {
      return cb(new Error('仅允许上传 JPG 或 PNG 格式的图片'), false);
    }
    cb(null, true);
  }
});

exports.uploadAvatar = [
  upload.single('avatar'),
  async (req, res) => {
    const userId = req.body.userId;
    if (!req.file || !userId) {
      return res.status(400).json({ message: '缺少文件或用户ID' });
    }
    const imageUrl = `/uploads/avatars/${req.file.filename}`;
    try {
      await db.query('UPDATE users SET avatar_url = ? WHERE id = ?', [imageUrl, userId]);
      res.status(200).json({ message: '上传成功', url: imageUrl });
    } catch (error) {
      console.error('更新头像路径失败:', error.message);
      res.status(500).json({ message: '服务器错误' });
    }
  }
];

// ========== 修改密码 ========== //
exports.changePassword = async (req, res) => {
  const { userId, oldPassword, newPassword } = req.body;
  if (!userId || !oldPassword || !newPassword) {
    return res.status(400).json({ message: '缺少参数' });
  }

  try {
    const [rows] = await db.query('SELECT password FROM users WHERE id = ?', [userId]);
    if (rows.length === 0) return res.status(404).json({ message: '用户不存在' });

    const isMatch = await bcrypt.compare(oldPassword, rows[0].password);
    if (!isMatch) return res.status(401).json({ message: '原密码错误' });

    const newHashedPassword = await bcrypt.hash(newPassword, 10);
    await db.query('UPDATE users SET password = ? WHERE id = ?', [newHashedPassword, userId]);
    res.status(200).json({ message: '密码修改成功' });

  } catch (error) {
    console.error('修改密码失败:', error.message);
    res.status(500).json({ message: '服务器错误' });
  }
};

// ========== 发送邮箱验证码 ========== //
exports.sendEmailCode = async (req, res) => {
  const { email } = req.body;
  if (!email) return res.status(400).json({ message: '缺少邮箱' });

  const code = Math.floor(100000 + Math.random() * 900000).toString();
  codeStore.set(email, { code, expires: Date.now() + 5 * 60 * 1000 });

  try {
    // 使用阿里云邮件推送服务
    const transporter = nodemailer.createTransport({
      host: 'smtpdm.aliyun.com',
      port: 465,
      secure: true,
      auth: {
        user: '754211575@emailsender114514.fun', // 替换为你的发信地址
        pass: 'TIANfeng221310' // 替换为阿里云邮件推送获取的 SMTP 授权码
      }
    });

    await transporter.sendMail({
      from: '"Mental Math Battle" <754211575@emailsender114514.fun> ', // 发信人
      to: email,
      subject: '邮箱验证码',
      text: `您的验证码是: ${code}，5分钟内有效。`
    });

    res.status(200).json({ message: '验证码发送成功' });
  } catch (err) {
    console.error('发送邮箱验证码失败:', err.message);
    res.status(500).json({ message: '发送失败' });
  }
};

// ========== 绑定邮箱 ========== //
exports.bindEmail = async (req, res) => {
  const { email, code, userId } = req.body;


  if (!userId) {
    console.warn('请求缺少 userId');
    return res.json({ success: false, message: '缺少用户ID' });
  }

  const record = codeStore.get(email);


  if (!record || record.code !== code || Date.now() > record.expires) {
    console.warn('验证码无效或已过期');
    return res.json({ success: false, message: '验证码无效或已过期' });
  }

  try {
    await db.query('UPDATE users SET email = ? WHERE id = ?', [email, userId]);
    codeStore.delete(email);

    res.json({ success: true });
  } catch (err) {
    console.error('绑定邮箱失败:', err.message);
    res.json({ success: false, message: '绑定失败，请稍后再试' });
  }
};


