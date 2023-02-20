SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Records of base_rbac_menu
-- ----------------------------
BEGIN;
INSERT INTO `base_rbac_menu` (`id`, `parent_id`, `name`, `sort`, `level`, `icon`, `status`, `link_type`, `link_url`, `crt_time`, `crt_user`, `crt_name`, `crt_host`, `upd_time`, `upd_user`, `upd_name`, `upd_host`, `deleted`) VALUES (44, 0, '文档管理', 2, 0, 'floppy-disk', 1, 1, '/admin/disk', '2022-12-22 09:48:01', '1', '超级管理员', '192.168.58.1', '2022-12-22 09:48:03', '1', '超级管理员', '192.168.58.1', 0);
INSERT INTO `base_rbac_menu` (`id`, `parent_id`, `name`, `sort`, `level`, `icon`, `status`, `link_type`, `link_url`, `crt_time`, `crt_user`, `crt_name`, `crt_host`, `upd_time`, `upd_user`, `upd_name`, `upd_host`, `deleted`) VALUES (45, 44, '文件网盘', 1, 1, 'laptop-file', 1, 1, '/admin/disk/store', '2022-12-22 09:49:00', '1', '超级管理员', '192.168.58.1', '2022-12-23 10:33:29', '1', '超级管理员', '192.168.58.1', 0);
INSERT INTO `base_rbac_menu` (`id`, `parent_id`, `name`, `sort`, `level`, `icon`, `status`, `link_type`, `link_url`, `crt_time`, `crt_user`, `crt_name`, `crt_host`, `upd_time`, `upd_user`, `upd_name`, `upd_host`, `deleted`) VALUES (46, 45, '全部文件', 0, 1, 'box-archive', 1, 1, '/admin/disk/store/alls', '2022-12-27 11:10:58', '1', '超级管理员', '192.168.58.1', '2022-12-27 11:10:59', '1', '超级管理员', '192.168.0.101', 1);
INSERT INTO `base_rbac_menu` (`id`, `parent_id`, `name`, `sort`, `level`, `icon`, `status`, `link_type`, `link_url`, `crt_time`, `crt_user`, `crt_name`, `crt_host`, `upd_time`, `upd_user`, `upd_name`, `upd_host`, `deleted`) VALUES (47, 45, '最近文件', 1, 1, 'clock', 1, 1, '/admin/disk/store/recent', '2022-12-27 11:11:01', '1', '超级管理员', '192.168.58.1', '2022-12-27 11:11:01', '1', '超级管理员', '192.168.0.101', 1);
INSERT INTO `base_rbac_menu` (`id`, `parent_id`, `name`, `sort`, `level`, `icon`, `status`, `link_type`, `link_url`, `crt_time`, `crt_user`, `crt_name`, `crt_host`, `upd_time`, `upd_user`, `upd_name`, `upd_host`, `deleted`) VALUES (48, 45, '星标文件', 2, 1, 'star', 1, 1, '/admin/disk/store/starred', '2022-12-27 11:11:03', '1', '超级管理员', '192.168.58.1', '2022-12-27 11:11:03', '1', '超级管理员', '192.168.0.101', 1);
INSERT INTO `base_rbac_menu` (`id`, `parent_id`, `name`, `sort`, `level`, `icon`, `status`, `link_type`, `link_url`, `crt_time`, `crt_user`, `crt_name`, `crt_host`, `upd_time`, `upd_user`, `upd_name`, `upd_host`, `deleted`) VALUES (49, 45, '回收站', 3, 1, 'trash-can', 1, 1, '/admin/disk/store/recycle', '2022-12-27 11:11:04', '1', '超级管理员', '192.168.58.1', '2022-12-27 11:11:05', '1', '超级管理员', '192.168.0.101', 1);
INSERT INTO `base_rbac_menu` (`id`, `parent_id`, `name`, `sort`, `level`, `icon`, `status`, `link_type`, `link_url`, `crt_time`, `crt_user`, `crt_name`, `crt_host`, `upd_time`, `upd_user`, `upd_name`, `upd_host`, `deleted`) VALUES (50, 46, '新增库', 0, 9, NULL, 1, 1, '/admin/disk/store/alls/bucket@add', '2022-12-23 10:22:37', '1', '超级管理员', '192.168.58.1', '2022-12-23 10:22:39', '1', '超级管理员', '192.168.58.1', 1);
INSERT INTO `base_rbac_menu` (`id`, `parent_id`, `name`, `sort`, `level`, `icon`, `status`, `link_type`, `link_url`, `crt_time`, `crt_user`, `crt_name`, `crt_host`, `upd_time`, `upd_user`, `upd_name`, `upd_host`, `deleted`) VALUES (51, 44, '文件仓库', 0, 1, 'store', 1, 1, '/admin/disk/buckets', '2022-12-23 10:23:29', '1', '超级管理员', '192.168.58.1', '2022-12-23 10:23:33', '1', '超级管理员', '192.168.58.1', 0);
COMMIT;


SET FOREIGN_KEY_CHECKS = 1;
