package com.faber.api.disk.store.biz;

import com.faber.api.disk.store.entity.StoreFile;
import org.springframework.stereotype.Service;

import com.faber.api.disk.store.entity.StoreFileHis;
import com.faber.api.disk.store.mapper.StoreFileHisMapper;
import com.faber.core.web.biz.BaseBiz;

/**
 * STORE-文件-历史记录
 *
 * @author xu.pengfei
 * @email 1508075252@qq.com
 * @date 2023-03-14 17:13:27
 */
@Service
public class StoreFileHisBiz extends BaseBiz<StoreFileHisMapper,StoreFileHis> {

    public void saveSnapshot(StoreFile storeFile) {
        StoreFileHis storeFileHis = new StoreFileHis();
        storeFileHis.setStoreFileId(storeFile.getId());
        storeFileHis.setFileSaveId(storeFile.getFileId());
        storeFileHis.setFileName(storeFile.getName());
        super.save(storeFileHis);
    }

}