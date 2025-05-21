const express = require('express');
const router = express.Router();
const authController = require('../controllers/authController');

router.post('/register', authController.register);
router.post('/login', authController.login);
router.post('/send-reset-code', authController.sendResetCode);
router.post('/reset-password', authController.resetPassword);
router.get('/user/:userId', authController.getUserInfo);
router.get('/leaderboard', authController.getLeaderboard);

module.exports = router;
