package com.sdu.rabbix.common.aspect;

import cn.hutool.core.util.StrUtil;
import com.sdu.rabbix.common.annotation.Idempotent;
import com.sdu.rabbix.common.service.lock.IdempotentService;
import com.sdu.rabbix.common.utils.SpElUtils;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

@Aspect
@Component
@Slf4j
@Order(0)
public class IdempotentAspect {

    @Autowired
    private IdempotentService idempotentService;

    @Around("@annotation(idempotent)")
    public Object around(ProceedingJoinPoint joinPoint, Idempotent idempotent) throws Throwable {
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        // 默认方法限定名+注解排名（可能多个）
        String prefix = StrUtil.isBlank(idempotent.prefix()) ? SpElUtils.getMethodKey(method) : idempotent.prefix();
        String key = SpElUtils.parseSpEl(method, joinPoint.getArgs(), idempotent.key());
        return idempotentService.executeWithIdempotentCheck(prefix + ":" + key, idempotent.waitTime(), idempotent.unit(), joinPoint::proceed);
    }
}
