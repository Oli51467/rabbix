package com.sdu.rabbitmq.rdts.annotation;

import org.springframework.lang.Nullable;

import java.util.concurrent.Executor;

public interface SecureInvokeConfigurer {

    /**
     * 返回一个线程池
     */
    @Nullable
    default Executor getSecureInvokeExecutor() {
        return null;
    }

}
