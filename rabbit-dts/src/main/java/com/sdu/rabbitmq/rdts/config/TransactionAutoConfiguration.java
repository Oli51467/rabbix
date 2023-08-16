package com.sdu.rabbitmq.rdts.config;

import com.sdu.rabbitmq.rdts.annotation.SecureInvokeConfigurer;
import com.sdu.rabbitmq.rdts.aspect.SecureInvokeAspect;
import com.sdu.rabbitmq.rdts.dao.SecureInvokeRecordDAO;
import com.sdu.rabbitmq.rdts.repository.SecureInvokeRecordMapper;
import com.sdu.rabbitmq.rdts.service.MQProducer;
import com.sdu.rabbitmq.rdts.service.SecureInvokeService;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.lang.Nullable;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.util.CollectionUtils;
import org.springframework.util.function.SingletonSupplier;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Configuration
@EnableScheduling
@MapperScan(basePackageClasses = SecureInvokeRecordMapper.class)
@Import({SecureInvokeAspect.class, SecureInvokeRecordDAO.class})
public class TransactionAutoConfiguration {

    @Nullable
    protected Executor executor;

    /**
     * Collect any beans through autowiring.
     */
    @Autowired
    void setConfigurers(ObjectProvider<SecureInvokeConfigurer> configurers) {
        Supplier<SecureInvokeConfigurer> configurer = SingletonSupplier.of(() -> {
            List<SecureInvokeConfigurer> candidates = configurers.stream().collect(Collectors.toList());
            if (CollectionUtils.isEmpty(candidates)) {
                return null;
            }
            if (candidates.size() > 1) {
                throw new IllegalStateException("Only one SecureInvokeConfigurer may exist");
            }
            return candidates.get(0);
        });
        executor = Optional.ofNullable(configurer.get()).map(SecureInvokeConfigurer::getSecureInvokeExecutor).orElse(ForkJoinPool.commonPool());
    }

    @Bean
    public SecureInvokeService getSecureInvokeService(SecureInvokeRecordDAO secureInvokeRecordDAO) {
        return new SecureInvokeService(secureInvokeRecordDAO, executor);
    }

    @Bean
    public MQProducer getMQProducer() {
        return new MQProducer();
    }
}
