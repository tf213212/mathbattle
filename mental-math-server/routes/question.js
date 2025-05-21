const express = require('express');
const router = express.Router();
const questionController = require('../controllers/questionController');

// 生成口算题目
router.get('/generate', questionController.generateQuestion);

// 提交答案
router.post('/submit', questionController.submitAnswer);

// 获取错题记录
router.get('/wrong-questions', questionController.getWrongQuestions);
// 删除错题记录
router.delete('/wrong-questions/:id', questionController.deleteWrongQuestion);

// 创建训练 session
router.post('/training/start', questionController.startTrainingSession);

// 结束训练 session
router.post('/training/end', questionController.endTrainingSession);

// 获取用户所有训练记录
router.get('/training/sessions', questionController.getTrainingSessions);

// 获取训练 session 的详细信息（每道题）
router.get('/training/detail', questionController.getTrainingDetail);


module.exports = router;