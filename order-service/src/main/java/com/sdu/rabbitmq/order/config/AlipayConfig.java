package com.sdu.rabbitmq.order.config;

import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AlipayConfig {

    @Bean
    public AlipayClient alipayClient(AlipayProperties alipayProperties) {
        return new DefaultAlipayClient(alipayProperties.getGatewayUrl(),
                alipayProperties.getAppId(),
                alipayProperties.getMerchantPrivateKey(),
                "json",
                alipayProperties.getCharset(),
                alipayProperties.getAlipayPublicKey(),
                alipayProperties.getSignType());
    }
}
