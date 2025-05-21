const express = require('express');
const router = express.Router();
const userController = require('../controllers/userController');

router.post('/upload-avatar', userController.uploadAvatar);
router.post('/change-password', userController.changePassword);
router.post('/send-email-code', userController.sendEmailCode);
router.post('/bind-email', userController.bindEmail);
module.exports = router;
