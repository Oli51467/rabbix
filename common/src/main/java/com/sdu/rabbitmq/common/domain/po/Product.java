package com.sdu.rabbitmq.common.domain.po;

import com.baomidou.mybatisplus.annotation.TableId;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.sdu.rabbitmq.common.commons.enums.ProductStatus;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.util.Date;

@Getter
@Setter
@ToString
public class Product {

    @TableId
    private Long id;

    private String name;

    private BigDecimal price;

    private Long restaurantId;

    private ProductStatus status;

    private Integer stock;

    private Integer stockLocked;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm", timezone = "GMT+8")
    private Date createTime;
}
