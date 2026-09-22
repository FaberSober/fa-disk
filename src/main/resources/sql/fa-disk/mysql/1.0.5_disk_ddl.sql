-- ------------------------- info -------------------------
-- @@ver: 1_000_005
-- @@info: 扩大文件标签颜色字段长度
-- ------------------------- info -------------------------

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

ALTER TABLE `disk_store_tag`
    MODIFY COLUMN `color` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '颜色';

SET FOREIGN_KEY_CHECKS = 1;
