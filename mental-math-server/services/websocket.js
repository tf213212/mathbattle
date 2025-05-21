let waitingPlayers = []; // 用于管理多个等待的玩家
let playerPairs = new Map(); // 存储玩家和对手的映射
let currentQuestions = new Map(); // 存储每个玩家当前题目信息
let playerSessions = new Map(); // 存储玩家会话信息

function setupWebSocket(server) {
  const WebSocket = require('ws');
  const wss = new WebSocket.Server({ server });

  wss.on('connection', (ws) => {
    console.log('新的WebSocket连接建立');

    let sessionId = Date.now().toString(); // 为每个连接创建唯一会话ID

    // 存储会话信息
    playerSessions.set(ws, {
      id: sessionId,
      userId: null,
      connectedAt: new Date(),
      lastActivity: new Date(),
      isCompleted: false // 新增：跟踪玩家是否已完成答题
    });

    ws.on('message', (data) => {
      try {
        const msg = JSON.parse(data);
        console.log(`收到消息 [会话 ${sessionId}]:`, msg);

        // 更新最后活动时间
        playerSessions.get(ws).lastActivity = new Date();

        if (msg.type === 'match') {
          handleMatch(ws, msg);
        } else if (msg.type === 'answer') {
          handleAnswer(ws, msg);
        } else if (msg.type === 'statusUpdate') {
          handleStatusUpdate(ws, msg);
        } else if (msg.type === 'battleCompleted') { 
          handleBattleCompleted(ws, msg);
        } else {
          console.warn('未知的消息类型:', msg.type);
          ws.send(JSON.stringify({
            type: 'error',
            message: '未知的消息类型',
            receivedType: msg.type
          }));
        }
      } catch (e) {
        console.error('消息解析错误:', e.message);
        ws.send(JSON.stringify({
          type: 'error',
          message: '无效的JSON格式',
          details: e.message
        }));
      }
    });

    ws.on('close', () => {
      console.log(`连接关闭 [会话 ${sessionId}]`);
      cleanupPlayer(ws);
    });

    ws.on('error', (err) => {
      console.error(`WebSocket错误 [会话 ${sessionId}]:`, err);
      cleanupPlayer(ws);
    });
  });

  console.log('WebSocket 服务已启动');
}

// 新增：处理 battleCompleted 消息
function handleBattleCompleted(ws, msg) {
    const session = playerSessions.get(ws);
    const opponent = getOpponent(ws);

    if (!opponent) {
        console.warn('未找到对手');
        return safeSend(ws, {
            type: 'error',
            message: '未找到对手，无法完成对战'
        });
    }

    // 更新本方的正确数和答题数
    session.correctCount = msg.correctCount;
    session.totalQuestions = msg.totalQuestions;
    session.timeSpent = msg.timeSpent;

    session.isCompleted = true;

    const opponentSession = playerSessions.get(opponent);
    if (opponentSession.isCompleted) {
        // 对手也已完成，可以结束对战
        handleBattleEnd(ws, msg);
    } else {
        // 对手未完成，等待对手完成
        safeSend(ws, {
            type: 'waitingForOpponent',
            message: '等待对手完成对战...'
        });
    }
}

function handleMatch(ws, msg) {
  const session = playerSessions.get(ws);
  session.userId = msg.userId;
  currentUserId = msg.userId;

  console.log(`玩家 ${msg.userId} 请求匹配 [会话 ${session.id}]`);

  if (waitingPlayers.length > 0) {
    const opponent = waitingPlayers.pop();
    const opponentSession = playerSessions.get(opponent);

    console.log(`匹配成功: ${msg.userId} vs ${opponentSession.userId}`);

    const startMessage = {
      type: 'start',
      message: '匹配成功，游戏开始！',
      opponentId: opponentSession.userId,
      yourId: msg.userId
    };

    playerPairs.set(ws, opponent);
    playerPairs.set(opponent, ws);

    // 发送匹配成功消息
    safeSend(opponent, startMessage);
    safeSend(ws, startMessage);

    // 生成并发送相同题目
    const currentQuestion = generateQuestion();
    sendNewQuestion(ws, currentQuestion);
    sendNewQuestion(opponent, currentQuestion);
  } else {
    waitingPlayers.push(ws);
    console.log(`玩家 ${msg.userId} 进入等待队列`);

    safeSend(ws, {
      type: 'waiting',
      message: '正在等待对手...',
      queuePosition: waitingPlayers.length
    });

    // 匹配超时逻辑
    const timeoutId = setTimeout(() => {
      if (waitingPlayers.includes(ws)) {
        console.log(`玩家 ${msg.userId} 匹配超时`);
        safeSend(ws, {
          type: 'timeout',
          message: '匹配超时，请稍后再试'
        });
        waitingPlayers = waitingPlayers.filter(p => p !== ws);
      }
    }, 30000);

    // 存储超时ID以便清理
    session.matchTimeout = timeoutId;
  }
}

