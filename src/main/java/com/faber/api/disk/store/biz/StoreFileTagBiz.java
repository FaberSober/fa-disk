package com.faber.api.disk.store.biz;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import com.faber.api.disk.store.entity.StoreFileTag;
import com.faber.api.disk.store.entity.StoreFile;
import com.faber.api.disk.store.entity.StoreTag;
import com.faber.api.disk.store.mapper.StoreFileTagMapper;
import com.faber.core.exception.BuzzException;
import com.faber.core.vo.query.QueryParams;
import com.faber.core.web.biz.BaseBiz;

import jakarta.annotation.Resource;
import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * STORE-文件-标签
 *
 * @author Farando
 * @email faberxu@gmail.com
 * @date 2022-12-27 11:25:19
 */
@Service
public class StoreFileTagBiz extends BaseBiz<StoreFileTagMapper, StoreFileTag> {

    @Lazy
    @Resource
    StoreFileBiz storeFileBiz;

    @Resource
    StoreTagBiz storeTagBiz;

    @Override
    protected void preProcessQuery(QueryParams query) {
        query.getQuery().put("fileId#$in", storeFileBiz.getAccessibleFileIds());
    }

    private StoreFileTag requireAccessible(Serializable id) {
        StoreFileTag link = baseMapper.selectById(id);
        if (link == null) throw new BuzzException("文件标签关联不存在");
        validateRelation(link);
        return link;
    }

    private void validateRelation(StoreFileTag link) {
        StoreFile file = storeFileBiz.requireAccessible(link.getFileId());
        StoreTag tag = storeTagBiz.requireAccessible(link.getTagId());
        if (!Objects.equals(file.getBucketId(), tag.getBucketId())) {
            throw new BuzzException("文件标签不能跨库关联");
        }
    }

    @Override
    public List<StoreFileTag> list() {
        QueryWrapper<StoreFileTag> wrapper = new QueryWrapper<>();
        wrapper.in("file_id", storeFileBiz.getAccessibleFileIds());
        return super.list(wrapper);
    }

    @Override
    public StoreFileTag getById(Serializable id) {
        return requireAccessible(id);
    }

    @Override
    public StoreFileTag getDetailById(Serializable id) {
        return getById(id);
    }

    @Override
    public <ID extends Serializable> List<StoreFileTag> getByIds(List<ID> ids) {
        if (ids != null) ids.forEach(this::requireAccessible);
        return super.getByIds(ids);
    }

    @Override
    public boolean save(StoreFileTag entity) {
        if (entity == null || entity.getFileId() == null || entity.getTagId() == null) {
            throw new BuzzException("文件和标签不能为空");
        }
        validateRelation(entity);
        return super.save(entity);
    }

    @Override
    public boolean updateById(StoreFileTag entity) {
        StoreFileTag current = requireAccessible(entity == null ? null : entity.getId());
        if (entity.getFileId() == null) entity.setFileId(current.getFileId());
        if (entity.getTagId() == null) entity.setTagId(current.getTagId());
        validateRelation(entity);
        entity.setId(current.getId());
        return super.updateById(entity);
    }

    @Override
    public boolean removeById(Serializable id) {
        StoreFileTag storeFileTag = requireAccessible(id);
        super.removeById(id);

        storeFileBiz.syncFileTags(storeFileTag.getFileId());
        return true;
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
        StoreFileTag link = baseMapper.selectByIdIgnoreLogic(id);
        if (link == null) return;
        requireAccessible(id);
        super.removePerById(id);
        storeFileBiz.syncFileTags(link.getFileId());
    }

    @Override
    public void removeByQuery(QueryParams query) {
        List<StoreFileTag> links = list(query);
        links.forEach(link -> removeById(link.getId()));
    }

    @Override
    public boolean saveBatch(Collection<StoreFileTag> entityList) {
        if (entityList == null) return true;
        entityList.forEach(this::save);
        return true;
    }

    @Override
    public boolean updateBatchById(Collection<StoreFileTag> entityList) {
        if (entityList == null) return true;
        entityList.forEach(this::updateById);
        return true;
    }

    @Override
    public boolean saveOrUpdate(StoreFileTag entity) {
        return entity.getId() == null ? save(entity) : updateById(entity);
    }

    @Override
    public boolean saveOrUpdateBatch(Collection<StoreFileTag> entityList) {
        if (entityList == null) return true;
        entityList.forEach(this::saveOrUpdate);
        return true;
    }

}
