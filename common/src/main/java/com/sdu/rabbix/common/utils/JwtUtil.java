package com.sdu.rabbix.common.utils;

import cn.hutool.core.util.StrUtil;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import lombok.extern.slf4j.Slf4j;

import java.util.Date;
import java.util.Map;
import java.util.Optional;

@Slf4j
public class JwtUtil {

    /**
     * token秘钥，请勿泄露，请勿随便修改
     */
    public static final String secret = "rfoin3gj3409rf3er43rf34grevdsgq4325234r2";

    private static final String UID_CLAIM = "uid";
    private static final String CREATE_TIME = "createTime";

    /**
     * JWT生成Token
     * JWT构成: header, payload, signature
     */
    public static String createToken(Long uid) {
        // build token
        return JWT.create()
                .withClaim(UID_CLAIM, uid)
                .withClaim(UID_CLAIM, uid) // 只存一个uid信息，其他的自己去redis查
                .withClaim(CREATE_TIME, new Date())
                .sign(Algorithm.HMAC256(secret));
    }

    /**
     * 解密Token
     */
    public static Map<String, Claim> verifyToken(String token) {
        if (StrUtil.isEmpty(token)) {
            return null;
        }
        try {
            JWTVerifier verifier = JWT.require(Algorithm.HMAC256(secret)).build();
            DecodedJWT jwt = verifier.verify(token);
            return jwt.getClaims();
        } catch (Exception e) {
            log.error("decode error,token:{}", token);
        }
        return null;
    }


    /**
     * 根据Token获取uid
     */
    public static Long getUidOrNull(String token) {
        return Optional.ofNullable(verifyToken(token))
                .map(map -> map.get(UID_CLAIM))
                .map(Claim::asLong)
                .orElse(null);
    }
}
