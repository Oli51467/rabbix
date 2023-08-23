package com.sdu.rabbix.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.sdu.rabbix.common.commons.RedisKey;
import com.sdu.rabbix.common.response.ResponseResult;
import com.sdu.rabbix.common.response.exception.ExceptionEnum;
import com.sdu.rabbix.common.utils.JwtUtil;
import com.sdu.rabbix.common.utils.RedisUtil;
import com.sdu.rabbix.order.entity.dto.RegisterReq;
import com.sdu.rabbix.order.entity.po.User;
import com.sdu.rabbix.order.entity.vo.BaseUserResp;
import com.sdu.rabbix.order.repository.UserMapper;
import com.sdu.rabbix.order.service.AccountService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

import static com.sdu.rabbix.common.commons.RedisKey.USER_TOKEN_KEY;

@Service("AccountService")
public class AccountServiceImpl implements AccountService {

    @Resource
    private UserMapper userMapper;

    @Override
    public ResponseResult register(RegisterReq req) {
        String username = req.getUsername();
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("name", username);
        User userDB = userMapper.selectOne(queryWrapper);
        if (null != userDB) {
            return ResponseResult.fail(ExceptionEnum.USER_EXIST.getMsg());
        }
        User user = new User(req.getUsername(), req.getPassword());
        userMapper.insert(user);
        String token = JwtUtil.createToken(user.getId());
        BaseUserResp resp = BaseUserResp.builder().id(user.getId()).username(username).token(token).build();
        String userTokenKey = RedisKey.getKey(USER_TOKEN_KEY, user.getId());
        RedisUtil.setExpTime(userTokenKey, token, 5, TimeUnit.DAYS);
        return ResponseResult.ok(resp);
    }
}
