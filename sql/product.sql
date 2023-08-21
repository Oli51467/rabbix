SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for product
-- ----------------------------
DROP TABLE IF EXISTS `product`;
CREATE TABLE `product`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT COMMENT '产品id',
  `name` varchar(36) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '名称',
  `price` decimal(9, 2) NULL DEFAULT NULL COMMENT '单价',
  `restaurant_id` bigint(0) NULL DEFAULT NULL COMMENT '餐厅id',
  `stock` int(0) NULL DEFAULT NULL COMMENT '剩余库存',
  `stock_locked` int(0) NULL DEFAULT NULL COMMENT '锁定库存',
  `status` varchar(36) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '状态',
  `create_time` datetime(0) NULL DEFAULT NULL COMMENT '时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of product
-- ----------------------------
INSERT INTO `product` VALUES (2, '北京烤鸭', 18.38, 1, 100, 0, 'AVAILABLE', '2020-05-06 19:19:04');
INSERT INTO `product` VALUES (3, '鱼香肉丝', 25.25, 1, 100, 0, 'AVAILABLE', '2020-05-06 19:19:05');

SET FOREIGN_KEY_CHECKS = 1;

truncate table order_detail;
truncate table order_product;
truncate table settlement;
truncate table trans_message;
update product set stock = 100, stock_locked = 0 where id = 2;
update product set stock = 100, stock_locked = 0 where id = 3;