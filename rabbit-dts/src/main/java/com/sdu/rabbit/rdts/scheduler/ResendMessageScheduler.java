package com.sdu.rabbit.rdts.scheduler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@EnableScheduling
@Configuration
@Component
@Slf4j
public class ResendMessageScheduler {

    @Value("${rdts.resend-time}")
    private Integer resendTimeDuration;

    @Scheduled(fixedDelayString = "${rdts.resend-frequency}")
    public void resendMessage() {

    }
}
