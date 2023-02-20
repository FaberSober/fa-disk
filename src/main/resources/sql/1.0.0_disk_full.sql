/*
 Navicat Premium Data Transfer

 Source Server         : localhost-dev-root
 Source Server Type    : MySQL
 Source Server Version : 50740
 Source Host           : localhost:3306
 Source Schema         : file_web

 Target Server Type    : MySQL
 Target Server Version : 50740
 File Encoding         : 65001

 Date: 20/02/2023 11:32:11
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for disk_store_bucket
-- ----------------------------
DROP TABLE IF EXISTS `disk_store_bucket`;
CREATE TABLE `disk_store_bucket` (
  `id` int(11) unsigned NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `name` varchar(255) NOT NULL COMMENT '库名称',
  `size` bigint(20) DEFAULT NULL COMMENT '总文件大小',
  `dir_count` int(11) DEFAULT NULL COMMENT '目录数量',
  `file_count` int(11) DEFAULT NULL COMMENT '文件数量',
  `crt_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '创建时间',
  `crt_user` varchar(32) NOT NULL COMMENT '创建用户ID',
  `crt_name` varchar(255) NOT NULL COMMENT '创建用户',
  `crt_host` varchar(255) DEFAULT NULL COMMENT '创建IP',
  `upd_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `upd_user` varchar(32) DEFAULT NULL COMMENT '更新用户ID',
  `upd_name` varchar(255) DEFAULT NULL COMMENT '更新用户',
  `upd_host` varchar(255) DEFAULT NULL COMMENT '更新IP',
  `deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='STORE-库';

-- ----------------------------
-- Table structure for disk_store_bucket_user
-- ----------------------------
DROP TABLE IF EXISTS `disk_store_bucket_user`;
CREATE TABLE `disk_store_bucket_user` (
  `id` int(11) unsigned NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `bucket_id` int(11) NOT NULL COMMENT '库ID',
  `user_id` varchar(32) NOT NULL COMMENT '用户ID',
  `type` tinyint(4) NOT NULL COMMENT '类型1-创建者/2-操作者',
  `crt_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '创建时间',
  `crt_user` varchar(32) NOT NULL COMMENT '创建用户ID',
  `crt_name` varchar(255) NOT NULL COMMENT '创建用户',
  `crt_host` varchar(255) DEFAULT NULL COMMENT '创建IP',
  `upd_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `upd_user` varchar(32) DEFAULT NULL COMMENT '更新用户ID',
  `upd_name` varchar(255) DEFAULT NULL COMMENT '更新用户',
  `upd_host` varchar(255) DEFAULT NULL COMMENT '更新IP',
  `deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='STORE-库-人员关联';

-- ----------------------------
-- Table structure for disk_store_file
-- ----------------------------
DROP TABLE IF EXISTS `disk_store_file`;
CREATE TABLE `disk_store_file` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `bucket_id` int(11) NOT NULL COMMENT '库ID',
  `name` varchar(255) NOT NULL COMMENT '文件夹名称',
  `parent_id` int(11) NOT NULL COMMENT '父级节点',
  `sort` int(11) DEFAULT NULL COMMENT '排序ID',
  `dir` tinyint(1) DEFAULT NULL COMMENT '是否文件夹',
  `size` int(11) unsigned DEFAULT NULL COMMENT '文件大小(目录下文件数量/KB)',
  `type` varchar(255) DEFAULT NULL COMMENT '文件类型',
  `file_id` varchar(64) DEFAULT NULL COMMENT '文件ID',
  `full_path` varchar(1000) DEFAULT NULL COMMENT '完整路径#dir#,#dir#',
  `tags` json DEFAULT NULL COMMENT '标签',
  `info` varchar(255) DEFAULT NULL COMMENT '文件信息',
  `delete_action` tinyint(1) DEFAULT NULL COMMENT '是否有删除动作',
  `crt_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '创建时间',
  `crt_user` varchar(32) NOT NULL COMMENT '创建用户ID',
  `crt_name` varchar(255) NOT NULL COMMENT '创建用户',
  `crt_host` varchar(255) DEFAULT NULL COMMENT '创建IP',
  `upd_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `upd_user` varchar(32) DEFAULT NULL COMMENT '更新用户ID',
  `upd_name` varchar(255) DEFAULT NULL COMMENT '更新用户',
  `upd_host` varchar(255) DEFAULT NULL COMMENT '更新IP',
  `deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='STORE-文件夹';

-- ----------------------------
-- Table structure for disk_store_file_tag
-- ----------------------------
DROP TABLE IF EXISTS `disk_store_file_tag`;
CREATE TABLE `disk_store_file_tag` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `file_id` int(11) NOT NULL COMMENT '文件ID',
  `tag_id` int(11) NOT NULL COMMENT '标签ID',
  `crt_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '创建时间',
  `crt_user` varchar(32) NOT NULL COMMENT '创建用户ID',
  `crt_name` varchar(255) NOT NULL COMMENT '创建用户',
  `crt_host` varchar(255) DEFAULT NULL COMMENT '创建IP',
  `upd_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `upd_user` varchar(32) DEFAULT NULL COMMENT '更新用户ID',
  `upd_name` varchar(255) DEFAULT NULL COMMENT '更新用户',
  `upd_host` varchar(255) DEFAULT NULL COMMENT '更新IP',
  `deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='STORE-文件-标签';

-- ----------------------------
-- Table structure for disk_store_tag
-- ----------------------------
DROP TABLE IF EXISTS `disk_store_tag`;
CREATE TABLE `disk_store_tag` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `bucket_id` int(11) NOT NULL COMMENT '库ID',
  `parent_id` int(11) NOT NULL COMMENT '父ID',
  `name` varchar(255) NOT NULL COMMENT '名称',
  `sort` int(11) NOT NULL COMMENT '排序ID',
  `color` varchar(10) NOT NULL COMMENT '颜色',
  `crt_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '创建时间',
  `crt_user` varchar(32) NOT NULL COMMENT '创建用户ID',
  `crt_name` varchar(255) NOT NULL COMMENT '创建用户',
  `crt_host` varchar(255) DEFAULT NULL COMMENT '创建IP',
  `upd_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `upd_user` varchar(32) DEFAULT NULL COMMENT '更新用户ID',
  `upd_name` varchar(255) DEFAULT NULL COMMENT '更新用户',
  `upd_host` varchar(255) DEFAULT NULL COMMENT '更新IP',
  `deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='STORE-标签';

SET FOREIGN_KEY_CHECKS = 1;