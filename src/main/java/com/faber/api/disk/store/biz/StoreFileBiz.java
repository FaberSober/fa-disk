package com.faber.api.disk.store.biz;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.ZipUtil;
//import com.alicp.jetcache.anno.CacheInvalidate;
//import com.alicp.jetcache.anno.Cached;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.faber.api.base.admin.biz.FileSaveBiz;
import com.faber.api.base.admin.entity.FileSave;
import com.faber.api.disk.store.entity.StoreFile;
import com.faber.api.disk.store.entity.StoreFileTag;
import com.faber.api.disk.store.entity.StoreTag;
import com.faber.api.disk.store.mapper.StoreFileMapper;
import com.faber.api.disk.store.vo.req.StoreFileQueryVo;
import com.faber.api.disk.store.vo.req.StoreFilesAddTags;
import com.faber.api.disk.store.vo.req.StoreFilesMoveTo;
import com.faber.core.exception.BuzzException;
import com.faber.core.utils.FaFileUtils;
import com.faber.core.vo.msg.TableRet;
import com.faber.core.vo.query.BasePageQuery;
import com.faber.core.vo.query.QueryParams;
import com.faber.core.web.biz.BaseTreeBiz;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * STORE-文件
 *
 * @author Farando
 * @email faberxu@gmail.com
 * @date 2022-12-22 09:31:17
 */
@Service
public class StoreFileBiz extends BaseTreeBiz<StoreFileMapper, StoreFile> {

    public static final String ROOT_DIR_NAME = "全部文件";

    @Resource
    FileSaveBiz fileSaveBiz;

    @Resource
    StoreTagBiz storeTagBiz;

    @Lazy
    @Resource
    StoreFileTagBiz storeFileTagBiz;

    @Lazy
    @Resource
    StoreFileHisBiz storeFileHisBiz;

    @Resource
    StoreBucketUserBiz storeBucketUserBiz;

    @Override
    protected void enhanceTreeQuery(QueryWrapper<StoreFile> wrapper) {
        wrapper.in("bucket_id", storeBucketUserBiz.getAccessibleBucketIds());
        wrapper.eq("dir", true);
    }

    @Override
    public QueryWrapper<StoreFile> parseQuery(QueryParams query) {
        QueryWrapper<StoreFile> wrapper = super.parseQuery(query);
        wrapper.in("bucket_id", storeBucketUserBiz.getAccessibleBucketIds());
        return wrapper;
    }

    @Override
    public List<StoreFile> list() {
        QueryWrapper<StoreFile> wrapper = new QueryWrapper<>();
        wrapper.in("bucket_id", storeBucketUserBiz.getAccessibleBucketIds());
        return super.list(wrapper);
    }

    @Override
    public List<StoreFile> getAllChildrenFromNode(Serializable id) {
        requireAccessible(Integer.valueOf(id.toString()));
        return super.getAllChildrenFromNode(id);
    }

    public StoreFile requireAccessible(Integer id) {
        return requireAccessible(id, false);
    }

    public StoreFile requireAccessible(Integer id, boolean includeDeleted) {
        if (id == null) {
            throw new BuzzException("文件ID不能为空");
        }
        StoreFile file = includeDeleted ? baseMapper.selectByIdIgnoreLogic(id) : super.getById(id);
        if (file == null) {
            throw new BuzzException("文件不存在");
        }
        storeBucketUserBiz.requireAccessible(file.getBucketId());
        return file;
    }

    private StoreFile requireTrash(Integer id) {
        StoreFile file = requireAccessible(id, true);
        if (baseMapper.countDeletedById(id) == 0) {
            throw new BuzzException("文件不在回收站");
        }
        return file;
    }

    private Integer parseFileId(Serializable id) {
        if (id == null) {
            throw new BuzzException("文件ID不能为空");
        }
        try {
            return Integer.valueOf(id.toString());
        } catch (NumberFormatException e) {
            throw new BuzzException("文件ID格式错误");
        }
    }

