package com.sdu.rabbitmq.order.entity.dto;

import lombok.Data;
import lombok.Setter;

@Data
@Setter
public class RequestInfo {

    private Long uid;

    private String ip;
}
