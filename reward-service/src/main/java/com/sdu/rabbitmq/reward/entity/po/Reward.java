package com.sdu.rabbitmq.reward.entity.po;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.sdu.rabbitmq.reward.enums.RewardStatus;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.util.Date;

@Getter
@Setter
@ToString
public class Reward {

    private Long id;

    private Long orderId;

    private BigDecimal amount;

    private RewardStatus status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm", timezone = "GMT+8")
    private Date createTime;
}
