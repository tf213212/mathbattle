const bcrypt = require('bcrypt');
const jwt = require('jsonwebtoken');
const db = require('../db');  // 引入数据库连接池

const JWT_SECRET = process.env.JWT_SECRET || 'your_secret_key'; // 获取 JWT 密钥，使用环境变量





// 生成题目
exports.generateQuestion = async (req, res) => {
  const { userId, difficulty } = req.query;

  if (!userId) {
    return res.status(400).json({ message: '缺少 userId 参数' });
  }

  try {
    // 获取错题记录（保留分析逻辑）
    const [wrongQuestions] = await db.query(
      `SELECT question, correct_answer 
       FROM wrong_questions 
       WHERE user_id = ? 
       ORDER BY recorded_at DESC 
       LIMIT 10`,
      [userId]
    );

    // 生成单题逻辑
    let question;
function shouldGenerateSimilar() {
  const FIXED_PROBABILITY = 0.8; // 固定80%概率生成相似题
  return Math.random() < FIXED_PROBABILITY;
}

if (wrongQuestions.length === 0) {
  question = generateSingleDefaultQuestion(difficulty);
} else {
  if (shouldGenerateSimilar()) { // 60%概率进入相似题逻辑
    question = generateSimilarQuestion(wrongQuestions, difficulty);
  } else { // 40%概率生成普通题
    question = generateSingleDefaultQuestion(difficulty);
  }
}

    res.json(question); // 直接返回题目对象
  } catch (err) {
    console.error('题目生成失败:', err.message);
    res.status(500).json({ message: '题目生成失败' });
  }
};

// 生成单道默认题目
function generateSingleDefaultQuestion(difficulty = 'easy') {
  const operators = ['+', '-', '×', '÷'];
  let isValid = false;
  let questionObj;

  // 根据难度设置数字范围
  const ranges = {
    easy: [1, 20],
    medium: [20, 100],
    hard: [100, 999]
  };
  const [min, max] = ranges[difficulty] || ranges.easy;

  while (!isValid) {
    const num1 = Math.floor(Math.random() * (max - min + 1)) + min;
    const num2 = Math.floor(Math.random() * (max - min + 1)) + min;
    const op = operators[Math.floor(Math.random() * 4)];

    try {
      const answer = safeCalculate(num1, op, num2);
      questionObj = {
        question: `${num1} ${op} ${num2} = ___`,
        answer: formatAnswer(answer)
      };
      isValid = true;
    } catch (err) {
      // 无效重试
    }
  }
  return questionObj;
}

function generateSimilarQuestion(wrongQuestions, difficulty = 'easy') {
  const operations = analyzeOperations(wrongQuestions);
  const operatorPool = ['+', '-', '×', '÷'];
  
  // 核心修改点1：设置固定概率参数
  const MAIN_OP_PROBABILITY = 0.6; // 主错误符号概率80%
  const mainOp = operations.max || operatorPool[0]; 
  const otherOps = operatorPool.filter(op => op !== mainOp);

  // 核心修改点2：带概率的运算符选择逻辑
  const selectOperator = () => {
    if (Math.random() < MAIN_OP_PROBABILITY) {
      return mainOp; // 80%概率选择主错误运算符
    } else {
      // 20%概率均匀分配其他运算符
      const randomIndex = Math.floor(Math.random() * otherOps.length);
      return otherOps[randomIndex];
    }
  };

  // 原有逻辑增强
  const ranges = { easy: [1,20], medium: [20,100], hard: [100,999] };
  const [min, max] = ranges[difficulty] || ranges.easy;
  
  let isValid = false, questionObj;
  while (!isValid) {
    const currentOp = selectOperator(); // 概率选择运算符
    let num1, num2;
    
    // 运算符特判逻辑
    do {
      num1 = Math.floor(Math.random() * (max - min + 1)) + min;
      num2 = Math.floor(Math.random() * (max - min + 1)) + min;
      
      // 减法确保非负结果
      if (currentOp === '-') [num1, num2] = [Math.max(num1, num2), Math.min(num1, num2)];
      // 除法确保整除
      if (currentOp === '÷') num1 = num2 * (Math.floor(Math.random() * Math.floor(max/num2)) + 1);
    } while (
      (currentOp === '÷' && num2 === 0) || 
      (currentOp === '-' && num1 === num2)
    );

    try {
      const answer = safeCalculate(num1, currentOp, num2);
      questionObj = {
        question: `${num1} ${currentOp} ${num2} = ___`,
        answer: formatAnswer(answer),
        meta: { operator: currentOp, isMainOp: currentOp === mainOp } // 扩展元数据
      };
      isValid = true;
    } catch (err) {
      console.warn(`Invalid operation: ${num1}${currentOp}${num2}`, err);
    }
  }
  return questionObj;
}

// 安全计算函数（确保除法能整除）
function safeCalculate(num1, operator, num2) {
  const ops = {
    '+': (a, b) => a + b,
    '-': (a, b) => a - b,
    '×': (a, b) => a * b,
    '÷': (a, b) => {
      if (b === 0) throw new Error('除数不能为零');
      if (a % b !== 0) throw new Error('除法不能整除');
      return a / b;
    }
  };

  if (!ops[operator]) throw new Error('无效运算符');
  return ops[operator](num1, num2);
}

// 修改运算符分析逻辑
function analyzeOperations(questions) {
  const opMap = {
    '+': '加法',
    '-': '减法',
    '×': '乘法',
    '÷': '除法'
  };

  const opCount = { '+': 0, '-': 0, '×': 0, '÷': 0 };

  questions.forEach(({ question }) => {
    const found = Object.keys(opMap).find(op => question.includes(op));
    if (found) opCount[found]++;
  });

  return {
    max: Object.entries(opCount).reduce((a, b) => a[1] > b[1] ? a : b)[0]
  };
}

