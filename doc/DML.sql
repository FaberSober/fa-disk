-- disk_store_file add info
ALTER TABLE `file_web`.`disk_store_file` ADD COLUMN `info` varchar(255) NULL COMMENT '文件信息' AFTER `tags`;
