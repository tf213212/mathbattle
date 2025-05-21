const mysql = require('mysql2');

// 创建连接池，保持连接池的复用，避免每次请求都新建连接
const pool = mysql.createPool({
  host: 'localhost', // 数据库主机
  user: 'root',      // 数据库用户名
  password: '123456',  // 数据库密码
  database: 'mental_math',   // 数据库名称
  waitForConnections: true,  // 等待连接
  connectionLimit: 10,       // 最大连接数
  queueLimit: 0
});

// 导出连接池，供其他文件使用
module.exports = pool.promise();