    public List<Integer> getAccessibleFileIds() {
        List<Integer> ids = lambdaQuery()
                .select(StoreFile::getId)
                .in(StoreFile::getBucketId, storeBucketUserBiz.getAccessibleBucketIds())
                .list()
                .stream()
                .map(StoreFile::getId)
                .collect(Collectors.toList());
        return ids.isEmpty() ? List.of(0) : ids;
    }

    private void requireParent(Integer bucketId, Integer parentId) {
        if (parentId == null || parentId == 0) return;
        StoreFile parent = requireAccessible(parentId);
        if (!parent.getDir() || !Objects.equals(parent.getBucketId(), bucketId)) {
            throw new BuzzException("目标文件夹不存在，请确认");
        }
    }

    private List<StoreFile> getActiveRoots(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new BuzzException("未选择文件");
        }
        Map<Integer, StoreFile> selected = new LinkedHashMap<>();
        for (Integer id : ids) {
            selected.put(id, requireAccessible(id));
        }
        Set<Integer> selectedIds = new LinkedHashSet<>(selected.keySet());
        return selected.values().stream()
                .filter(file -> !hasSelectedAncestor(file, selectedIds, selected))
                .collect(Collectors.toList());
    }

    private void requireOperationTarget(List<StoreFile> files, Integer toDirId) {
        if (toDirId == null) {
            throw new BuzzException("目标文件夹不能为空");
        }
        Integer bucketId = files.get(0).getBucketId();
        for (StoreFile file : files) {
            if (!Objects.equals(bucketId, file.getBucketId())) {
                throw new BuzzException("不能跨库操作文件");
            }
        }
        requireParent(bucketId, toDirId);
        if (toDirId == 0) return;

        Set<Integer> sourceDirIds = files.stream()
                .filter(StoreFile::getDir)
                .map(StoreFile::getId)
                .collect(Collectors.toSet());
        StoreFile target = requireAccessible(toDirId);
        while (target != null && target.getId() > 0) {
            if (sourceDirIds.contains(target.getId())) {
                throw new BuzzException("目录不能移动或复制到自身及子目录");
            }
            Integer parentId = target.getParentId();
            target = parentId == null || parentId == 0 ? null : requireAccessible(parentId);
        }
    }

    private void requireNameAvailable(StoreFile file, Integer parentId, Integer ignoredId) {
        var query = lambdaQuery()
                .eq(StoreFile::getBucketId, file.getBucketId())
                .eq(StoreFile::getDir, file.getDir())
                .eq(StoreFile::getParentId, parentId)
                .eq(StoreFile::getName, file.getName());
        if (ignoredId != null) {
            query.ne(StoreFile::getId, ignoredId);
        }
        if (query.count() > 0) {
            throw new BuzzException("目标文件夹存在同名文件或目录");
        }
    }

    private void prepareQuery(BasePageQuery<StoreFileQueryVo> query, boolean deleted) {
        if (query == null || query.getQuery() == null) {
            throw new BuzzException("文件查询参数不能为空");
        }
        StoreFileQueryVo fileQuery = query.getQuery();
        fileQuery.setDeleted(deleted);
        fileQuery.setAccessibleBucketIds(storeBucketUserBiz.getAccessibleBucketIds());
        if (query.getSorter() == null || query.getSorter().isBlank()) {
            query.setSorter("dir DESC, sort ASC, name ASC, id ASC");
        }
        if (fileQuery.getBucketId() != null) {
            storeBucketUserBiz.requireAccessible(fileQuery.getBucketId());
        }
    }

