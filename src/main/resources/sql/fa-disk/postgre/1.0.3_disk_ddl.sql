-- ------------------------- info -------------------------
-- @@ver: 1_000_003
-- @@info: 修复crt_time字段更新自动更新
-- ------------------------- info -------------------------

ALTER TABLE "disk_store_bucket" ALTER COLUMN "crt_time" SET DATA TYPE timestamp;
ALTER TABLE "disk_store_bucket" ALTER COLUMN "crt_time" SET NOT NULL;
ALTER TABLE "disk_store_bucket" ALTER COLUMN "crt_time" SET DEFAULT CURRENT_TIMESTAMP;
COMMENT ON COLUMN "disk_store_bucket"."crt_time" IS '创建时间';
ALTER TABLE "disk_store_bucket_user" ALTER COLUMN "crt_time" SET DATA TYPE timestamp;
ALTER TABLE "disk_store_bucket_user" ALTER COLUMN "crt_time" SET NOT NULL;
ALTER TABLE "disk_store_bucket_user" ALTER COLUMN "crt_time" SET DEFAULT CURRENT_TIMESTAMP;
COMMENT ON COLUMN "disk_store_bucket_user"."crt_time" IS '创建时间';
ALTER TABLE "disk_store_file" ALTER COLUMN "crt_time" SET DATA TYPE timestamp;
ALTER TABLE "disk_store_file" ALTER COLUMN "crt_time" SET NOT NULL;
ALTER TABLE "disk_store_file" ALTER COLUMN "crt_time" SET DEFAULT CURRENT_TIMESTAMP;
COMMENT ON COLUMN "disk_store_file"."crt_time" IS '创建时间';
ALTER TABLE "disk_store_file_his" ALTER COLUMN "crt_time" SET DATA TYPE timestamp;
ALTER TABLE "disk_store_file_his" ALTER COLUMN "crt_time" SET NOT NULL;
ALTER TABLE "disk_store_file_his" ALTER COLUMN "crt_time" SET DEFAULT CURRENT_TIMESTAMP;
COMMENT ON COLUMN "disk_store_file_his"."crt_time" IS '创建时间';
ALTER TABLE "disk_store_file_tag" ALTER COLUMN "crt_time" SET DATA TYPE timestamp;
ALTER TABLE "disk_store_file_tag" ALTER COLUMN "crt_time" SET NOT NULL;
ALTER TABLE "disk_store_file_tag" ALTER COLUMN "crt_time" SET DEFAULT CURRENT_TIMESTAMP;
COMMENT ON COLUMN "disk_store_file_tag"."crt_time" IS '创建时间';
ALTER TABLE "disk_store_tag" ALTER COLUMN "crt_time" SET DATA TYPE timestamp;
ALTER TABLE "disk_store_tag" ALTER COLUMN "crt_time" SET NOT NULL;
ALTER TABLE "disk_store_tag" ALTER COLUMN "crt_time" SET DEFAULT CURRENT_TIMESTAMP;
COMMENT ON COLUMN "disk_store_tag"."crt_time" IS '创建时间';

