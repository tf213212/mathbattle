const bcrypt = require('bcrypt');
const jwt = require('jsonwebtoken');
const nodemailer = require('nodemailer');
const codeStore = new Map(); // 临时存储验证码
const db = require('../db');  // 引入数据库连接池

const JWT_SECRET = process.env.JWT_SECRET || 'your_secret_key'; // 获取 JWT 密钥，使用环境变量

// 用户注册
exports.register = async (req, res) => {
  const { username, password } = req.body;

  // 检查输入
  if (!username || !password) {
    return res.status(400).json({ message: '用户名和密码不能为空' });
  }

  try {
    // 检查用户名是否已存在
    const [existingUser] = await db.query('SELECT * FROM users WHERE username = ?', [username]);
    if (existingUser.length > 0) {
      return res.status(400).json({ message: '用户名已存在' });
    }

    // 加密密码，使用盐值为12提高安全性
    const hashedPassword = await bcrypt.hash(password, 12);

    // 将用户数据插入数据库
    await db.query('INSERT INTO users (username, password,score) VALUES (?, ?,0)', [username, hashedPassword]);

    res.status(201).json({ message: '注册成功' });
  } catch (error) {
    console.error('注册时发生错误：', error.message);  // 更详细的错误日志
    res.status(500).json({ message: '服务器错误' });
  }
};

// 用户登录
exports.login = async (req, res) => {
  const { username, password } = req.body;

  // 检查输入
  if (!username || !password) {
    return res.status(400).json({ message: '用户名和密码不能为空' });
  }

  try {
    // 查找用户
    const [user] = await db.query('SELECT * FROM users WHERE username = ?', [username]);
    if (user.length === 0) {
      return res.status(400).json({ message: '用户名或密码错误' });
    }

    // 比较密码
    const isMatch = await bcrypt.compare(password, user[0].password);
    if (!isMatch) {
      return res.status(400).json({ message: '用户名或密码错误' });
    }

    // 生成 JWT
    const token = jwt.sign({ userId: user[0].id }, JWT_SECRET, { expiresIn: '1h' });

    // 返回 token 和其他信息给前端
    res.status(200).json({
      message: '登录成功',
      token,
      score:user[0].score,
      userId: user[0].id,
      username: user[0].username,
      email: user[0].email || '未绑定',
      avatarUrl: user[0].avatar_url || null,
    });
    
  } catch (error) {
    console.error('登录时发生错误：', error.message);  // 更详细的错误日志
    res.status(500).json({ message: '服务器错误' });
  }
};
// 发送找回密码验证码
exports.sendResetCode = async (req, res) => {
  const { email } = req.body;

  if (!email) return res.status(400).json({ message: '缺少邮箱' });

  try {
    const [result] = await db.query('SELECT id FROM users WHERE email = ?', [email]);
    if (result.length === 0) {
      return res.status(400).json({ message: '该邮箱未绑定任何账号' });
    }

    const code = Math.floor(100000 + Math.random() * 900000).toString();
    codeStore.set(email, { code, expires: Date.now() + 5 * 60 * 1000 });

    // 使用 nodemailer 发送邮件
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
      subject: '密码重置验证码',
      text: `您的验证码是：${code}，5分钟内有效。`,
    });

    res.json({ success: true, message: '验证码已发送到邮箱' });
  } catch (err) {
    console.error('发送验证码失败：', err.message);
    res.status(500).json({ message: '发送失败，请稍后再试' });
  }
};

//  重置密码
exports.resetPassword = async (req, res) => {
  const { email, code, newPassword } = req.body;
  const record = codeStore.get(email);

  if (!record || record.code !== code || Date.now() > record.expires) {
    return res.status(400).json({ message: '验证码无效或已过期' });
  }

  try {
    const hashedPassword = await bcrypt.hash(newPassword, 12);
    await db.query('UPDATE users SET password = ? WHERE email = ?', [hashedPassword, email]);
    codeStore.delete(email);
    res.json({ success: true, message: '密码重置成功' });
  } catch (err) {
    console.error('重置密码失败:', err.message);
    res.status(500).json({ message: '服务器错误' });
  }
};


// 获取用户信息
exports.getUserInfo = async (req, res) => {
  const { userId } = req.params;  // 从 URL 参数获取 userId

  try {
    // 查找用户信息
    const [user] = await db.query('SELECT id, username, created_at FROM users WHERE id = ?', [userId]);

    if (user.length === 0) {
      return res.status(404).json({ message: '用户不存在' });
    }

    // 返回用户信息
    res.status(200).json({
      id: user[0].id,
      username: user[0].username,
      created_at: user[0].created_at,  // 假设数据库存储了用户的注册时间
    });
  } catch (error) {
    console.error('获取用户信息时发生错误：', error.message);
    res.status(500).json({ message: '服务器错误' });
  }
};

// 在后端代码中添加（通常放在新的文件如 controllers/leaderboard.js）
// 获取排行榜数据（独立函数版本）
exports.getLeaderboard = async (req, res) => {
  try {
    // 执行SQL查询（网页6[6](@ref)建议的字段选择方式）
    const [users] = await db.query(`
      SELECT 
        id,
        username,
        avatar_url,
        score,
        created_at 
      FROM users
    `);

    // 格式化响应数据（参考网页1[1](@ref)的响应格式）
    const responseData = users.map(user => ({
      id: user.id,
      username: user.username,
      avatar: user.avatar_url || '/default-avatar.png', // 处理空头像情况
      score: user.score,
      registeredAt: user.created_at
    }));

    res.status(200).json({
      code: 200,
      message: 'Success',
      data: responseData
    });

  } catch (error) {
    console.error('数据库查询错误:', error.message);
    res.status(500).json({
      code: 500,
      message: '服务器内部错误'
    });
  }
};