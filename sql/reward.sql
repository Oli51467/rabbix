SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for reward
-- ----------------------------
DROP TABLE IF EXISTS `reward`;
CREATE TABLE `reward`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT COMMENT '奖励id',
  `order_id` bigint(0) NULL DEFAULT NULL COMMENT '订单id',
  `amount` decimal(9, 2) NULL DEFAULT NULL COMMENT '积分量',
  `status` varchar(36) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '状态',
  `create_time` datetime(0) NULL DEFAULT NULL COMMENT '时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 8 ROW_FORMAT = Dynamic;

SET FOREIGN_KEY_CHECKS = 1;