function handleBattleEnd(ws, originalMsg) {
    console.log(`对战结束处理`);
    const session = playerSessions.get(ws);
    const opponent = getOpponent(ws);

    if (!opponent) {
        console.warn('未找到对手');
        return safeSend(ws, {
            type: 'error',
            message: '未找到对手，无法结束对战'
        });
    }

    const opponentSession = playerSessions.get(opponent);

    // 使用双方 session 中的数据
    const yourCorrectCount = session.correctCount || 0;
    const yourTotalQuestions = session.totalQuestions || 0;
    const yourTimeSpent = session.timeSpent || 0;

    const opponentCorrectCount = opponentSession.correctCount || 0;
    const opponentTotalQuestions = opponentSession.totalQuestions || 0;
    const opponentTimeSpent = opponentSession.timeSpent || 0;

    // 计算双方的正确率
    const yourAccuracy = yourTotalQuestions > 0 ? yourCorrectCount / yourTotalQuestions : 0;
    const opponentAccuracy = opponentTotalQuestions > 0 ? opponentCorrectCount / opponentTotalQuestions : 0;

    // 计算双方得分
    const yourScore = calculateScore(yourAccuracy, yourTimeSpent);
    const opponentScore = calculateScore(opponentAccuracy, opponentTimeSpent);

    // 判断胜负
    let yourResult, opponentResult;
    if (yourScore > opponentScore) {
        yourResult = 'win';
        opponentResult = 'lose';
    } else if (yourScore < opponentScore) {
        yourResult = 'lose';
        opponentResult = 'win';
    } else {
        // 得分相同，时间短者胜
        if (yourTimeSpent < opponentTimeSpent) {
            yourResult = 'win';
            opponentResult = 'lose';
        } else if (yourTimeSpent > opponentTimeSpent) {
            yourResult = 'lose';
            opponentResult = 'win';
        } else {
            yourResult = 'draw';
            opponentResult = 'draw';
        }
    }

    // 发送结果
    safeSend(ws, {
        type: 'endBattle',
        result: yourResult,
        correctCount: yourCorrectCount,
        totalQuestions: yourTotalQuestions,
        timeSpent: yourTimeSpent,
        score: yourScore
    });

    safeSend(opponent, {
        type: 'endBattle',
        result: opponentResult,
        correctCount: opponentCorrectCount,
        totalQuestions: opponentTotalQuestions,
        timeSpent: opponentTimeSpent,
        score: opponentScore
    });

    // 保存结果
    saveBattleResultToDB(
        session.userId,
        opponentSession.userId,
        yourResult,
        yourCorrectCount,
        yourTotalQuestions,
        yourTimeSpent,
        session.battleSettings?.difficulty || 'easy'
    );
     // 新增：更新用户积分
  updateScoresWithTransaction(session.userId, yourCorrectCount,opponentSession.userId, opponentCorrectCount);


    // 清理对局状态数据
    cleanupAfterBattle(ws);
    cleanupAfterBattle(opponent);
}
async function updateScoresWithTransaction(userId1, score1, userId2, score2) {
  const connection = await db.getConnection();
  try {
    await connection.beginTransaction();
    
    await connection.query(
      'UPDATE users SET score = score + ? WHERE id = ?',
      [score1, userId1]
    );
    
    await connection.query(
      'UPDATE users SET score = score + ? WHERE id = ?',
      [score2, userId2]
    );

    await connection.commit();
    console.log('积分更新事务成功');
  } catch (err) {
    await connection.rollback();
    console.error('积分更新事务失败:', err);
  } finally {
    connection.release();
  }
}
function cleanupAfterBattle(ws) {
  playerPairs.delete(ws);
  currentQuestions.delete(ws);

  const session = playerSessions.get(ws);
  if (session) {
    session.totalQuestions = 0;
    session.correctCount = 0;
    session.timeSpent = 0;
    session.isCompleted = false; // 重置完成状态
  }
}

function calculateScore(accuracy, timeSpent) {
  // 权重分配：正确率占90%，答题速度占10%
  const accuracyWeight = 0.9;
  const timeWeight = 0.1;

  // 正确率分数（0-100）
  const accuracyScore = accuracy * 100;

  // 时间分数（0-100），时间越短分数越高
  // 假设最大时间为30秒，转换为0-100的分数
  const maxTime = 30;
  const timeScore = timeSpent > 0 ? (1 - Math.min(timeSpent, maxTime) / maxTime) * 100 : 100;

  // 综合得分
  const totalScore = accuracyScore * accuracyWeight + timeScore * timeWeight;
  return totalScore;
}

const db = require('../db');
function saveBattleResultToDB(userId, opponentId, result, correctCount, totalQuestions, timeSpent, difficulty) {
  const sql = `
      INSERT INTO battle_records (
          user_id, 
          opponent_id, 
          result, 
          correct_count, 
          total_questions, 
          time_spent, 
          difficulty
      ) VALUES (?, ?, ?, ?, ?, ?, ?)
  `;
  const values = [userId, opponentId, result, correctCount, totalQuestions, timeSpent, difficulty];

  db.query(sql, values, (error, results, fields) => {
      if (error) {
          console.error('保存对战结果时发生错误:', error);
          return;
      }
      console.log('对战结果已保存:', results.insertId);
  });
}

