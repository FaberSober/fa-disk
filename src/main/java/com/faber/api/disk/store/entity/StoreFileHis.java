package com.faber.api.disk.store.entity;

import com.alibaba.excel.annotation.ExcelProperty;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.faber.core.annotation.FaModalName;
import com.faber.core.bean.BaseDelEntity;
import lombok.Data;

    
/**
 * STORE-文件-历史记录
 * 
 * @author xu.pengfei
 * @email 1508075252@qq.com
 * @date 2023-03-14 17:13:27
 */
@FaModalName(name = "STORE-文件-历史记录")
@TableName("disk_store_file_his")
@Data
public class StoreFileHis extends BaseDelEntity {
	
    @ExcelProperty("ID")
    @TableId(type = IdType.AUTO)
    private Integer id;

    @ExcelProperty("存储文件ID")
    private Integer storeFileId;

    @ExcelProperty("版本文件ID")
    private String fileSaveId;

    @ExcelProperty("文件名")
    private String fileName;

    @ExcelProperty("备注")
    private String remark;

}
