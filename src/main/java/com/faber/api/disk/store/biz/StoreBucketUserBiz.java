package com.faber.api.disk.store.biz;

import com.faber.api.base.admin.biz.UserBiz;
import com.faber.api.disk.store.entity.StoreBucket;
import com.faber.api.disk.store.entity.StoreBucketUser;
import com.faber.api.disk.store.enums.StoreBucketUserTypeEnum;
import com.faber.api.disk.store.mapper.StoreBucketMapper;
import com.faber.api.disk.store.mapper.StoreBucketUserMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.faber.core.exception.BuzzException;
import com.faber.core.exception.auth.UserNoPermissionException;
import com.faber.core.vo.query.QueryParams;
import com.faber.core.web.biz.BaseBiz;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * STORE-库-人员关联
 *
 * @author Farando
 * @email faberxu@gmail.com
 * @date 2022-12-28 11:14:56
 */
@Service
public class StoreBucketUserBiz extends BaseBiz<StoreBucketUserMapper,StoreBucketUser> {

    @Resource
    UserBiz userBiz;

    @Resource
    StoreBucketMapper storeBucketMapper;

    /** 当前用户可访问的文件库 ID。超级管理员可访问所有未删除文件库。 */
    public List<Integer> getAccessibleBucketIds() {
        List<Integer> ids;
        if (isSuperAdminUser(getCurrentUserId())) {
            ids = storeBucketMapper.selectList(null).stream()
                    .map(StoreBucket::getId)
                    .collect(Collectors.toList());
        } else {
            ids = lambdaQuery()
                    .eq(StoreBucketUser::getUserId, getCurrentUserId())
                    .select(StoreBucketUser::getBucketId)
                    .list()
                    .stream()
                    .map(StoreBucketUser::getBucketId)
                    .distinct()
                    .collect(Collectors.toList());
        }
        return ids.isEmpty() ? Collections.singletonList(0) : ids;
    }

    public StoreBucket requireAccessible(Integer bucketId) {
        if (bucketId == null) {
            throw new BuzzException("文件库ID不能为空");
        }

        StoreBucket bucket = storeBucketMapper.selectById(bucketId);
        if (bucket == null) {
            throw new BuzzException("文件库不存在");
        }
        if (!isSuperAdminUser(getCurrentUserId()) && lambdaQuery()
                .eq(StoreBucketUser::getBucketId, bucketId)
                .eq(StoreBucketUser::getUserId, getCurrentUserId())
                .count() == 0) {
            throw new UserNoPermissionException("无权访问该文件库");
        }
        return bucket;
    }

    /** 仅文件库创建者或超级管理员可以维护库和库成员。 */
    public void requireManage(Integer bucketId) {
        requireAccessible(bucketId);
        if (isSuperAdminUser(getCurrentUserId())) {
            return;
        }

        boolean creator = lambdaQuery()
                .eq(StoreBucketUser::getBucketId, bucketId)
                .eq(StoreBucketUser::getUserId, getCurrentUserId())
                .eq(StoreBucketUser::getType, StoreBucketUserTypeEnum.CREATOR)
                .count() > 0;
        if (!creator) {
            throw new UserNoPermissionException("无权管理该文件库");
        }
    }

    @Override
    public QueryWrapper<StoreBucketUser> parseQuery(QueryParams query) {
        QueryWrapper<StoreBucketUser> wrapper = super.parseQuery(query);
        wrapper.in("bucket_id", getAccessibleBucketIds());
        return wrapper;
    }

    @Override
    public List<StoreBucketUser> list() {
        QueryWrapper<StoreBucketUser> wrapper = new QueryWrapper<>();
        wrapper.in("bucket_id", getAccessibleBucketIds());
        return super.list(wrapper);
    }

    @Override
    public StoreBucketUser getById(Serializable id) {
        StoreBucketUser link = super.getById(id);
        if (link != null) {
            requireAccessible(link.getBucketId());
        }
        return link;
    }

    @Override
    public StoreBucketUser getDetailById(Serializable id) {
        StoreBucketUser link = getById(id);
        if (link != null) {
            decorateOne(link);
        }
        return link;
    }

