-- ------------------------- info -------------------------
-- @@ver: 1_000_006
-- @@info: 增加网盘业务表租户隔离
-- ------------------------- info -------------------------

-- 若升级时尚无启用租户，历史行会保留 NULL；创建默认租户后须先完成归属回填再开放普通租户访问。

ALTER TABLE "disk_store_bucket" ADD COLUMN IF NOT EXISTS "tenant_id" varchar(32) DEFAULT NULL;
COMMENT ON COLUMN "disk_store_bucket"."tenant_id" IS '租户ID';
ALTER TABLE "disk_store_bucket_user" ADD COLUMN IF NOT EXISTS "tenant_id" varchar(32) DEFAULT NULL;
COMMENT ON COLUMN "disk_store_bucket_user"."tenant_id" IS '租户ID';
ALTER TABLE "disk_store_file" ADD COLUMN IF NOT EXISTS "tenant_id" varchar(32) DEFAULT NULL;
COMMENT ON COLUMN "disk_store_file"."tenant_id" IS '租户ID';
ALTER TABLE "disk_store_tag" ADD COLUMN IF NOT EXISTS "tenant_id" varchar(32) DEFAULT NULL;
COMMENT ON COLUMN "disk_store_tag"."tenant_id" IS '租户ID';
ALTER TABLE "disk_store_file_tag" ADD COLUMN IF NOT EXISTS "tenant_id" varchar(32) DEFAULT NULL;
COMMENT ON COLUMN "disk_store_file_tag"."tenant_id" IS '租户ID';
ALTER TABLE "disk_store_file_his" ADD COLUMN IF NOT EXISTS "tenant_id" varchar(32) DEFAULT NULL;
COMMENT ON COLUMN "disk_store_file_his"."tenant_id" IS '租户ID';

-- 老文件库归属到创建者默认租户；若创建者没有租户关联，则回退到平台排序最前的启用租户。
UPDATE "disk_store_bucket" b
SET "tenant_id" = COALESCE(
    (SELECT tu."tenant_id"
     FROM "tn_tenant_user" tu
     INNER JOIN "tn_tenant" t ON t."id" = tu."tenant_id"
     WHERE tu."user_id" = b."crt_user" AND tu."status" = true AND tu."deleted" = false
       AND t."status" = true AND t."deleted" = false
       AND (t."expire_time" IS NULL OR t."expire_time" > CURRENT_TIMESTAMP)
     ORDER BY tu."sort", tu."id" LIMIT 1),
    (SELECT t."id" FROM "tn_tenant" t
     WHERE t."status" = true AND t."deleted" = false
       AND (t."expire_time" IS NULL OR t."expire_time" > CURRENT_TIMESTAMP)
     ORDER BY t."sort", t."id" LIMIT 1)
)
WHERE b."tenant_id" IS NULL;

-- 关联数据跟随所属文件库/文件，避免因记录创建人不同而拆散同一业务关系。
UPDATE "disk_store_bucket_user" u
SET "tenant_id" = COALESCE(
    (SELECT b."tenant_id" FROM "disk_store_bucket" b WHERE b."id" = u."bucket_id"),
    (SELECT t."id" FROM "tn_tenant" t WHERE t."status" = true AND t."deleted" = false
     AND (t."expire_time" IS NULL OR t."expire_time" > CURRENT_TIMESTAMP) ORDER BY t."sort", t."id" LIMIT 1)
)
WHERE u."tenant_id" IS NULL;

UPDATE "disk_store_file" f
SET "tenant_id" = COALESCE(
    (SELECT b."tenant_id" FROM "disk_store_bucket" b WHERE b."id" = f."bucket_id"),
    (SELECT t."id" FROM "tn_tenant" t WHERE t."status" = true AND t."deleted" = false
     AND (t."expire_time" IS NULL OR t."expire_time" > CURRENT_TIMESTAMP) ORDER BY t."sort", t."id" LIMIT 1)
)
WHERE f."tenant_id" IS NULL;

UPDATE "disk_store_tag" tag
SET "tenant_id" = COALESCE(
    (SELECT b."tenant_id" FROM "disk_store_bucket" b WHERE b."id" = tag."bucket_id"),
    (SELECT t."id" FROM "tn_tenant" t WHERE t."status" = true AND t."deleted" = false
     AND (t."expire_time" IS NULL OR t."expire_time" > CURRENT_TIMESTAMP) ORDER BY t."sort", t."id" LIMIT 1)
)
WHERE tag."tenant_id" IS NULL;

UPDATE "disk_store_file_tag" ft
SET "tenant_id" = COALESCE(
    (SELECT f."tenant_id" FROM "disk_store_file" f WHERE f."id" = ft."file_id"),
    (SELECT t."id" FROM "tn_tenant" t WHERE t."status" = true AND t."deleted" = false
     AND (t."expire_time" IS NULL OR t."expire_time" > CURRENT_TIMESTAMP) ORDER BY t."sort", t."id" LIMIT 1)
)
WHERE ft."tenant_id" IS NULL;

UPDATE "disk_store_file_his" h
SET "tenant_id" = COALESCE(
    (SELECT f."tenant_id" FROM "disk_store_file" f WHERE f."id" = h."store_file_id"),
    (SELECT t."id" FROM "tn_tenant" t WHERE t."status" = true AND t."deleted" = false
     AND (t."expire_time" IS NULL OR t."expire_time" > CURRENT_TIMESTAMP) ORDER BY t."sort", t."id" LIMIT 1)
)
WHERE h."tenant_id" IS NULL;

CREATE INDEX IF NOT EXISTS "idx_disk_store_bucket_tenant" ON "disk_store_bucket" ("tenant_id");
CREATE INDEX IF NOT EXISTS "idx_disk_store_bucket_user_tenant" ON "disk_store_bucket_user" ("tenant_id", "bucket_id", "user_id");
CREATE INDEX IF NOT EXISTS "idx_disk_store_file_tenant_parent" ON "disk_store_file" ("tenant_id", "bucket_id", "parent_id");
CREATE INDEX IF NOT EXISTS "idx_disk_store_tag_tenant_parent" ON "disk_store_tag" ("tenant_id", "bucket_id", "parent_id");
CREATE INDEX IF NOT EXISTS "idx_disk_store_file_tag_tenant_file" ON "disk_store_file_tag" ("tenant_id", "file_id", "tag_id");
CREATE INDEX IF NOT EXISTS "idx_disk_store_file_his_tenant_file" ON "disk_store_file_his" ("tenant_id", "store_file_id", "ver");