//    @Cached(name = "store:file:fullpath:", key = "#id", expire = 3600)
    public List<StoreFile> getFullPath(Integer id) {
        return super.treePathLine(id);
    }

    public void syncFullPath(StoreFile entity) {
        StoreFile dirEntity = this.getById(entity.getParentId());
        if (dirEntity == null) {
            entity.setFullPath("#" + ROOT_DIR_NAME + "#");
        } else {
            entity.setFullPath(dirEntity.getFullPath() + ",#" + dirEntity.getName() + "#");
        }
    }

    public void syncDirSize(Integer dirId) {
        if (dirId == 0) return;
        long count = lambdaQuery()
                .eq(StoreFile::getParentId, dirId)
                .count();
        lambdaUpdate().eq(StoreFile::getId, dirId)
                .set(StoreFile::getSize, count)
                .update();
    }

    @Override
    public StoreFile getById(Serializable id) {
        StoreFile file = super.getById(id);
        if (file != null) {
            storeBucketUserBiz.requireAccessible(file.getBucketId());
        }
        return file;
    }

    @Override
    public StoreFile getDetailById(Serializable id) {
        StoreFile file = getById(id);
        if (file != null) {
            decorateOne(file);
        }
        return file;
    }

    @Override
    public <ID extends Serializable> List<StoreFile> getByIds(List<ID> ids) {
        if (ids != null) {
            ids.forEach(id -> requireAccessible(Integer.valueOf(id.toString())));
        }
        return super.getByIds(ids);
    }

    @Override
    public boolean save(StoreFile entity) {
        if (entity == null || entity.getBucketId() == null) {
            throw new BuzzException("文件库ID不能为空");
        }
        if (entity.getDir() == null) {
            throw new BuzzException("文件类型不能为空");
        }
        storeBucketUserBiz.requireAccessible(entity.getBucketId());
        if (entity.getParentId() == null) {
            entity.setParentId(0);
        }
        requireParent(entity.getBucketId(), entity.getParentId());

        // store file
        if (!entity.getDir()) { // 文件类型，存储文件
            FileSave fileSave = fileSaveBiz.getById(entity.getFileId());
            if (fileSave == null) {
                throw new BuzzException("上传文件不存在");
            }

            entity.setName(fileSave.getOriginalFilename());
            entity.setSize(fileSave.getSize());
            entity.setType(FileUtil.extName(fileSave.getOriginalFilename()));

            // judge same name file
            long count = lambdaQuery()
                    .eq(StoreFile::getBucketId, entity.getBucketId())
                    .eq(StoreFile::getDir, false)
                    .eq(StoreFile::getParentId, entity.getParentId())
                    .eq(StoreFile::getName, entity.getName())
                    .count();
            if (count > 0) {
                String now = DateUtil.format(new Date(), "yyyyMMddHHmmss");
                entity.setName(FaFileUtils.addSuffixToFileName(fileSave.getOriginalFilename(), "_(" + count + ")_" + now));
            }
        } else { // 文件夹，保存文件夹
            // judge same name dir
            long count = lambdaQuery()
                    .eq(StoreFile::getBucketId, entity.getBucketId())
                    .eq(StoreFile::getDir, true)
                    .eq(StoreFile::getParentId, entity.getParentId())
                    .eq(StoreFile::getName, entity.getName())
                    .count();
            if (count > 0) throw new BuzzException("已经存在同名目录");
        }

        // update full path
        this.syncFullPath(entity);
        entity.setDeleteAction(false);

        super.save(entity);

        // update dir count
        this.syncDirSize(entity.getParentId());

        // 保持历史版本
        if (!entity.getDir()) {
            storeFileHisBiz.saveSnapshot(entity);
        }

        return true;
    }