    @Override
    public <ID extends Serializable> List<StoreBucketUser> getByIds(List<ID> ids) {
        if (ids != null) {
            ids.forEach(id -> {
                StoreBucketUser link = super.getById(id);
                if (link != null) {
                    requireAccessible(link.getBucketId());
                }
            });
        }
        return super.getByIds(ids);
    }

    @Override
    public boolean save(StoreBucketUser entity) {
        if (entity == null || entity.getBucketId() == null || entity.getUserId() == null) {
            throw new BuzzException("文件库成员参数不能为空");
        }

        // 文件库创建时由 StoreBucketBiz 写入创建者关联。
        if (StoreBucketUserTypeEnum.CREATOR.equals(entity.getType())
                && Objects.equals(entity.getUserId(), getCurrentUserId())) {
            StoreBucket bucket = storeBucketMapper.selectById(entity.getBucketId());
            if (bucket != null && Objects.equals(bucket.getCrtUser(), getCurrentUserId())) {
                return super.save(entity);
            }
        }

        requireManage(entity.getBucketId());
        entity.setType(StoreBucketUserTypeEnum.USER);
        return super.save(entity);
    }

    @Override
    public boolean updateById(StoreBucketUser entity) {
        StoreBucketUser current = super.getById(entity.getId());
        if (current == null) {
            throw new BuzzException("文件库成员不存在");
        }
        requireManage(current.getBucketId());
        if (entity.getBucketId() != null && !Objects.equals(entity.getBucketId(), current.getBucketId())) {
            throw new BuzzException("文件库成员不能跨库修改");
        }
        entity.setBucketId(current.getBucketId());
        entity.setUserId(current.getUserId());
        entity.setType(current.getType());
        return super.updateById(entity);
    }

    @Override
    public void decorateOne(StoreBucketUser i) {
        i.setUser(userBiz.getByIdWithCache(i.getUserId()));
    }

    public void updateBucketUser(List<StoreBucketUser> list) {
        if (list == null || list.isEmpty()) return;
        for (StoreBucketUser link : list) {
            requireManage(link.getBucketId());
            if (link.getUserId() == null) {
                throw new BuzzException("用户ID不能为空");
            }
            long count = lambdaQuery()
                    .eq(StoreBucketUser::getBucketId, link.getBucketId())
                    .eq(StoreBucketUser::getUserId, link.getUserId())
                    .count();
            if (count == 1) continue;

            if (count > 0) {
                lambdaUpdate()
                        .eq(StoreBucketUser::getBucketId, link.getBucketId())
                        .eq(StoreBucketUser::getUserId, link.getUserId())
                        .remove();
            }

            link.setId(null);
            link.setType(StoreBucketUserTypeEnum.USER);
            super.save(link);
        }
    }

    @Override
    public boolean removeById(Serializable id) {
        StoreBucketUser link = super.getById(id);
        if (link == null) return false;
        requireManage(link.getBucketId());
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
        StoreBucketUser link = baseMapper.selectByIdIgnoreLogic(id);
        if (link == null) return;
        requireManage(link.getBucketId());
        super.removePerById(id);
    }

    @Override
    public void removeByQuery(QueryParams query) {
        List<StoreBucketUser> links = list(query);
        links.forEach(link -> requireManage(link.getBucketId()));
        super.removeByQuery(query);
    }

    @Override
    public boolean saveBatch(Collection<StoreBucketUser> entityList) {
        if (entityList == null) return true;
        entityList.forEach(this::save);
        return true;
    }

    @Override
    public boolean updateBatchById(Collection<StoreBucketUser> entityList) {
        if (entityList == null) return true;
        entityList.forEach(this::updateById);
        return true;
    }

    @Override
    public boolean saveOrUpdate(StoreBucketUser entity) {
        return entity.getId() == null ? save(entity) : updateById(entity);
    }

    @Override
    public boolean saveOrUpdateBatch(Collection<StoreBucketUser> entityList) {
        if (entityList == null) return true;
        entityList.forEach(this::saveOrUpdate);
        return true;
    }

}
