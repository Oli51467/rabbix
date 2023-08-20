SET NAMES utf8mb4;
SET
FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for order_product
-- ----------------------------
DROP TABLE IF EXISTS `order_product`;
CREATE TABLE `order_product`
(
    `order_id`      bigint(0) NOT NULL COMMENT '订单id',
    `product_id`    bigint(0) NOT NULL COMMENT '商品id',
    `count` int(0) NULL DEFAULT NULL COMMENT '商品件数',
    `create_time` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '时间',
    PRIMARY KEY (`order_id`, `product_id`, `create_time`) USING BTREE
) ENGINE = InnoDB ROW_FORMAT = Dynamic;