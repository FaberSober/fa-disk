package com.faber.api.disk.store.biz;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import cn.hutool.core.util.StrUtil;
import com.faber.api.disk.store.entity.StoreBucket;
import com.faber.api.disk.store.entity.StoreBucketUser;
import com.faber.api.disk.store.entity.StoreFile;
import com.faber.api.disk.store.enums.StoreBucketUserTypeEnum;
import com.faber.api.disk.store.mapper.StoreBucketMapper;
import com.faber.core.exception.BuzzException;
import com.faber.core.vo.query.QueryParams;
import com.faber.core.web.biz.BaseBiz;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;
import java.io.Serializable;
import java.util.Collection;
import java.util.List;

/**
 * STORE-库
 *
 * @author Farando
 * @email faberxu@gmail.com
 * @date 2022-12-22 09:31:17
 */
@Service
@Transactional
public class StoreBucketBiz extends BaseBiz<StoreBucketMapper, StoreBucket> {

    @Resource
    StoreBucketUserBiz storeBucketUserBiz;

    @Lazy
    @Resource
    StoreFileBiz storeFileBiz;

    @Override
    public boolean save(StoreBucket entity) {
        if (entity == null || StrUtil.isBlank(entity.getName())) {
            throw new BuzzException("文件库名称不能为空");
        }
        if (!super.save(entity)) {
            return false;
        }

        // 添加创建者绑定关系
        StoreBucketUser link = new StoreBucketUser();
        link.setBucketId(entity.getId());
        link.setUserId(getCurrentUserId());
        link.setType(StoreBucketUserTypeEnum.CREATOR);
        if (!storeBucketUserBiz.save(link)) {
            throw new BuzzException("文件库创建者关联保存失败");
        }

        return true;
    }

    public List<Integer> getMyIds() {
        return storeBucketUserBiz.getAccessibleBucketIds();
    }

    public List<StoreBucket> getMyList() {
        List<Integer> linkBucketIds = this.getMyIds();

        return lambdaQuery().in(StoreBucket::getId, linkBucketIds).list();
    }

    public StoreBucket requireAccessible(Integer bucketId) {
        return storeBucketUserBiz.requireAccessible(bucketId);
    }

    public void requireManage(Integer bucketId) {
        storeBucketUserBiz.requireManage(bucketId);
    }

    @Override
    public QueryWrapper<StoreBucket> parseQuery(QueryParams query) {
        QueryWrapper<StoreBucket> wrapper = super.parseQuery(query);
        wrapper.in("id", getMyIds());
        return wrapper;
    }

    @Override
    public List<StoreBucket> list() {
        QueryWrapper<StoreBucket> wrapper = new QueryWrapper<>();
        wrapper.in("id", getMyIds());
        return super.list(wrapper);
    }

    @Override
    public StoreBucket getById(Serializable id) {
        StoreBucket bucket = super.getById(id);
        if (bucket != null) {
            requireAccessible(bucket.getId());
        }
        return bucket;
    }

    @Override
    public StoreBucket getDetailById(Serializable id) {
        return getById(id);
    }

    @Override
    public <ID extends Serializable> List<StoreBucket> getByIds(List<ID> ids) {
        if (ids != null) {
            ids.forEach(id -> requireAccessible(Integer.valueOf(id.toString())));
        }
        return super.getByIds(ids);
    }

    @Override
    public boolean updateById(StoreBucket entity) {
        if (entity == null || entity.getId() == null) {
            throw new BuzzException("文件库ID不能为空");
        }
        if (entity.getName() != null && StrUtil.isBlank(entity.getName())) {
            throw new BuzzException("文件库名称不能为空");
        }
        requireManage(entity.getId());
        return super.updateById(entity);
    }

    @Override
    public boolean saveOrUpdate(StoreBucket entity) {
        if (entity == null) {
            throw new BuzzException("文件库参数不能为空");
        }
        return entity.getId() == null ? save(entity) : updateById(entity);
    }

    @Override
    public boolean saveBatch(Collection<StoreBucket> entityList) {
        if (entityList == null) return true;
        entityList.forEach(this::save);
        return true;
    }

    @Override
    public boolean updateBatchById(Collection<StoreBucket> entityList) {
        if (entityList == null) return true;
        entityList.forEach(this::updateById);
        return true;
    }

    @Override
    public boolean saveOrUpdateBatch(Collection<StoreBucket> entityList) {
        if (entityList == null) return true;
        entityList.forEach(this::saveOrUpdate);
        return true;
    }

    @Override
    public boolean removeById(Serializable id) {
        Integer bucketId = requireBucketId(id);
        requireManage(bucketId);
        requireEmpty(bucketId);
        boolean removed = super.removeById(id);
        if (removed) {
            clearBucketUsers(bucketId);
        }
        return removed;
    }

    @Override
    public void removeBatchByIds(List<Serializable> ids) {
        if (ids == null) return;
        ids.forEach(this::removeById);
    }

    @Override
    public boolean removeBatchByIds(Collection<?> ids) {
        if (ids == null || ids.isEmpty()) return true;
        ids.forEach(id -> removeById((Serializable) id));
        return true;
    }

    @Override
    public void removePerById(Serializable id) {
        Integer bucketId = requireBucketId(id);
        requireManage(bucketId);
        requireEmpty(bucketId);
        super.removePerById(id);
        clearBucketUsers(bucketId);
    }

    @Override
    public void removeByQuery(QueryParams query) {
        List<Integer> bucketIds = list(query).stream().map(StoreBucket::getId).toList();
        bucketIds.forEach(bucketId -> {
            requireManage(bucketId);
            requireEmpty(bucketId);
        });
        super.removeByQuery(query);
        bucketIds.forEach(this::clearBucketUsers);
    }

    @Override
    public void removeMine() {
        lambdaQuery().eq(StoreBucket::getCrtUser, getCurrentUserId()).list()
                .forEach(bucket -> removeById(bucket.getId()));
    }

    private Integer requireBucketId(Serializable id) {
        if (id == null) {
            throw new BuzzException("文件库ID不能为空");
        }
        try {
            return Integer.valueOf(id.toString());
        } catch (NumberFormatException e) {
            throw new BuzzException("文件库ID格式错误");
        }
    }

    private void requireEmpty(Integer bucketId) {
        if (storeFileBiz.getBaseMapper().countByBucketId(bucketId) > 0) {
            throw new BuzzException("文件库存在文件，请先清空文件后再删除");
        }
    }

    private void clearBucketUsers(Integer bucketId) {
        storeBucketUserBiz.lambdaUpdate()
                .eq(StoreBucketUser::getBucketId, bucketId)
                .remove();
    }

    public void syncBucketSize() {
        List<StoreBucket> bucketList = lambdaQuery().list();

        for (StoreBucket bucket : bucketList) {
            Long size = storeFileBiz.getBaseMapper().sumFileSizeByBucketId(bucket.getId());
            bucket.setSize(size);

            Long dirCount = storeFileBiz.lambdaQuery()
                    .eq(StoreFile::getBucketId, bucket.getId())
                    .eq(StoreFile::getDir, true)
                    .count();
            bucket.setDirCount(dirCount);

            Long fileCount = storeFileBiz.lambdaQuery()
                    .eq(StoreFile::getBucketId, bucket.getId())
                    .eq(StoreFile::getDir, false)
                    .count();
            bucket.setFileCount(fileCount);

            this.updateById(bucket);
        }
    }

}