//    @CacheInvalidate(name = "store:file:fullpath:", key = "#entity.id")
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateById(StoreFile entity) {
        StoreFile current = requireAccessible(entity == null ? null : entity.getId());
        if (entity.getBucketId() == null) entity.setBucketId(current.getBucketId());
        if (!Objects.equals(entity.getBucketId(), current.getBucketId())) {
            throw new BuzzException("文件不能跨库修改");
        }
        if (entity.getParentId() == null) entity.setParentId(current.getParentId());
        if (entity.getDir() == null) entity.setDir(current.getDir());
        if (entity.getName() == null) entity.setName(current.getName());
        boolean parentChanged = !Objects.equals(entity.getParentId(), current.getParentId());
        if (parentChanged) {
            requireOperationTarget(List.of(current), entity.getParentId());
            syncFullPath(entity);
        } else {
            requireParent(entity.getBucketId(), entity.getParentId());
        }
        requireNameAvailable(entity, entity.getParentId(), entity.getId());

        boolean updated = super.updateById(entity);
        if (parentChanged) {
            syncDir(entity.getId());
            syncDirSize(current.getParentId());
            syncDirSize(entity.getParentId());
        }
        return updated;
    }

    public void updateInfo(StoreFile entity) {
        StoreFile current = requireAccessible(entity == null ? null : entity.getId());
        current.setInfo(entity.getInfo());
        this.updateById(current);
    }

    public void updateInfoBatch(List<StoreFile> list) {
        for (StoreFile storeFile : list) {
            this.updateInfo(storeFile);
        }
    }

    @Override
    public boolean saveBatch(Collection<StoreFile> entityList) {
        if (entityList == null) return true;
        entityList.forEach(this::save);
        return true;
    }

    @Override
    public boolean updateBatchById(Collection<StoreFile> entityList) {
        if (entityList == null) return true;
        entityList.forEach(this::updateById);
        return true;
    }

    @Override
    public boolean saveOrUpdate(StoreFile entity) {
        return entity.getId() == null ? save(entity) : updateById(entity);
    }

    @Override
    public boolean saveOrUpdateBatch(Collection<StoreFile> entityList) {
        if (entityList == null) return true;
        entityList.forEach(this::saveOrUpdate);
        return true;
    }

    public void loopDelete(List<StoreFile> list) {
        for (StoreFile item : list) {
            if (item.getDir()) {
                List<StoreFile> child = lambdaQuery().eq(StoreFile::getParentId, item.getId()).list();
                this.loopDelete(child);
            }
            baseMapper.deleteById(item.getId());
        }
    }

    @Override
    @Transactional
    public boolean removeById(Serializable id) {
        Integer fileId = parseFileId(id);
        StoreFile file = requireAccessible(fileId);

        // mark delete action
        lambdaUpdate().eq(StoreFile::getId, fileId).set(StoreFile::getDeleteAction, true).update();

        // 删除子元素
        List<StoreFile> child = lambdaQuery().eq(StoreFile::getParentId, fileId).list();
        loopDelete(child);

        baseMapper.deleteById(fileId);

        // sync parent Dir size
        this.syncDirSize(file.getParentId());

        return true;
    }

    @Override
    @Transactional
    public boolean removeBatchByIds(Collection<?> list) {
        return removeBatchSafely(list);
    }

    @Override
    @Transactional
    public void removeBatchByIds(List<Serializable> ids) {
        removeBatchSafely(ids);
    }

    public void loopDeletePre(List<StoreFile> list) {
        for (StoreFile item : list) {
            if (item.getDir()) {
                List<StoreFile> child = baseMapper.queryChildren(item.getId());
                this.loopDeletePre(child);
            }
            baseMapper.deleteByIdIgnoreLogic(item.getId());
        }
    }

    @Override
    @Transactional
    public void removePerById(Serializable id) {
        Integer fileId = parseFileId(id);
        requireTrash(fileId);
        // remove child item
        List<StoreFile> child = baseMapper.queryChildren(fileId);
        loopDeletePre(child);

        baseMapper.deleteByIdIgnoreLogic(fileId);
    }

    public void downloadZip(List<Integer> ids) throws IOException {
        if (ids == null || ids.isEmpty()) throw new BuzzException("未找到文件，请确认");
        ids.forEach(this::requireAccessible);
        List<StoreFile> list = lambdaQuery()
                .in(StoreFile::getId, ids)
                .orderByDesc(StoreFile::getDir)
                .orderByAsc(StoreFile::getName)
                .list();
        if (list == null || list.isEmpty()) throw new BuzzException("未找到文件，请确认");
        String firstName = list.get(0).getName();

        String now = DateUtil.format(new Date(), "yyyyMMddHHmmss");

        String zipFilePath = FaFileUtils.getAbsolutePath() + File.separator + "static"
                + File.separator + "zip"
                + File.separator + now
                + File.separator + firstName + "_" + System.currentTimeMillis();

        FileUtil.mkdir(zipFilePath);

        // loop set file
        this.addFiles(zipFilePath, list);

        // zip dir to file
        File zipFile = ZipUtil.zip(zipFilePath);

        FaFileUtils.downloadFile(zipFile);
    }

    public void addFiles(String rootDir, List<StoreFile> list) throws IOException {
        for (StoreFile storeFile : list) {
            if (storeFile.getDir()) {
                // create dir
                String dirPath = rootDir + File.separator + storeFile.getName();
                FileUtil.mkdir(dirPath);

                // query sub file
                List<StoreFile> subList = lambdaQuery()
                        .eq(StoreFile::getParentId, storeFile.getId())
                        .orderByDesc(StoreFile::getDir)
                        .orderByAsc(StoreFile::getName)
                        .list();

                this.addFiles(dirPath, subList);
            } else {
                File srcFile = fileSaveBiz.getFileObj(storeFile.getFileId());

                FileUtil.copyFile(srcFile, new File(rootDir));
                FileUtil.rename(new File(rootDir + File.separator + srcFile.getName()), storeFile.getName(), true);
            }
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void moveToDir(StoreFilesMoveTo params) {
        if (params == null) {
            throw new BuzzException("移动参数不能为空");
        }
        List<StoreFile> files = getActiveRoots(params.getFileIds());
        requireOperationTarget(files, params.getToDirId());
        Set<Integer> oldParentIds = files.stream()
                .map(StoreFile::getParentId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        for (StoreFile file : files) {
            file.setParentId(params.getToDirId());
            updateById(file);
        }
        syncDir(params.getToDirId());
        syncDirSize(params.getToDirId());
        oldParentIds.stream()
                .filter(parentId -> parentId > 0 && !Objects.equals(parentId, params.getToDirId()))
                .forEach(this::syncDirSize);
    }

    @Transactional(rollbackFor = Exception.class)
    public void copyToDir(StoreFilesMoveTo params) {
        if (params == null) {
            throw new BuzzException("复制参数不能为空");
        }
        List<StoreFile> files = getActiveRoots(params.getFileIds());
        requireOperationTarget(files, params.getToDirId());
        for (StoreFile file : files) {
            copyTree(file, params.getToDirId());
        }
        syncDir(params.getToDirId());
        syncDirSize(params.getToDirId());
    }

    private void copyTree(StoreFile source, Integer toDirId) {
        Integer sourceId = source.getId();
        requireNameAvailable(source, toDirId, null);
        source.setParentId(toDirId);
        source.setId(null);
        save(source);

        if (source.getDir()) {
            List<StoreFile> children = lambdaQuery().eq(StoreFile::getParentId, sourceId).list();
            for (StoreFile child : children) {
                copyTree(child, source.getId());
            }
        }
    }

    public void syncFileTags(Integer fileId) {
        StoreFile storeFile = requireAccessible(fileId);
        StoreFile.Tag[] tags = storeFileTagBiz.lambdaQuery()
                .eq(StoreFileTag::getFileId, fileId)
                .list()
                .stream().map(i -> {
                    StoreTag tag = storeTagBiz.requireAccessible(i.getTagId());
                    if (!Objects.equals(storeFile.getBucketId(), tag.getBucketId())) {
                        throw new BuzzException("文件标签不能跨库关联");
                    }
                    return new StoreFile.Tag(i.getId(), i.getTagId(), tag.getName(), tag.getColor());
                })
                .toArray(StoreFile.Tag[]::new);

        storeFile.setTags(tags);
        super.updateById(storeFile);
    }

    public void addTags(StoreFilesAddTags params) {
        if (params == null || params.getFileIds() == null || params.getTagIds() == null) {
            throw new BuzzException("文件和标签不能为空");
        }
        for (Integer fileId : params.getFileIds()) {
            StoreFile file = requireAccessible(fileId);
            for (Integer tagId : params.getTagIds()) {
                StoreTag tag = storeTagBiz.requireAccessible(tagId);
                if (!Objects.equals(file.getBucketId(), tag.getBucketId())) {
                    throw new BuzzException("文件和标签不能跨库操作");
                }
                long count = storeFileTagBiz.lambdaQuery()
                        .eq(StoreFileTag::getFileId, fileId)
                        .eq(StoreFileTag::getTagId, tagId)
                        .count();
                if (count == 1) continue; // no need to operate

                // remove pre tagLink
                if (count > 0) {
                    storeFileTagBiz.lambdaUpdate()
                            .eq(StoreFileTag::getFileId, fileId)
                            .eq(StoreFileTag::getTagId, tagId)
                            .remove();
                }

                // add new Tag Link
                StoreFileTag fileTag = new StoreFileTag();
                fileTag.setFileId(fileId);
                fileTag.setTagId(tagId);
                storeFileTagBiz.save(fileTag);
            }

            this.syncFileTags(fileId);
        }
    }

    public void syncDir(Integer dirId) {
        if (dirId != null && dirId > 0) requireAccessible(dirId);
        List<StoreFile> dirs = this.getFullPath(dirId);
        List<String> dirNames = dirs.stream().map(i -> i.getName()).collect(Collectors.toList());
        dirNames.add(0, ROOT_DIR_NAME);
        String fullPath = ArrayUtil.join(dirNames.toArray(), ",", "#", "#");

        List<StoreFile> list = lambdaQuery()
                .in(StoreFile::getBucketId, storeBucketUserBiz.getAccessibleBucketIds())
                .eq(StoreFile::getParentId, dirId)
                .list();
        for (StoreFile item : list) {
            item.setFullPath(fullPath);
            this.updateById(item);

            if (item.getDir()) {
                this.syncDir(item.getId());
            }
        }
    }

    public List<StoreFile> queryFile(BasePageQuery<StoreFileQueryVo> query) {
        prepareQuery(query, false);
        return baseMapper.queryFile(query.getQuery(), query.getSorter());
    }

    public TableRet<StoreFile> queryFilePage(BasePageQuery<StoreFileQueryVo> query) {
        prepareQuery(query, false);
        PageInfo<StoreFile> info = PageHelper.startPage(query.getCurrent(), query.getPageSize())
                .doSelectPageInfo(() -> baseMapper.queryFile(query.getQuery(), query.getSorter()));
        return new TableRet<>(info);
    }

    public TableRet<StoreFile> queryTrashFilePage(BasePageQuery<StoreFileQueryVo> query) {
        prepareQuery(query, true);
        query.getQuery().setDeleteAction(true);
        PageInfo<StoreFile> info = PageHelper.startPage(query.getCurrent(), query.getPageSize())
                .doSelectPageInfo(() -> baseMapper.queryFile(query.getQuery(), query.getSorter()));
        return new TableRet<>(info);
    }

    public void loopPutBack(List<StoreFile> list, Integer toDirId) {
        for (StoreFile item : list) {
            // put file back
            this.putFileBack(item, toDirId);

            if (item.getDir()) {
                List<StoreFile> child = baseMapper.queryChildren(item.getId());
                loopPutBack(child, item.getId());
            }
        }
        this.syncDirSize(toDirId);
    }

    @Transactional
    public void putBack(List<Integer> ids) {
        for (StoreFile file : getRestoreRoots(ids)) {
            restoreTree(file, file.getParentId());
        }
    }

    @Transactional
    public void putBackToDir(StoreFilesMoveTo params) {
        if (params == null || params.getToDirId() == null || params.getFileIds() == null || params.getFileIds().isEmpty()) {
            throw new BuzzException("目标文件夹不能为空");
        }
        List<StoreFile> files = getRestoreRoots(params.getFileIds());
        files.forEach(file -> requireParent(file.getBucketId(), params.getToDirId()));
        for (StoreFile file : files) {
            restoreTree(file, params.getToDirId());
        }
    }

    private void restoreTree(StoreFile file, Integer toDirId) {
        putFileBack(file, toDirId);
        loopPutBack(baseMapper.queryChildren(file.getId()), file.getId());
        syncDirSize(toDirId);
    }

    private List<StoreFile> getRestoreRoots(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new BuzzException("未选择文件");
        }
        Map<Integer, StoreFile> selected = new LinkedHashMap<>();
        for (Integer id : ids) {
            Integer fileId = parseFileId(id);
            selected.put(fileId, requireTrash(fileId));
        }
        Set<Integer> selectedIds = new LinkedHashSet<>(selected.keySet());
        return selected.values().stream()
                .filter(file -> !hasSelectedAncestor(file, selectedIds, selected))
                .collect(Collectors.toList());
    }

    private boolean removeBatchSafely(Collection<?> ids) {
        if (ids == null || ids.isEmpty()) return true;
        Map<Integer, StoreFile> selected = new LinkedHashMap<>();
        for (Object id : ids) {
            Integer fileId = parseFileId((Serializable) id);
            selected.put(fileId, requireAccessible(fileId));
        }
        Set<Integer> selectedIds = new LinkedHashSet<>(selected.keySet());
        selected.values().stream()
                .filter(file -> !hasSelectedAncestor(file, selectedIds, selected))
                .map(StoreFile::getId)
                .forEach(this::removeById);
        return true;
    }

    private boolean hasSelectedAncestor(StoreFile file, Set<Integer> selectedIds, Map<Integer, StoreFile> selected) {
        Integer parentId = file.getParentId();
        while (parentId != null && parentId > 0) {
            if (selectedIds.contains(parentId)) return true;
            StoreFile parent = selected.get(parentId);
            if (parent == null) return false;
            parentId = parent.getParentId();
        }
        return false;
    }

    public void putFileBack(StoreFile file, Integer toDirId) {
        storeBucketUserBiz.requireAccessible(file.getBucketId());
        // check toDirId exists
        String fullPath = ROOT_DIR_NAME;
        if (toDirId > 0) {
            StoreFile parentDir = requireAccessible(toDirId);
            if (!parentDir.getDir() || !Objects.equals(parentDir.getBucketId(), file.getBucketId())) {
                throw new BuzzException("目标文件夹不存在，请确认");
            }
            fullPath = parentDir.getFullPath();
        }

        // check file name single
        long count = lambdaQuery()
                .eq(StoreFile::getBucketId, file.getBucketId())
                .eq(StoreFile::getDir, file.getDir())
                .eq(StoreFile::getParentId, toDirId)
                .eq(StoreFile::getName, file.getName())
                .ne(StoreFile::getId, file.getId())
                .count();
        if (count > 0) {
            throw new BuzzException("目标文件夹[" + fullPath + "]存在相同文件[" + file.getName() + "]");
        }

        // put file back
        baseMapper.putFileTo(file.getId(), toDirId);

        file.setParentId(toDirId);
        this.syncFullPath(file);
        lambdaUpdate().eq(StoreFile::getId, file.getId())
                .set(StoreFile::getFullPath, file.getFullPath())
                .update();
    }

}
