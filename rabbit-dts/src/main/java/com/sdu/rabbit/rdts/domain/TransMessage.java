package com.sdu.rabbit.rdts.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.sdu.rabbit.rdts.enums.TransMessageType;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.Date;

@Getter
@Setter
@ToString
public class TransMessage {

    private String id;

    private String service;

    private TransMessageType type;

    private String exchange;

    private String routingKey;

    private String queue;

    private Integer sequence;

    private String payload;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm", timezone = "GMT+8")
    private Date createTime;
}
