-- ------------------------- info -------------------------
-- @@ver: 1_000_005
-- @@info: 扩大文件标签颜色字段长度
-- ------------------------- info -------------------------

ALTER TABLE "disk_store_tag"
    ALTER COLUMN "color" TYPE varchar(32);
