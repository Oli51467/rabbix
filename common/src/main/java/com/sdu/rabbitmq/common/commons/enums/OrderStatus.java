package com.sdu.rabbitmq.common.commons.enums;

public enum OrderStatus {

    /**
     * 等待支付中
     */
    WAITING_PAY,
    /**
     * 创建中
     */
    ORDER_CREATING,
    /**
     * 餐厅已确认
     */
    RESTAURANT_CONFIRMED,
    /**
     * 骑手确认
     */
    DELIVERYMAN_CONFIRMED,
    /**
     * 已结算
     */
    SETTLEMENT_CONFIRMED,
    /**
     * 订单已创建
     */
    ORDER_CREATED,
    /**
     * 订单取消
     */
    CANCELED,
    /**
     * 订单创建失败
     */
    FAILED
}