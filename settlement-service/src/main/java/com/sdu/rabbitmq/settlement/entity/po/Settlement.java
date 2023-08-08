package com.sdu.rabbitmq.settlement.entity.po;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.sdu.rabbitmq.settlement.common.enums.SettlementStatus;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.util.Date;

@Getter
@Setter
@ToString
public class Settlement {

    private Long id;

    private Long orderId;

    private Long transactionId;

    private SettlementStatus status;

    private BigDecimal amount;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm", timezone = "GMT+8")
    private Date createTime;
}
