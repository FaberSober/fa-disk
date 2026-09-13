-- ------------------------- info -------------------------
-- @@ver: 1_000_002
-- @@info: 文档增加历史记录信息
-- ------------------------- info -------------------------

ALTER TABLE "disk_store_file_his" ADD COLUMN "change_file_id" varchar(32) NULL DEFAULT NULL;
COMMENT ON COLUMN "disk_store_file_his"."change_file_id" IS 'Office文件变更内容zip包文件ID（适用于onlyoffice）';

ALTER TABLE "disk_store_file_his" ADD COLUMN "ver" integer NOT NULL;
COMMENT ON COLUMN "disk_store_file_his"."ver" IS '版本号';

