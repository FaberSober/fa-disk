-- ------------------------- info -------------------------
-- @@ver: 1_000_004
-- @@info: 优化文件目录、路径和回收站查询索引
-- ------------------------- info -------------------------

CREATE INDEX IF NOT EXISTS "idx_disk_store_file_query"
    ON "disk_store_file" ("bucket_id", "deleted", "parent_id", "dir", "sort", "name");
CREATE INDEX IF NOT EXISTS "idx_disk_store_file_path"
    ON "disk_store_file" ("bucket_id", "deleted", "full_path");
CREATE INDEX IF NOT EXISTS "idx_disk_store_file_trash"
    ON "disk_store_file" ("bucket_id", "deleted", "delete_action", "upd_time");
