package com.faber.api.disk.store.biz;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.faber.api.disk.store.entity.StoreFile;
import com.faber.api.disk.store.entity.StoreFileHis;
import com.faber.api.disk.store.mapper.StoreFileHisMapper;
import com.faber.core.exception.BuzzException;
import com.faber.core.vo.query.QueryParams;
import com.faber.core.web.biz.BaseBiz;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.io.Serializable;
import java.util.Collection;
import java.util.List;

/**
 * STORE-文件-历史记录
 *
 * @author xu.pengfei
 * @email faberxu@gmail.com
 * @date 2023-03-15 16:31:06
 */
@Service
public class StoreFileHisBiz extends BaseBiz<StoreFileHisMapper,StoreFileHis> {

    @Lazy
    @Resource
    StoreFileBiz storeFileBiz;

    @Override
    protected void preProcessQuery(QueryParams query) {
        // ponytail: use an ID scope for related-table filtering; replace with an EXISTS join if file volume makes IN lists too large.
        query.getQuery().put("storeFileId#$in", storeFileBiz.getAccessibleFileIds());
    }

    @Override
    public List<StoreFileHis> list() {
        QueryWrapper<StoreFileHis> wrapper = new QueryWrapper<>();
        wrapper.in("store_file_id", storeFileBiz.getAccessibleFileIds());
        return super.list(wrapper);
    }

    @Override
    public StoreFileHis getById(Serializable id) {
        StoreFileHis history = super.getById(id);
        if (history != null) {
            storeFileBiz.requireAccessible(history.getStoreFileId(), true);
        }
        return history;
    }

    @Override
    public StoreFileHis getDetailById(Serializable id) {
        return getById(id);
    }

    @Override
    public <ID extends Serializable> List<StoreFileHis> getByIds(List<ID> ids) {
        if (ids != null) ids.forEach(id -> {
            StoreFileHis history = super.getById(id);
            if (history != null) storeFileBiz.requireAccessible(history.getStoreFileId(), true);
        });
        return super.getByIds(ids);
    }

    @Override
    public boolean save(StoreFileHis entity) {
        if (entity == null || entity.getStoreFileId() == null) {
            throw new BuzzException("存储文件ID不能为空");
        }
        storeFileBiz.requireAccessible(entity.getStoreFileId(), true);
        return super.save(entity);
    }

    @Override
    public boolean updateById(StoreFileHis entity) {
        StoreFileHis current = super.getById(entity == null ? null : entity.getId());
        if (current == null) throw new BuzzException("文件历史不存在");
        storeFileBiz.requireAccessible(current.getStoreFileId(), true);
        if (entity.getStoreFileId() != null && !entity.getStoreFileId().equals(current.getStoreFileId())) {
            throw new BuzzException("文件历史不能跨文件修改");
        }
        entity.setStoreFileId(current.getStoreFileId());
        return super.updateById(entity);
    }

    public Integer getStoreFileMaxVer(Integer storeFileId) {
        QueryWrapper<StoreFileHis> wrapper = new QueryWrapper<>();
        wrapper.eq("store_file_id", storeFileId);
        return getMaxSort(wrapper, "ver");
    }

    public void saveSnapshot(StoreFile storeFile) {
        StoreFileHis storeFileHis = new StoreFileHis();
        storeFileHis.setStoreFileId(storeFile.getId());
        storeFileHis.setFileSaveId(storeFile.getFileId());
        storeFileHis.setFileName(storeFile.getName());
        storeFileHis.setVer(1);
        super.save(storeFileHis);
    }

    @Override
    public boolean removeById(Serializable id) {
        StoreFileHis history = super.getById(id);
        if (history == null) return false;
        storeFileBiz.requireAccessible(history.getStoreFileId(), true);
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
        StoreFileHis history = baseMapper.selectByIdIgnoreLogic(id);
        if (history == null) return;
        storeFileBiz.requireAccessible(history.getStoreFileId(), true);
        super.removePerById(id);
    }

    @Override
    public void removeByQuery(QueryParams query) {
        list(query).forEach(history -> removeById(history.getId()));
    }

    @Override
    public boolean saveBatch(Collection<StoreFileHis> entityList) {
        if (entityList == null) return true;
        entityList.forEach(this::save);
        return true;
    }

    @Override
    public boolean updateBatchById(Collection<StoreFileHis> entityList) {
        if (entityList == null) return true;
        entityList.forEach(this::updateById);
        return true;
    }

    @Override
    public boolean saveOrUpdate(StoreFileHis entity) {
        return entity.getId() == null ? save(entity) : updateById(entity);
    }

    @Override
    public boolean saveOrUpdateBatch(Collection<StoreFileHis> entityList) {
        if (entityList == null) return true;
        entityList.forEach(this::saveOrUpdate);
        return true;
    }

}