function handleStatusUpdate(ws, msg) {
    const session = playerSessions.get(ws);
    const opponent = getOpponent(ws);

    if (!opponent) {
        console.warn('未找到对手');
        return safeSend(ws, {
            type: 'error',
            message: '未找到对手，无法更新状态'
        });
    }

    // 向对手发送状态更新
    safeSend(opponent, {
        type: 'statusUpdate',
        correctCount: msg.correctCount,
        totalQuestions: msg.totalQuestions
    });
}

function handleAnswer(ws, msg) {
  const session = playerSessions.get(ws);
  console.log(`处理答案 [玩家 ${session.userId}]:`, msg);

  const current = currentQuestions.get(ws);
  if (!current) {
    console.warn('没有找到当前题目');
    return safeSend(ws, {
      type: 'error',
      message: '未找到当前题目',
      yourQuestion: msg.question
    });
  }

  // 验证题目是否匹配
  if (msg.question !== current.question) {
    console.warn('题目不匹配', {
      received: msg.question,
      expected: current.question
    });
    return safeSend(ws, {
      type: 'error',
      message: '题目不匹配',
      yourQuestion: msg.question,
      currentQuestion: current.question
    });
  }

  // 验证答案
  const userAnswer = parseInt(msg.answer);
  if (isNaN(userAnswer)) {
    console.warn('无效的答案格式', msg.answer);
    return safeSend(ws, {
      type: 'error',
      message: '答案必须是数字',
      received: msg.answer
    });
  }

  // 更新答题数
  session.totalQuestions = (session.totalQuestions || 0) + 1;

  // 判断是否答对，更新正确数
  const isCorrect = userAnswer === current.correctAnswer;
  if (isCorrect) {
    session.correctCount = (session.correctCount || 0) + 1;
  }

  const resultMessage = {
    type: 'result',
    isCorrect: isCorrect,
    correctAnswer: current.correctAnswer,
    yourAnswer: userAnswer,
    question: current.question,
    message: isCorrect ? '回答正确!' : '回答错误'
  };

  safeSend(ws, resultMessage);

  // 发送新题目
  const newQuestion = generateQuestion();
  sendNewQuestion(ws, newQuestion);
}

function getOpponent(player) {
  return playerPairs.get(player);
}

function sendNewQuestion(player, questionData) {
  currentQuestions.set(player, questionData);
  safeSend(player, {
    type: 'newQuestion',
    question: questionData.question,
    correctAnswer: questionData.correctAnswer // 明确包含正确答案
  });
}

function generateQuestion() {
  const operators = ['+', '-', '×', '÷']; // 修改运算符为手写符号
  const operator = operators[Math.floor(Math.random() * operators.length)];
  const num1 = Math.floor(Math.random() * 10) + 1;
  const num2 = Math.floor(Math.random() * 10) + 1;

  let question = '';
  let correctAnswer = 0;

  switch (operator) {
    case '+':
      question = `${num1} + ${num2}`;
      correctAnswer = num1 + num2;
      break;
    case '-':
      question = `${num1} - ${num2}`;
      correctAnswer = num1 - num2;
      break;
    case '×': // 手写乘号用"×"而非"*"
      question = `${num1} × ${num2}`;
      correctAnswer = num1 * num2;
      break;
    case '÷': // 手写除号用"÷"而非"/"
      // 确保除法题目能整除
      question = `${num1 * num2} ÷ ${num2}`;
      correctAnswer = num1;
      break;
  }

  return { question, correctAnswer };
}

// 安全发送消息，避免连接已关闭时报错
function safeSend(ws, data) {
  if (ws && ws.readyState === ws.OPEN) {
    try {
      ws.send(JSON.stringify(data));
    } catch (e) {
      console.error('发送消息失败:', e.message);
    }
  } else {
    console.warn('尝试向已关闭的连接发送消息');
  }
}

// 清理玩家数据
function cleanupPlayer(ws) {
  const session = playerSessions.get(ws);
  if (session) {
    console.log(`清理玩家数据 [会话 ${session.id}, 用户 ${session.userId}]`);
    
    // 清除匹配超时定时器
    if (session.matchTimeout) {
      clearTimeout(session.matchTimeout);
    }
    
    // 通知对手玩家断开连接
    const opponent = getOpponent(ws);
    if (opponent) {
      safeSend(opponent, {
        type: 'opponentDisconnected',
        message: '你的对手已断开连接'
      });
      playerPairs.delete(opponent);
    }
    
    // 从各种集合中移除
    waitingPlayers = waitingPlayers.filter(p => p !== ws);
    playerPairs.delete(ws);
    currentQuestions.delete(ws);
    playerSessions.delete(ws);
  }
}

module.exports = setupWebSocket;