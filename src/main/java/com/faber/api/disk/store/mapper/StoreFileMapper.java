package com.faber.api.disk.store.mapper;

import com.faber.core.config.mybatis.base.FaBaseMapper;
import com.faber.api.disk.store.entity.StoreFile;
import com.faber.api.disk.store.vo.req.StoreFileQueryVo;
import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * STORE-文件
 * 
 * @author Farando
 * @email faberxu@gmail.com
 * @date 2022-12-22 09:31:17
 */
public interface StoreFileMapper extends FaBaseMapper<StoreFile> {

    @InterceptorIgnore(tenantLine = "true")
    @Select("select tenant_id from disk_store_file where id = #{id} and deleted = false")
    String selectTenantIdByIdForOnlyoffice(@Param("id") Integer id);

    List<StoreFile> queryFile(@Param("query") StoreFileQueryVo query, @Param("sorter") String sorter);

    List<StoreFile> queryChildren(@Param("id") Integer id);

    void putFileTo(@Param("id") Integer id, @Param("toDirId") Integer toDirId);

    Long sumFileSizeByBucketId(@Param("bucketId") Integer bucketId);

    long countByBucketId(@Param("bucketId") Integer bucketId);

    long countDeletedById(@Param("id") Integer id);

}
