package com.faber.api.disk.store.biz;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.faber.api.disk.store.entity.StoreTag;
import com.faber.api.disk.store.mapper.StoreTagMapper;
import com.faber.core.exception.BuzzException;
import com.faber.core.vo.query.QueryParams;
import com.faber.core.web.biz.BaseTreeBiz;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * STORE-标签
 *
 * @author Farando
 * @email faberxu@gmail.com
 * @date 2022-12-27 11:25:19
 */
@Service
public class StoreTagBiz extends BaseTreeBiz<StoreTagMapper,StoreTag> {

    @Resource
    StoreBucketUserBiz storeBucketUserBiz;

    @Override
    protected void enhanceTreeQuery(QueryWrapper<StoreTag> wrapper) {
        wrapper.in("bucket_id", storeBucketUserBiz.getAccessibleBucketIds());
    }

    @Override
    public QueryWrapper<StoreTag> parseQuery(QueryParams query) {
        QueryWrapper<StoreTag> wrapper = super.parseQuery(query);
        wrapper.in("bucket_id", storeBucketUserBiz.getAccessibleBucketIds());
        return wrapper;
    }

    public StoreTag requireAccessible(Integer id) {
        if (id == null) throw new BuzzException("标签ID不能为空");
        StoreTag tag = super.getById(id);
        if (tag == null) throw new BuzzException("标签不存在");
        storeBucketUserBiz.requireAccessible(tag.getBucketId());
        return tag;
    }

    @Override
    public List<StoreTag> list() {
        QueryWrapper<StoreTag> wrapper = new QueryWrapper<>();
        wrapper.in("bucket_id", storeBucketUserBiz.getAccessibleBucketIds());
        return super.list(wrapper);
    }

    @Override
    public List<StoreTag> getAllChildrenFromNode(Serializable id) {
        requireAccessible(Integer.valueOf(id.toString()));
        return super.getAllChildrenFromNode(id);
    }

    @Override
    public StoreTag getById(Serializable id) {
        StoreTag tag = super.getById(id);
        if (tag != null) storeBucketUserBiz.requireAccessible(tag.getBucketId());
        return tag;
    }

    @Override
    public StoreTag getDetailById(Serializable id) {
        return getById(id);
    }

    @Override
    public <ID extends Serializable> List<StoreTag> getByIds(List<ID> ids) {
        if (ids != null) ids.forEach(id -> requireAccessible(Integer.valueOf(id.toString())));
        return super.getByIds(ids);
    }

    private void requireParent(Integer bucketId, Integer parentId) {
        if (parentId == null || parentId == 0) return;
        StoreTag parent = requireAccessible(parentId);
        if (!Objects.equals(bucketId, parent.getBucketId())) {
            throw new BuzzException("标签不能跨库挂载");
        }
    }

    @Override
    public boolean save(StoreTag entity) {
        if (entity == null || entity.getBucketId() == null) {
            throw new BuzzException("文件库ID不能为空");
        }
        storeBucketUserBiz.requireAccessible(entity.getBucketId());
        requireParent(entity.getBucketId(), entity.getParentId());
        return super.save(entity);
    }

    @Override
    public boolean updateById(StoreTag entity) {
        StoreTag current = requireAccessible(entity == null ? null : entity.getId());
        if (entity.getBucketId() == null) entity.setBucketId(current.getBucketId());
        if (!Objects.equals(entity.getBucketId(), current.getBucketId())) {
            throw new BuzzException("标签不能跨库修改");
        }
        if (entity.getParentId() == null) entity.setParentId(current.getParentId());
        requireParent(entity.getBucketId(), entity.getParentId());
        return super.updateById(entity);
    }

    @Override
    public boolean saveBatch(Collection<StoreTag> entityList) {
        if (entityList == null) return true;
        entityList.forEach(this::save);
        return true;
    }

    @Override
    public boolean updateBatchById(Collection<StoreTag> entityList) {
        if (entityList == null) return true;
        entityList.forEach(this::updateById);
        return true;
    }

    @Override
    public boolean saveOrUpdate(StoreTag entity) {
        return entity.getId() == null ? save(entity) : updateById(entity);
    }

    @Override
    public boolean saveOrUpdateBatch(Collection<StoreTag> entityList) {
        if (entityList == null) return true;
        entityList.forEach(this::saveOrUpdate);
        return true;
    }

    @Override
    public boolean removeById(Serializable id) {
        requireAccessible(Integer.valueOf(id.toString()));
        return super.removeById(id);
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
        StoreTag tag = baseMapper.selectByIdIgnoreLogic(id);
        if (tag == null) return;
        storeBucketUserBiz.requireAccessible(tag.getBucketId());
        super.removePerById(id);
    }

    @Override
    public void removeByQuery(QueryParams query) {
        list(query).forEach(tag -> storeBucketUserBiz.requireAccessible(tag.getBucketId()));
        super.removeByQuery(query);
    }
}
