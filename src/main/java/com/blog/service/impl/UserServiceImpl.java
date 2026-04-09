package com.blog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.blog.common.Result;
import com.blog.common.PageResult;
import com.blog.common.ResultCode;
import com.blog.dto.UserDTO;
import com.blog.dto.UserLoginDTO;
import com.blog.dto.UserRegisterDTO;
import com.blog.dto.UserUpdateDTO;
import com.blog.dto.ChangePasswordDTO;
import com.blog.dto.SendResetCodeDTO;
import com.blog.dto.SendRegisterCodeDTO;
import com.blog.dto.ResetPasswordByCodeDTO;
import com.blog.dto.TokenRefreshResponseDTO;
import com.blog.entity.User;
import com.blog.entity.UserFollow;
import com.blog.exception.BusinessException;
import com.blog.mapper.UserMapper;
import com.blog.mapper.UserFollowMapper;
import com.blog.service.CaptchaService;
import com.blog.service.UserService;
import com.blog.utils.JWTUtils;
import com.blog.utils.PasswordPolicyUtils;
import com.blog.utils.RedisUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import jakarta.mail.internet.MimeMessage;

import com.blog.utils.RedisDistributedLock;
import com.blog.service.EmailTemplateService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionSynchronization;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import java.util.concurrent.Executor;
import java.util.concurrent.CompletableFuture;

/**
 * 用户服务实现类
 */
