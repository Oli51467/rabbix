package com.sdu.rabbitmq.restaurant.entity.po;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.sdu.rabbitmq.restaurant.enums.RestaurantStatus;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.Date;

@Getter
@Setter
@ToString
public class Restaurant {

    private Long id;

    private String name;

    private String address;

    private RestaurantStatus status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm", timezone = "GMT+8")
    private Date createTime;
}