// 答案格式化
function formatAnswer(answer) {
  return Number.isInteger(answer) ? 
         answer.toString() : 
         answer.toFixed(2);
}



// 提交答案
exports.submitAnswer = async (req, res) => {
  const { userId, question, userAnswer, correctAnswer, sessionId } = req.body;

  if (
    userId == null || sessionId == null ||
    question == null || userAnswer == null || correctAnswer == null
  ) {
    return res.status(400).json({ message: '缺少必要参数' });
  }

  const isCorrect = parseInt(userAnswer) === parseInt(correctAnswer);

  try {
    // 打印调试信息，确认查询参数正确
    console.log('Executing query with values:', [sessionId, question, userAnswer, correctAnswer, isCorrect]);

    // 写入练习记录详情
    const result = await db.query(
      `INSERT INTO training_record_detail (session_id, question, user_answer, correct_answer, is_correct)
       VALUES (?, ?, ?, ?, ?)`,
      [sessionId, question, userAnswer, correctAnswer, isCorrect]
    );
    

    // 更新 session 的题目总数和正确数
    const updateResult = await db.query(
      `UPDATE training_session
       SET question_count = question_count + 1,
           correct_count = correct_count + ?,
           accuracy = ROUND((correct_count + ?)/(question_count + 1)*100, 2)
       WHERE id = ?`,
      [isCorrect ? 1 : 0, isCorrect ? 1 : 0, sessionId]
    );

    // 错题存档
    if (!isCorrect) {
      await db.query(
        `INSERT INTO wrong_questions (user_id, question, user_answer, correct_answer, recorded_at)
         VALUES (?, ?, ?, ?, NOW())`,
        [userId, question, userAnswer, correctAnswer]
      );
    }

    // 返回正确的响应
    res.json({ correct: isCorrect });
  } catch (err) {
    // 捕获并打印错误
    console.error('提交答案失败:', err.message);
    res.status(500).json({ message: '提交失败' });
  }
};



// 获取用户错题
exports.getWrongQuestions = async (req, res) => {
  const { userId } = req.query;

  if (!userId) return res.status(400).json({ message: '缺少 userId' });

  try {
    const [rows] = await db.query('SELECT * FROM wrong_questions WHERE user_id = ?', [userId]);
    res.json(rows);
  } catch (err) {
    console.error('查询错题失败：', err.message);
    res.status(500).json({ message: '服务器错误' });
  }
};
// 根据错题ID删除记录
exports.deleteWrongQuestion = async (req, res) => {
  const { id } = req.params; // 从路由参数获取要删除的错题ID

  if (!id) return res.status(400).json({ message: '缺少错题ID参数' });

  try {
    // 执行删除操作（网页5的SQL操作最佳实践）
    const [result] = await db.query(
      'DELETE FROM wrong_questions WHERE id = ?', 
      [id]
    );

    // 处理删除结果（网页3的数据验证逻辑）
    if (result.affectedRows === 0) {
      return res.status(404).json({ 
        success: false,
        message: '未找到对应错题记录'
      });
    }

    res.json({ 
      success: true,
      message: '错题删除成功',
      deletedId: Number(id)
    });
  } catch (err) {
    console.error('错题删除失败:', err.message);
    res.status(500).json({
      success: false,
      message: '服务器内部错误'
    });
  }
};

// 创建训练 session
exports.startTrainingSession = async (req, res) => {
  const { userId } = req.body;

  if (!userId) {
    return res.status(400).json({ message: '缺少 userId' });
  }

  try {
    const [result] = await db.query(
      `INSERT INTO training_session (user_id, question_count, correct_count, accuracy, start_time)
       VALUES (?, 0, 0, 0.00, NOW())`,
      [userId]
    );

    res.json({ sessionId: result.insertId });
  } catch (err) {
    console.error('创建训练 session 失败:', err.message);
    res.status(500).json({ message: '创建训练失败' });
  }
};

exports.endTrainingSession = async (req, res) => {
  const { sessionId } = req.body;

  if (!sessionId) return res.status(400).json({ message: '缺少 sessionId' });

  try {
    await db.query(
      `UPDATE training_session SET end_time = NOW() WHERE id = ?`,
      [sessionId]
    );
    res.json({ message: '训练结束记录成功' });
  } catch (err) {
    console.error('结束训练失败：', err.message);
    res.status(500).json({ message: '结束训练失败' });
  }
};

// 获取用户所有训练记录（训练概览）
exports.getTrainingSessions = async (req, res) => {
  const { userId } = req.query;

  if (!userId) {
    return res.status(400).json({ message: '缺少 userId 参数' });
  }

  try {
    const [rows] = await db.query(
      `SELECT id, question_count, correct_count, accuracy, start_time, end_time 
       FROM training_session 
       WHERE user_id = ? 
       ORDER BY start_time DESC`,
      [userId]
    );
    res.json(rows);
  } catch (err) {
    console.error('获取训练记录失败:', err.message);
    res.status(500).json({ message: '服务器错误' });
  }
};
// 获取单个训练记录的详细信息（每道题）
exports.getTrainingDetail = async (req, res) => {
  const { sessionId } = req.query;

  if (!sessionId) {
    return res.status(400).json({ message: '缺少 sessionId 参数' });
  }

  try {
    const [details] = await db.query(
      `SELECT question, user_answer, correct_answer, is_correct 
       FROM training_record_detail 
       WHERE session_id = ?`,
      [sessionId]
    );
    res.json(details);
  } catch (err) {
    console.error('获取训练详情失败:', err.message);
    res.status(500).json({ message: '服务器错误' });
  }
};
