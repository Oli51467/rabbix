package com.sdu.rabbitmq.order.service.impl;

import cn.hutool.core.util.StrUtil;
import com.sdu.rabbitmq.common.commons.RedisKey;
import com.sdu.rabbitmq.common.utils.JwtUtil;
import com.sdu.rabbitmq.order.service.LoginService;
import com.sdu.rabbitmq.order.utils.RedisUtil;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static com.sdu.rabbitmq.common.commons.RedisKey.*;

@Service("LoginService")
public class LoginServiceImpl implements LoginService {

    @Override
    public String login(Long uid) {
        String userTokenKey = RedisKey.getKey(USER_TOKEN_KEY, uid);
        String token = RedisUtil.get(userTokenKey);
        if (StrUtil.isNotBlank(token)) {
            long expireDays = RedisUtil.getExpire(userTokenKey, TimeUnit.DAYS);
            if (expireDays < TOKEN_RENEWAL_DAYS) {
                RedisUtil.expire(userTokenKey, TOKEN_EXPIRE_DAYS, TimeUnit.DAYS);
            }
            return token;
        }
        token = JwtUtil.createToken(uid);
        RedisUtil.setExpTime(userTokenKey, token, TOKEN_EXPIRE_DAYS, TimeUnit.DAYS);
        return token;
    }

    /**
     * 认证token是否有效
     * @param token jwt令牌
     * @return 是否有效
     */
    @Override
    public boolean authenticate(String token) {
        Long uid = JwtUtil.getUidOrNull(token);
        if (Objects.isNull(uid)) {
            return false;
        }
        String userTokenKey = RedisKey.getKey(USER_TOKEN_KEY, uid);
        String tokenRedis = RedisUtil.get(userTokenKey);
        return Objects.equals(tokenRedis, token);
    }

    @Override
    public Long getUseridByToken(String token) {
        return JwtUtil.getUidOrNull(token);
    }
}
