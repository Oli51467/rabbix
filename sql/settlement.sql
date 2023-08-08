SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for settlement
-- ----------------------------
DROP TABLE IF EXISTS `settlement`;
CREATE TABLE `settlement`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT COMMENT '结算id',
  `order_id` bigint(0) NULL DEFAULT NULL COMMENT '订单id',
  `transaction_id` bigint(0) NULL DEFAULT NULL COMMENT '交易id',
  `amount` decimal(9, 2) NULL DEFAULT NULL COMMENT '金额',
  `status` varchar(36) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '状态',
  `create_time` datetime(0) NULL DEFAULT NULL COMMENT '时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1168 ROW_FORMAT = Dynamic;

SET FOREIGN_KEY_CHECKS = 1;