@Service
@Slf4j
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JWTUtils jwtUtils;

    @Autowired
    private HttpServletRequest request;

    @Autowired
    private CaptchaService captchaService;

    @Autowired
    private com.blog.service.NotificationService notificationService;

    @Autowired
    private UserFollowMapper userFollowMapper;

    @Autowired
    private RedisDistributedLock redisDistributedLock;

    @Autowired
    private RedisUtils redisUtils;

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private EmailTemplateService emailTemplateService;

    @org.springframework.beans.factory.annotation.Value("${spring.mail.from}")
    private String mailFrom;

    @org.springframework.beans.factory.annotation.Value("${github.oauth.client-id}")
    private String githubClientId;

    @org.springframework.beans.factory.annotation.Value("${github.oauth.client-secret}")
    private String githubClientSecret;

    @org.springframework.beans.factory.annotation.Value("${github.oauth.redirect-uri}")
    private String githubRedirectUri;

    @org.springframework.beans.factory.annotation.Value("${github.oauth.scope}")
    private String githubScope;

    @Autowired
    private org.springframework.web.client.RestTemplate restTemplate;

    @Autowired
    @org.springframework.beans.factory.annotation.Qualifier("notificationTaskExecutor")
    private Executor notificationTaskExecutor;

    private static final String PASSWORD_RESET_CODE_KEY_PREFIX = "password:reset:code:";
    private static final long PASSWORD_RESET_CODE_EXPIRE_MINUTES = 10;
    private static final long PASSWORD_RESET_CODE_SEND_INTERVAL_SECONDS = 60;
    private static final String REFRESH_TOKEN_KEY_PREFIX = "auth:refresh:user:";
    private static final String ACCESS_TOKEN_BLACKLIST_KEY_PREFIX = "auth:blacklist:access:";
    
    // 注册邮箱验证码相关
    private static final String REGISTER_CODE_KEY_PREFIX = "register:code:";
    private static final String REGISTER_CODE_LIMIT_KEY_PREFIX = "register:code:limit:";
    private static final long REGISTER_CODE_EXPIRE_MINUTES = 10;
    private static final long REGISTER_CODE_LIMIT_SECONDS = 60;

    @Override
    @Transactional
    public Result<String> register(UserRegisterDTO registerDTO) {
        log.info("用户注册：username={}", registerDTO.getUsername());

        // 验证邮箱验证码
        String email = registerDTO.getEmail();
        String emailCodeKey = REGISTER_CODE_KEY_PREFIX + email;
        String cachedEmailCode = redisUtils.get(emailCodeKey);
        if (!StringUtils.hasText(cachedEmailCode) || !cachedEmailCode.equals(registerDTO.getEmailCode())) {
            throw new BusinessException(ResultCode.ERROR, "邮箱验证码错误或已过期");
        }

        // 验证密码是否一致
        if (!registerDTO.getPassword().equals(registerDTO.getConfirmPassword())) {
            throw new BusinessException(ResultCode.PASSWORD_NOT_MATCH);
        }

        // 验证密码强度
        if (!PasswordPolicyUtils.validatePassword(registerDTO.getPassword())) {
            throw new BusinessException(ResultCode.ERROR, PasswordPolicyUtils.getPasswordPolicy());
        }

        // 检查用户名是否已存在
        if (userMapper.selectByUsername(registerDTO.getUsername()) != null) {
            throw new BusinessException(ResultCode.USERNAME_EXIST);
        }

        // 检查邮箱是否已存在
        if (StringUtils.hasText(registerDTO.getEmail()) &&
                userMapper.selectByEmail(registerDTO.getEmail()) != null) {
            throw new BusinessException(ResultCode.EMAIL_EXIST);
        }

        // 创建用户实体
        User user = new User();
        user.setUsername(registerDTO.getUsername());
        user.setPassword(passwordEncoder.encode(registerDTO.getPassword()));
        user.setEmail(registerDTO.getEmail());
        user.setNickname(
                StringUtils.hasText(registerDTO.getNickname()) ? registerDTO.getNickname() : registerDTO.getUsername());
        user.setPhone(registerDTO.getPhone());
        user.setAvatar(registerDTO.getAvatar());
        user.setBio(registerDTO.getBio());
        user.setPosition(registerDTO.getPosition());
        user.setCompany(registerDTO.getCompany());
        user.setStatus(1); // 邮箱已验证，直接激活
        user.setRole(1); // 普通用户角色
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());

        // 保存用户
        int result = userMapper.insert(user);
        if (result <= 0) {
            throw new BusinessException(ResultCode.ERROR, "用户注册失败");
        }

        // 删除已使用的邮箱验证码
        redisUtils.delete(emailCodeKey);
        redisUtils.delete(REGISTER_CODE_LIMIT_KEY_PREFIX + email);

        // 事务提交后再发送欢迎邮件，避免回滚场景下误发送
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            String welcomeNickname = user.getNickname();
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    submitWelcomeEmailTask(email, welcomeNickname);
                }
            });
        } else {
            submitWelcomeEmailTask(email, user.getNickname());
        }

        log.info("用户注册成功：username={}", registerDTO.getUsername());
        return Result.success("注册成功");
    }

    @Override
    public Result<UserDTO> login(UserLoginDTO loginDTO) {
        log.info("用户登录：username={}", loginDTO.getUsername());

        // 验证验证码（可选：仅对频繁登录失败的用户强制验证）
        if (!captchaService.verifyCaptcha(loginDTO.getCaptchaKey(), loginDTO.getCaptcha())) {
            // 这里可以决定是否强制验证验证码，为简单起见，我们验证但不强制
            // 在实际应用中，可以根据登录失败次数决定是否要求验证码
            log.warn("登录验证码验证失败，用户名：{}", loginDTO.getUsername());
        }

        // 根据用户名/邮箱/手机号查询用户
        User user = userMapper.selectByUsername(loginDTO.getUsername());
        if (user == null) {
            user = userMapper.selectByEmail(loginDTO.getUsername());
        }
        if (user == null && loginDTO.getUsername().matches("^1[3-9]\\d{9}$")) {
            // 如果是手机号格式，尝试按手机号查询
            LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(User::getPhone, loginDTO.getUsername());
            user = userMapper.selectOne(wrapper);
        }

        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }

        // 检查用户状态
        if (user.getStatus() == 0) {
            throw new BusinessException(ResultCode.ERROR, "账号未激活，请先验证邮箱");
        }
        if (user.getStatus() != 1) {
            throw new BusinessException(ResultCode.USER_DISABLED);
        }

        // 验证密码
        if (!passwordEncoder.matches(loginDTO.getPassword(), user.getPassword())) {
            throw new BusinessException(ResultCode.PASSWORD_ERROR);
        }

        String accessToken = jwtUtils.generateAccessToken(user.getId(), user.getUsername());
        String refreshToken = jwtUtils.generateRefreshToken(user.getId(), user.getUsername());
        storeRefreshToken(user.getId(), refreshToken);

        // 更新最后登录信息
        user.setLastLoginTime(LocalDateTime.now());
        user.setLastLoginIp(getClientIp());
        userMapper.updateById(user);

        // 构建用户信息 DTO
        UserDTO userDTO = convertToDTO(user);
        userDTO.setAccessToken(accessToken);
        userDTO.setRefreshToken(refreshToken);

        log.info("用户登录成功：username={}, userId={}", user.getUsername(), user.getId());
        return Result.success(userDTO);
    }

    @Override
    public Result<Void> logout(Long userId) {
        return logout(userId, null);
    }

    @Override
    public Result<Void> logout(Long userId, String refreshToken) {
        log.info("用户登出：userId={}", userId);
        deleteRefreshToken(userId);
        return Result.success();
    }

    private void submitWelcomeEmailTask(String email, String nickname) {
        try {
            CompletableFuture.runAsync(() -> sendWelcomeEmail(email, nickname), notificationTaskExecutor);
        } catch (Exception e) {
            log.error("提交欢迎邮件异步任务失败，降级为同步发送：email={}", email, e);
            sendWelcomeEmail(email, nickname);
        }
    }

    private void sendWelcomeEmail(String email, String nickname) {
        try {
            String emailHtml = emailTemplateService.getWelcomeEmailHtml(nickname);
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            helper.setFrom(mailFrom);
            helper.setTo(email);
            helper.setSubject("欢迎加入 Lumina");
            helper.setText(emailHtml, true);
            mailSender.send(mimeMessage);
            log.info("发送欢迎邮件成功：email={}", email);
        } catch (Exception e) {
            log.error("发送欢迎邮件失败：email={}", email, e);
        }
    }

    @Override
    public Result<TokenRefreshResponseDTO> refreshToken(String refreshToken) {
        log.info("刷新 JWT 令牌");

        if (!StringUtils.hasText(refreshToken)) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "刷新令牌不能为空");
        }

        if (!jwtUtils.validateRefreshToken(refreshToken)) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "刷新令牌无效");
        }

        if (!jwtUtils.isRefreshToken(refreshToken)) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "令牌类型错误");
        }

        if (jwtUtils.isRefreshTokenExpired(refreshToken)) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "刷新令牌已过期");
        }

        Long userId = jwtUtils.getUserIdFromRefreshToken(refreshToken);
        String username = jwtUtils.getUsernameFromRefreshToken(refreshToken);

        if (!isRefreshTokenValid(userId, refreshToken)) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "刷新令牌已失效，请重新登录");
        }

        User user = userMapper.selectById(userId);
        if (user == null || user.getStatus() == 0) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "用户不存在或已被禁用");
        }

        String newAccessToken = jwtUtils.generateAccessToken(userId, username);
        String newRefreshToken = jwtUtils.generateRefreshToken(userId, username);
        storeRefreshToken(userId, newRefreshToken);

        return Result.success(new TokenRefreshResponseDTO(newAccessToken, newRefreshToken));
    }

    @Override
    public Result<TokenRefreshResponseDTO> refreshTokenCompatible(String refreshToken, String authorizationHeader) {
        // 生产闭环：仅接受 refreshToken，避免 access token 作为刷新凭据
        if (StringUtils.hasText(refreshToken)) {
            return refreshToken(refreshToken);
        }
        throw new BusinessException(ResultCode.UNAUTHORIZED, "缺少刷新令牌");
    }

    @Override
    public Result<Boolean> validateToken(String authorizationHeader) {
        if (!StringUtils.hasText(authorizationHeader) || !authorizationHeader.startsWith("Bearer ")) {
            return Result.success(false);
        }

        String accessToken = authorizationHeader.substring(7);
        if (!jwtUtils.validateToken(accessToken) || jwtUtils.isTokenExpired(accessToken) || !jwtUtils.isAccessToken(accessToken)) {
            return Result.success(false);
        }

        try {
            Long userId = jwtUtils.getUserIdFromToken(accessToken);
            User user = userMapper.selectById(userId);
            boolean valid = user != null && user.getStatus() != 0;
            return Result.success(valid);
        } catch (Exception e) {
            log.warn("校验访问令牌失败：{}", e.getMessage());
            return Result.success(false);
        }
    }

    private void storeRefreshToken(Long userId, String refreshToken) {
        long ttl = jwtUtils.getRemainingRefreshTime(refreshToken);
        redisUtils.set(REFRESH_TOKEN_KEY_PREFIX + userId, refreshToken, ttl, TimeUnit.SECONDS);
    }

    private boolean isRefreshTokenValid(Long userId, String refreshToken) {
        String stored = redisUtils.get(REFRESH_TOKEN_KEY_PREFIX + userId);
        return refreshToken.equals(stored);
    }

    private void deleteRefreshToken(Long userId) {
        redisUtils.delete(REFRESH_TOKEN_KEY_PREFIX + userId);
    }

    @Override
    public Result<UserDTO> getUserInfo(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }

        UserDTO userDTO = convertToDTO(user);
        return Result.success(userDTO);
    }

    @Override
    @Transactional
    public Result<Void> updateUserInfo(Long userId, UserUpdateDTO updateDTO) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }

        // 检查邮箱是否已被其他用户使用
        // 注意：只有当新邮箱不为 null 且与原邮箱不同时才检查
        if (updateDTO.getEmail() != null &&
                !updateDTO.getEmail().equals(user.getEmail())) {
            // 如果新邮箱不是空字符串，才检查是否已被使用
            if (StringUtils.hasText(updateDTO.getEmail())) {
                User existingUser = userMapper.selectByEmail(updateDTO.getEmail());
                if (existingUser != null) {
                    throw new BusinessException(ResultCode.EMAIL_EXIST);
                }
            }
        }

        // 更新用户信息
        // 修改逻辑：字段存在即更新，null 值表示清空该字段
        // 使用 StringUtils.hasText() 会在空字符串时跳过更新，导致无法清空字段
        // 现在改为：字段不为 null 就更新，允许设置为 null 以清空字段

        if (updateDTO.getNickname() != null) {
            // 如果是空字符串，设置为 null，否则使用 trim 后的值
            user.setNickname(StringUtils.hasText(updateDTO.getNickname()) ?
                updateDTO.getNickname().trim() : null);
        }

        if (updateDTO.getEmail() != null) {
            // 邮箱不允许为空字符串，如果为空则不更新
            if (StringUtils.hasText(updateDTO.getEmail())) {
                user.setEmail(updateDTO.getEmail().trim());
            }
        }

        if (updateDTO.getPhone() != null) {
            user.setPhone(StringUtils.hasText(updateDTO.getPhone()) ?
                updateDTO.getPhone().trim() : null);
        }

        if (updateDTO.getAvatar() != null) {
            user.setAvatar(StringUtils.hasText(updateDTO.getAvatar()) ?
                updateDTO.getAvatar().trim() : null);
        }

        if (updateDTO.getBio() != null) {
            user.setBio(StringUtils.hasText(updateDTO.getBio()) ?
                updateDTO.getBio().trim() : null);
        }

        if (updateDTO.getWebsite() != null) {
            user.setWebsite(StringUtils.hasText(updateDTO.getWebsite()) ?
                updateDTO.getWebsite().trim() : null);
        }

        if (updateDTO.getPosition() != null) {
            user.setPosition(StringUtils.hasText(updateDTO.getPosition()) ?
                updateDTO.getPosition().trim() : null);
        }

        if (updateDTO.getCompany() != null) {
            user.setCompany(StringUtils.hasText(updateDTO.getCompany()) ?
                updateDTO.getCompany().trim() : null);
        }

        user.setUpdateTime(LocalDateTime.now());
        int result = userMapper.updateById(user);
        if (result <= 0) {
            throw new BusinessException(ResultCode.ERROR, "用户信息更新失败");
        }

        return Result.<Void>success();
    }

    @Override
    @Transactional
    public Result<Void> changePassword(Long userId, ChangePasswordDTO changePasswordDTO, String authorizationHeader) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }

        if (!passwordEncoder.matches(changePasswordDTO.getOldPassword(), user.getPassword())) {
            throw new BusinessException(ResultCode.OLD_PASSWORD_ERROR);
        }

        if (!PasswordPolicyUtils.validatePassword(changePasswordDTO.getNewPassword())) {
            throw new BusinessException(ResultCode.ERROR, PasswordPolicyUtils.getPasswordPolicy());
        }

        user.setPassword(passwordEncoder.encode(changePasswordDTO.getNewPassword()));
        user.setUpdateTime(LocalDateTime.now());

        int result = userMapper.updateById(user);
        if (result <= 0) {
            throw new BusinessException(ResultCode.ERROR, "密码修改失败");
        }

        if (StringUtils.hasText(authorizationHeader) && authorizationHeader.startsWith("Bearer ")) {
            String accessToken = authorizationHeader.substring(7);
            long ttl = jwtUtils.getRemainingTime(accessToken);
            if (ttl > 0) {
                redisUtils.set(ACCESS_TOKEN_BLACKLIST_KEY_PREFIX + accessToken, "1", ttl, TimeUnit.SECONDS);
            }
        }
        deleteRefreshToken(userId);

        return Result.<Void>success();
    }

    @Override
    @Transactional
    public Result<Void> resetPassword(String email, String newPassword) {
        User user = userMapper.selectByEmail(email);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }

        // 验证新密码强度
        if (!PasswordPolicyUtils.validatePassword(newPassword)) {
            throw new BusinessException(ResultCode.ERROR, PasswordPolicyUtils.getPasswordPolicy());
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setUpdateTime(LocalDateTime.now());

        int result = userMapper.updateById(user);
        if (result <= 0) {
            throw new BusinessException(ResultCode.ERROR, "密码重置失败");
        }

        return Result.<Void>success();
    }

    @Override
    public Result<Void> sendResetCode(SendResetCodeDTO sendResetCodeDTO) {
        String email = sendResetCodeDTO.getEmail();
        User user = userMapper.selectByEmail(email);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND, "邮箱未注册");
        }

        String redisKey = PASSWORD_RESET_CODE_KEY_PREFIX + email;
        Long remainSeconds = redisUtils.getExpire(redisKey, TimeUnit.SECONDS);
        if (remainSeconds != null && remainSeconds > (PASSWORD_RESET_CODE_EXPIRE_MINUTES * 60 - PASSWORD_RESET_CODE_SEND_INTERVAL_SECONDS)) {
            throw new BusinessException(ResultCode.ERROR, "验证码发送过于频繁，请稍后再试");
        }

        String verifyCode = String.format("%06d", new Random().nextInt(1_000_000));
        boolean cacheSuccess = redisUtils.set(redisKey, verifyCode, PASSWORD_RESET_CODE_EXPIRE_MINUTES, TimeUnit.MINUTES);
        if (!cacheSuccess) {
            throw new BusinessException(ResultCode.ERROR, "验证码生成失败，请稍后重试");
        }

        try {
            String emailHtml = emailTemplateService.getResetPasswordEmailHtml(
                verifyCode,
                PASSWORD_RESET_CODE_EXPIRE_MINUTES
            );

            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            helper.setFrom(mailFrom);
            helper.setTo(email);
            helper.setSubject("Lumina 密码重置验证码");
            helper.setText(emailHtml, true);

            mailSender.send(mimeMessage);
            log.info("发送重置密码验证码成功：email={}", email);
            return Result.success();
        } catch (Exception e) {
            redisUtils.delete(redisKey);
            log.error("发送重置密码验证码失败：email={}", email, e);
            throw new BusinessException(ResultCode.ERROR, "验证码发送失败，请稍后重试");
        }
    }

    @Override
    @Transactional
    public Result<Void> resetPasswordByCode(ResetPasswordByCodeDTO resetPasswordByCodeDTO) {
        String email = resetPasswordByCodeDTO.getEmail();
        String code = resetPasswordByCodeDTO.getCode();
        String newPassword = resetPasswordByCodeDTO.getNewPassword();

        User user = userMapper.selectByEmail(email);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND, "邮箱未注册");
        }

        String redisKey = PASSWORD_RESET_CODE_KEY_PREFIX + email;
        String cachedCode = redisUtils.get(redisKey);
        if (!StringUtils.hasText(cachedCode) || !cachedCode.equals(code)) {
            throw new BusinessException(ResultCode.ERROR, "验证码错误或已过期");
        }

        if (!PasswordPolicyUtils.validatePassword(newPassword)) {
            throw new BusinessException(ResultCode.ERROR, PasswordPolicyUtils.getPasswordPolicy());
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setUpdateTime(LocalDateTime.now());
        int result = userMapper.updateById(user);
        if (result <= 0) {
            throw new BusinessException(ResultCode.ERROR, "密码重置失败");
        }

        redisUtils.delete(redisKey);
        return Result.success();
    }

    @Override
    public Result<PageResult<UserDTO>> getUserList(Integer page, Integer size, String keyword) {
        IPage<User> userPage = new Page<>(page, size);
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(keyword)) {
            wrapper.like(User::getUsername, keyword)
                    .or()
                    .like(User::getNickname, keyword)
                    .or()
                    .like(User::getEmail, keyword);
        }

        wrapper.orderByDesc(User::getCreateTime);
        userPage = userMapper.selectPage(userPage, wrapper);

        List<UserDTO> userDTOList = userPage.getRecords().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

        PageResult<UserDTO> pageResult = PageResult.of(userDTOList, userPage.getTotal(), page, size);
        return Result.success(pageResult);
    }

    @Override
    @Transactional
    public Result<Void> updateUserStatus(Long userId, Integer status) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }

        user.setStatus(status);
        user.setUpdateTime(LocalDateTime.now());

        int result = userMapper.updateById(user);
        if (result <= 0) {
            throw new BusinessException(ResultCode.ERROR, "用户状态更新失败");
        }

        if (status != null && status == 0) {
            deleteRefreshToken(userId);
        }

        return Result.<Void>success();
    }

    @Override
    @Transactional
    public Result<Void> deleteUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }

        int result = userMapper.deleteById(userId);
        if (result <= 0) {
            throw new BusinessException(ResultCode.ERROR, "用户删除失败");
        }

        return Result.<Void>success();
    }

    @Override
    public User getUserByUsername(String username) {
        return userMapper.selectByUsername(username);
    }

    @Override
    public User getUserByEmail(String email) {
        return userMapper.selectByEmail(email);
    }

    @Override
    public User getUserById(Long userId) {
        return userMapper.selectById(userId);
    }

    @Override
    public Result<UserDTO> getPublicUserInfo(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }

        UserDTO userDTO = convertToDTO(user);

        // 脱敏处理，清除敏感信息
        userDTO.setEmail(null);
        userDTO.setPhone(null);
        userDTO.setLastLoginIp(null);
        userDTO.setLastLoginTime(null);
        userDTO.setAccessToken(null);
        userDTO.setRefreshToken(null);

        // 检查当前登录用户是否关注了该用户
        try {
            Long currentUserId = com.blog.utils.AuthUtils.getCurrentUserId();
            if (currentUserId != null && !currentUserId.equals(userId)) {
                LambdaQueryWrapper<UserFollow> wrapper = new LambdaQueryWrapper<>();
                wrapper.eq(UserFollow::getFollowerId, currentUserId)
                        .eq(UserFollow::getFollowingId, userId);
                Long count = userFollowMapper.selectCount(wrapper);
                userDTO.setIsFollowed(count > 0);
            }
        } catch (Exception e) {
            // 用户未登录，忽略
        }

        return Result.success(userDTO);
    }

    @Override
    @Transactional
    public Result<Void> follow(Long followerId, Long followingId) {
        log.info("关注用户：followerId={}, followingId={}", followerId, followingId);

        String lockKey = "follow:" + followerId + ":" + followingId;
        String lockValue = null;

        try {
            lockValue = redisDistributedLock.tryLock(lockKey, 5, TimeUnit.SECONDS);
            if (lockValue == null) {
                throw new BusinessException(ResultCode.ERROR, "操作过于频繁，请稍后再试");
            }

            // 检查是否自己关注自己
            if (followerId.equals(followingId)) {
                throw new BusinessException(ResultCode.ERROR, "不能关注自己");
            }

            // 检查被关注者是否存在
            User followingUser = userMapper.selectById(followingId);
            if (followingUser == null) {
                throw new BusinessException(ResultCode.USER_NOT_FOUND, "被关注用户不存在");
            }

            // 检查是否已关注（包括已逻辑删除的记录）
            UserFollow existFollow = userFollowMapper.selectByFollowerAndFollowingIncludingDeleted(followerId, followingId);

            if (existFollow != null && existFollow.getDeleted() == 0) {
                throw new BusinessException(ResultCode.FOLLOW_EXIST);
            }

            if (existFollow != null && existFollow.getDeleted() == 1) {
                // 恢复关注关系
                int restored = userFollowMapper.restoreFollow(existFollow.getId());
                if (restored <= 0) {
                    throw new BusinessException(ResultCode.ERROR, "恢复关注关系失败");
                }
            } else if (existFollow == null) {
                // 创建关注关系
                UserFollow userFollow = UserFollow.builder()
                        .followerId(followerId)
                        .followingId(followingId)
                        .createTime(LocalDateTime.now())
                        .deleted(0)
                        .build();
                userFollowMapper.insert(userFollow);
            }

            // 使用 TransactionSynchronization 在事务成功后更新计数器（带重试机制）
            final Long finalFollowingId = followingId;
            final Long finalFollowerId = followerId;
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    retryExecute(() -> userMapper.incrementFollowerCount(finalFollowingId),
                            "更新粉丝数失败");
                    retryExecute(() -> userMapper.incrementFollowingCount(finalFollowerId),
                            "更新关注数失败");
                    log.debug("事务提交后更新关注计数：followerId={}, followingId={}", finalFollowerId, finalFollowingId);
                }
            });

            // 发送关注通知
            try {
                notificationService.createNotification(
                        followingId,
                        followerId,
                        com.blog.entity.Notification.TYPE_USER_FOLLOW,
                        followerId,
                        com.blog.entity.Notification.TARGET_TYPE_USER,
                        "关注了你");
            } catch (Exception e) {
                log.error("发送关注通知失败", e);
            }

            log.info("关注用户成功：followerId={}, followingId={}", followerId, followingId);
            return Result.success();
        } finally {
            if (lockValue != null) {
                redisDistributedLock.releaseLock(lockKey, lockValue);
            }
        }
    }

    @Override
    @Transactional
    public Result<Void> unfollow(Long followerId, Long followingId) {
        log.info("取消关注用户：followerId={}, followingId={}", followerId, followingId);

        String lockKey = "follow:" + followerId + ":" + followingId;
        String lockValue = null;

        try {
            lockValue = redisDistributedLock.tryLock(lockKey, 5, TimeUnit.SECONDS);
            if (lockValue == null) {
                throw new BusinessException(ResultCode.ERROR, "操作过于频繁，请稍后再试");
            }

            // 检查关注关系是否存在
            LambdaQueryWrapper<UserFollow> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(UserFollow::getFollowerId, followerId)
                    .eq(UserFollow::getFollowingId, followingId);
            UserFollow userFollow = userFollowMapper.selectOne(wrapper);

            if (userFollow == null) {
                throw new BusinessException(ResultCode.ERROR, "未关注该用户");
            }

            // 删除关注关系
            userFollowMapper.deleteById(userFollow.getId());

            // 使用 TransactionSynchronization 在事务成功后更新计数器（带重试机制）
            final Long finalFollowingId = followingId;
            final Long finalFollowerId = followerId;
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    retryExecute(() -> userMapper.decrementFollowerCount(finalFollowingId),
                            "更新粉丝数失败");
                    retryExecute(() -> userMapper.decrementFollowingCount(finalFollowerId),
                            "更新关注数失败");
                    log.debug("事务提交后更新取消关注计数：followerId={}, followingId={}", finalFollowerId, finalFollowingId);
                }
            });

            log.info("取消关注用户成功：followerId={}, followingId={}", followerId, followingId);
            return Result.success();
        } finally {
            if (lockValue != null) {
                redisDistributedLock.releaseLock(lockKey, lockValue);
            }
        }
    }

    @Override
    public Result<Boolean> isFollowing(Long followerId, Long followingId) {
        LambdaQueryWrapper<UserFollow> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserFollow::getFollowerId, followerId)
                .eq(UserFollow::getFollowingId, followingId);
        Long count = userFollowMapper.selectCount(wrapper);
        return Result.success(count > 0);
    }

    @Override
    public Result<List<UserDTO>> getTopAuthors(Integer limit) {
        log.info("获取作者排行榜：limit={}", limit);

        // 按粉丝数降序查询
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getStatus, 1) // 只查询正常状态用户
                .orderByDesc(User::getFollowerCount)
                .last("LIMIT " + limit);

        log.debug("执行的 SQL 查询条件：status=1, 排序：follower_count DESC, limit={}", limit);

        List<User> users = userMapper.selectList(wrapper);
        log.info("查询到的用户数量：{}", users.size());
        if (!users.isEmpty()) {
            log.debug("查询到的用户列表：{}", users);
        }

        List<UserDTO> userDTOs = users.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

        // 尝试获取当前登录用户 ID，检查关注状态
        try {
            Long currentUserId = com.blog.utils.AuthUtils.getCurrentUserId();
            log.info("[Follow Debug] 成功获取当前用户 ID: {}", currentUserId);
            if (currentUserId != null && !userDTOs.isEmpty()) {
                List<Long> authorIds = userDTOs.stream().map(UserDTO::getId).collect(Collectors.toList());
                log.info("[Follow Debug] 查询关注状态，当前用户：{}, 作者列表：{}", currentUserId, authorIds);

                LambdaQueryWrapper<UserFollow> followWrapper = new LambdaQueryWrapper<>();
                followWrapper.eq(UserFollow::getFollowerId, currentUserId)
                        .in(UserFollow::getFollowingId, authorIds);

                List<UserFollow> followList = userFollowMapper.selectList(followWrapper);
                log.info("[Follow Debug] MyBatis 查询返回记录数：{}", followList.size());
                if (!followList.isEmpty()) {
                    log.info("[Follow Debug] 第一条记录：followerId={}, followingId={}",
                            followList.get(0).getFollowerId(), followList.get(0).getFollowingId());
                }

                java.util.Set<Long> followedIds = followList.stream()
                        .map(UserFollow::getFollowingId)
                        .collect(Collectors.toSet());

                log.info("[Follow Debug] 已关注的作者 ID 集合：{}", followedIds);

                for (UserDTO userDTO : userDTOs) {
                    boolean isFollowed = followedIds.contains(userDTO.getId());
                    userDTO.setIsFollowed(isFollowed);
                    log.info("[Follow Debug] 作者 {} (ID: {}) 关注状态：{}", userDTO.getNickname(), userDTO.getId(),
                            isFollowed);
                }
            } else {
                log.info("[Follow Debug] 当前用户 ID 为 null 或作者列表为空，跳过关注状态检查");
            }
        } catch (Exception e) {
            // 用户未登录或获取失败，不做处理，默认未关注
            log.warn("[Follow Debug] 获取当前用户失败，忽略关注状态检查：{}", e.getMessage());
        }

        return Result.success(userDTOs);
    }

    @Override
    public Result<List<UserDTO>> getFollowings(Long userId, Integer page, Integer size) {
        int p = (page == null || page < 1) ? 1 : page;
        int s = (size == null || size < 1) ? 10 : size;
        int offset = (p - 1) * s;

        LambdaQueryWrapper<UserFollow> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserFollow::getFollowerId, userId)
                .orderByDesc(UserFollow::getCreateTime)
                .last("LIMIT " + offset + ", " + s);

        List<UserFollow> follows = userFollowMapper.selectList(wrapper);
        if (follows == null || follows.isEmpty()) {
            return Result.success(java.util.Collections.emptyList());
        }

        List<Long> followingIds = follows.stream()
                .map(UserFollow::getFollowingId)
                .collect(java.util.stream.Collectors.toList());

        List<User> users = userMapper.selectBatchIds(followingIds);
        List<UserDTO> dtos = users.stream()
                .map(this::convertToDTO)
                .collect(java.util.stream.Collectors.toList());
        return Result.success(dtos);
    }

    @Override
    public Result<List<UserDTO>> getFollowers(Long userId, Integer page, Integer size) {
        int p = (page == null || page < 1) ? 1 : page;
        int s = (size == null || size < 1) ? 10 : size;
        int offset = (p - 1) * s;

        LambdaQueryWrapper<UserFollow> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserFollow::getFollowingId, userId)
                .orderByDesc(UserFollow::getCreateTime)
                .last("LIMIT " + offset + ", " + s);

        List<UserFollow> followers = userFollowMapper.selectList(wrapper);
        if (followers == null || followers.isEmpty()) {
            return Result.success(java.util.Collections.emptyList());
        }

        List<Long> followerIds = followers.stream()
                .map(UserFollow::getFollowerId)
                .collect(java.util.stream.Collectors.toList());

        List<User> users = userMapper.selectBatchIds(followerIds);
        List<UserDTO> dtos = users.stream()
                .map(this::convertToDTO)
                .collect(java.util.stream.Collectors.toList());
        return Result.success(dtos);
    }

    @Override
    public Result<Void> sendRegisterVerifyCode(SendRegisterCodeDTO sendRegisterCodeDTO) {
        log.info("发送注册验证码：email={}", sendRegisterCodeDTO.getEmail());

        // 验证图形验证码
        if (!captchaService.verifyCaptcha(sendRegisterCodeDTO.getCaptchaKey(), sendRegisterCodeDTO.getCaptcha())) {
            throw new BusinessException(ResultCode.ERROR, "图形验证码错误或已过期");
        }

        String email = sendRegisterCodeDTO.getEmail();

        // 检查邮箱是否已被注册
        User existingUser = userMapper.selectByEmail(email);
        if (existingUser != null) {
            throw new BusinessException(ResultCode.EMAIL_EXIST, "该邮箱已被注册");
        }

        // 检查频率限制
        String limitKey = REGISTER_CODE_LIMIT_KEY_PREFIX + email;
        Long remainSeconds = redisUtils.getExpire(limitKey, TimeUnit.SECONDS);
        if (remainSeconds != null && remainSeconds > 0) {
            throw new BusinessException(ResultCode.ERROR, "验证码发送过于频繁，请" + remainSeconds + "秒后重试");
        }

        // 生成 6 位验证码
        String verifyCode = String.format("%06d", new Random().nextInt(1_000_000));

        // 存储验证码到 Redis
        String codeKey = REGISTER_CODE_KEY_PREFIX + email;
        boolean cacheSuccess = redisUtils.set(codeKey, verifyCode, REGISTER_CODE_EXPIRE_MINUTES, TimeUnit.MINUTES);
        if (!cacheSuccess) {
            throw new BusinessException(ResultCode.ERROR, "验证码生成失败，请稍后重试");
        }

        // 设置频率限制标记
        redisUtils.set(limitKey, "1", REGISTER_CODE_LIMIT_SECONDS, TimeUnit.SECONDS);

        // 发送邮件
        try {
            String emailHtml = emailTemplateService.getRegisterVerifyCodeEmailHtml(
                verifyCode, 
                REGISTER_CODE_EXPIRE_MINUTES
            );
            
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            helper.setFrom(mailFrom);
            helper.setTo(email);
            helper.setSubject("Lumina 邮箱验证");
            helper.setText(emailHtml, true);
            
            mailSender.send(mimeMessage);
            log.info("发送注册验证码成功：email={}", email);
            return Result.success();
        } catch (Exception e) {
            redisUtils.delete(codeKey);
            redisUtils.delete(limitKey);
            log.error("发送注册验证码失败：email={}", email, e);
            throw new BusinessException(ResultCode.ERROR, "验证码发送失败，请稍后重试");
        }
    }

    /**
     * 将 User 实体转换为 UserDTO
     * 
     * @param user 用户实体
     * @return 用户 DTO
     */
    private UserDTO convertToDTO(User user) {
        UserDTO userDTO = new UserDTO();
        BeanUtils.copyProperties(user, userDTO);

        // 手动转换 role 字段：Integer -> String
        if (user.getRole() != null) {
            switch (user.getRole()) {
                case 2:
                case 3:
                    userDTO.setRole("admin");
                    break;
                default:
                    userDTO.setRole("user");
            }
        } else {
            userDTO.setRole("user");
        }

        return userDTO;
    }

    /**
     * 获取客户端 IP 地址
     * 
     * @return 客户端 IP
     */
    private String getClientIp() {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 如果是多级代理，取第一个 IP
        if (ip != null && ip.contains(",")) {
            ip = ip.substring(0, ip.indexOf(",")).trim();
        }
        return ip;
    }

    /**
     * 重试执行任务
     * 用于在事务提交后更新计数失败时进行重试
     *
     * @param task 要执行的任务
     * @param errorMsg 错误日志消息
     */
    private void retryExecute(Runnable task, String errorMsg) {
        int maxRetries = 5;
        for (int i = 0; i < maxRetries; i++) {
            try {
                task.run();
                return;
            } catch (Exception e) {
                if (i == maxRetries - 1) {
                    log.error(errorMsg + "，已达最大重试次数", e);
                } else {
                    try {
                        Thread.sleep(100 * (i + 1));
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                    log.warn(errorMsg + "，重试 {}/{}", i + 1, maxRetries);
                }
            }
        }
    }

    @Override
    @Transactional
    public Result<UserDTO> githubLogin(String code) {
        log.info("GitHub OAuth 登录，授权码：{}", code);

        try {
            String tokenUrl = "https://github.com/login/oauth/access_token";
            org.springframework.util.MultiValueMap<String, String> params = new org.springframework.util.LinkedMultiValueMap<>();
            params.add("client_id", githubClientId);
            params.add("client_secret", githubClientSecret);
            params.add("code", code);
            params.add("redirect_uri", githubRedirectUri);

            log.info("请求 GitHub Token，clientId={}, redirectUri={}", githubClientId, githubRedirectUri);

            org.springframework.http.HttpHeaders tokenHeaders = new org.springframework.http.HttpHeaders();
            tokenHeaders.set("Accept", "application/json");
            org.springframework.http.HttpEntity<org.springframework.util.MultiValueMap<String, String>> requestEntity =
                new org.springframework.http.HttpEntity<>(params, tokenHeaders);

            org.springframework.http.ResponseEntity<String> tokenResponse = restTemplate.postForEntity(
                tokenUrl, requestEntity, String.class);

            log.info("GitHub Token 响应状态：{}, body：{}", tokenResponse.getStatusCode(), tokenResponse.getBody());

            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            com.fasterxml.jackson.databind.JsonNode jsonNode = mapper.readTree(tokenResponse.getBody());

            if (jsonNode.has("error")) {
                String error = jsonNode.get("error").asText();
                String errorDesc = jsonNode.has("error_description") ? jsonNode.get("error_description").asText() : "无详细描述";
                log.error("GitHub OAuth 错误：{} - {}", error, errorDesc);
                throw new BusinessException(ResultCode.ERROR, "GitHub 授权失败：" + error + " - " + errorDesc);
            }

            String accessToken = jsonNode.get("access_token").asText();
            log.info("获取 GitHub Access Token 成功");

            String userInfoUrl = "https://api.github.com/user";
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.add("Authorization", "Bearer " + accessToken);
            headers.add("User-Agent", "Lumina-Blog");
            org.springframework.http.HttpEntity<String> userInfoRequest = new org.springframework.http.HttpEntity<>(headers);

            org.springframework.http.ResponseEntity<String> userInfoResponse = restTemplate.exchange(
                userInfoUrl, org.springframework.http.HttpMethod.GET, userInfoRequest, String.class);

            com.fasterxml.jackson.databind.JsonNode userInfoNode = mapper.readTree(userInfoResponse.getBody());
            Long githubId = userInfoNode.get("id").asLong();
            String githubUsername = userInfoNode.get("login").asText();
            String avatarUrl = userInfoNode.has("avatar_url") ? userInfoNode.get("avatar_url").asText() : null;
            String email = userInfoNode.has("email") && !userInfoNode.get("email").isNull()
                ? userInfoNode.get("email").asText() : null;

            if (email == null) {
                email = githubUsername + "@github.placeholder";
            }

            log.info("GitHub 用户信息：id={}, username={}, email={}", githubId, githubUsername, email);

            User existingUser = userMapper.selectByGithubId(githubId);
            if (existingUser != null) {
                String newAccessToken = jwtUtils.generateAccessToken(existingUser.getId(), existingUser.getUsername());
                String newRefreshToken = jwtUtils.generateRefreshToken(existingUser.getId(), existingUser.getUsername());
                storeRefreshToken(existingUser.getId(), newRefreshToken);

                existingUser.setLastLoginTime(LocalDateTime.now());
                existingUser.setLastLoginIp(getClientIp());
                userMapper.updateById(existingUser);

                UserDTO userDTO = convertToDTO(existingUser);
                userDTO.setAccessToken(newAccessToken);
                userDTO.setRefreshToken(newRefreshToken);

                log.info("GitHub 老用户登录成功：username={}", existingUser.getUsername());
                return Result.success(userDTO);
            }

            if (userMapper.selectByUsername(githubUsername) != null) {
                String newUsername = githubUsername + "_gh";
                User newUser = createGithubUser(githubId, newUsername, avatarUrl, email);
                return loginAndReturnDto(newUser);
            }

            User emailExistingUser = userMapper.selectByEmail(email);
            if (emailExistingUser != null) {
                emailExistingUser.setGithubId(githubId);
                if (avatarUrl != null && emailExistingUser.getAvatar() == null) {
                    emailExistingUser.setAvatar(avatarUrl);
                }
                String newAccessToken = jwtUtils.generateAccessToken(emailExistingUser.getId(), emailExistingUser.getUsername());
                String newRefreshToken = jwtUtils.generateRefreshToken(emailExistingUser.getId(), emailExistingUser.getUsername());
                storeRefreshToken(emailExistingUser.getId(), newRefreshToken);

                emailExistingUser.setLastLoginTime(LocalDateTime.now());
                emailExistingUser.setLastLoginIp(getClientIp());
                emailExistingUser.setUpdateTime(LocalDateTime.now());
                userMapper.updateById(emailExistingUser);

                UserDTO userDTO = convertToDTO(emailExistingUser);
                userDTO.setAccessToken(newAccessToken);
                userDTO.setRefreshToken(newRefreshToken);

                log.info("GitHub 用户绑定到已有账号成功：username={}, email={}", emailExistingUser.getUsername(), email);
                return Result.success(userDTO);
            }

            User newUser = createGithubUser(githubId, githubUsername, avatarUrl, email);
            return loginAndReturnDto(newUser);

        } catch (Exception e) {
            log.error("GitHub OAuth 登录失败", e);
            String errorMsg = e.getMessage();
            if (errorMsg == null || errorMsg.isEmpty()) {
                errorMsg = "GitHub 登录失败，请稍后重试";
            }
            throw new BusinessException(ResultCode.ERROR, errorMsg);
        }
    }

    private User createGithubUser(Long githubId, String username, String avatar, String email) {
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(java.util.UUID.randomUUID().toString()));
        user.setEmail(email);
        user.setNickname(username);
        user.setAvatar(avatar);
        user.setGithubId(githubId);
        user.setStatus(1);
        user.setRole(1);
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        userMapper.insert(user);
        log.info("创建 GitHub 新用户：username={}, githubId={}", username, githubId);
        return user;
    }

    private Result<UserDTO> loginAndReturnDto(User user) {
        String accessToken = jwtUtils.generateAccessToken(user.getId(), user.getUsername());
        String refreshToken = jwtUtils.generateRefreshToken(user.getId(), user.getUsername());
        storeRefreshToken(user.getId(), refreshToken);

        user.setLastLoginTime(LocalDateTime.now());
        user.setLastLoginIp(getClientIp());
        userMapper.updateById(user);

        UserDTO userDTO = convertToDTO(user);
        userDTO.setAccessToken(accessToken);
        userDTO.setRefreshToken(refreshToken);

        log.info("GitHub 用户登录成功：username={}", user.getUsername());
        return Result.success(userDTO);
    }
}
