package com.sdu.rabbitmq.rdts.domain.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.sdu.rabbitmq.rdts.enums.TransMessageType;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.Date;

@Getter
@Setter
@ToString
public class TransMessage {

    private String id;

    /**
     * 所属服务
     */
    private String service;

    /**
     * 消息类型
     */
    private TransMessageType type;

    /**
     * 目的交换机
     */
    private String exchange;

    /**
     * 目的路由key
     */
    private String routingKey;

    /**
     * 目的队列
     */
    private String queue;

    /**
     * 第几次发送
     */
    private Integer sequence;

    /**
     * 消息体
     */
    private String payload;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm", timezone = "GMT+8")
    private Date createTime;
}
