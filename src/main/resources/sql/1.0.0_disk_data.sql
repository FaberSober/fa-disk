SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Records of base_rbac_menu
-- ----------------------------
BEGIN;
INSERT INTO `base_rbac_menu` (`id`, `parent_id`, `name`, `sort`, `level`, `icon`, `status`, `link_type`, `link_url`, `crt_time`, `crt_user`, `crt_name`, `crt_host`, `upd_time`, `upd_user`, `upd_name`, `upd_host`, `deleted`) VALUES (22000000, 0, '文档管理', 2, 0, 'floppy-disk', 1, 1, '/admin/disk', '2022-12-22 09:48:01', '1', '超级管理员', '127.0.0.1', NULL, NULL, NULL, NULL, 0);
INSERT INTO `base_rbac_menu` (`id`, `parent_id`, `name`, `sort`, `level`, `icon`, `status`, `link_type`, `link_url`, `crt_time`, `crt_user`, `crt_name`, `crt_host`, `upd_time`, `upd_user`, `upd_name`, `upd_host`, `deleted`) VALUES (22010000, 22000000, '文件仓库', 0, 1, 'store', 1, 1, '/admin/disk/buckets', '2022-12-23 10:23:29', '1', '超级管理员', '127.0.0.1', NULL, NULL, NULL, NULL, 0);
INSERT INTO `base_rbac_menu` (`id`, `parent_id`, `name`, `sort`, `level`, `icon`, `status`, `link_type`, `link_url`, `crt_time`, `crt_user`, `crt_name`, `crt_host`, `upd_time`, `upd_user`, `upd_name`, `upd_host`, `deleted`) VALUES (22020000, 22000000, '文件网盘', 1, 1, 'laptop-file', 1, 1, '/admin/disk/store', '2022-12-22 09:49:00', '1', '超级管理员', '127.0.0.1', NULL, NULL, NULL, NULL, 0);
COMMIT;


SET FOREIGN_KEY_CHECKS = 1;
