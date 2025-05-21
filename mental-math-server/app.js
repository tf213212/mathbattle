const express = require('express');
const http = require('http');
const authRoutes = require('./routes/auth');
const questionRoutes = require('./routes/question');
const userRoutes = require('./routes/user');

const setupWebSocket = require('./services/websocket');

const app = express();
const server = http.createServer(app);


app.use(express.json());
app.use('/api', authRoutes);
app.use('/api', questionRoutes);
app.use('/api/user', userRoutes);
app.use('/uploads', express.static('uploads'));

setupWebSocket(server); // 启动 WebSocket 服务

const PORT = 8080;
server.listen(PORT, () => {
  console.log(`服务器已启动：http://localhost:${PORT}`);
});


const crypto = require('crypto');

// 生成64字节（512位）的随机密钥，并将其转换为16进制字符串
const JWT_SECRET = crypto.randomBytes(64).toString('hex');

// 打印生成的密钥
console.log('Generated JWT Secret:', JWT_SECRET);