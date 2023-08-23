package com.sdu.rabbix.delivery.entity.po;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.sdu.rabbix.delivery.enums.DeliverymanStatus;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.Date;

@Getter
@Setter
@ToString
public class Deliveryman {

    private Long id;

    private String name;

    private DeliverymanStatus status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm", timezone = "GMT+8")
    private Date createTime;
}
