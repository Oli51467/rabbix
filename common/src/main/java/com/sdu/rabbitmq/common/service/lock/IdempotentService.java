package com.sdu.rabbitmq.common.service.lock;

import com.sdu.rabbitmq.common.response.exception.BusinessException;
import com.sdu.rabbitmq.common.response.exception.ExceptionEnum;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class IdempotentService {

    @Autowired
    private RedissonClient redissonClient;

    public <T> T executeWithIdempotentCheck(String key, long timeout, TimeUnit timeUnit, SupplierThrow<T> supplier) throws Throwable {
        RLock lock = redissonClient.getLock(key);
        if (lock.isLocked()) {
            throw new BusinessException(ExceptionEnum.REPEAT_REQUEST);
        }
        lock.lock(timeout, timeUnit);
        return supplier.get();
    }

    @FunctionalInterface
    public interface SupplierThrow<T> {
        T get() throws Throwable;
    }
}
