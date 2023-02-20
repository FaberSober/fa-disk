package com.faber.api.disk.store;

import com.faber.core.config.dbinit.DbInit;
import com.faber.core.config.dbinit.vo.FaDdl;
import com.faber.core.config.dbinit.vo.FaDdlSql;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Farando
 * @date 2023/2/18 20:12
 * @description
 */
@Slf4j
@Service
public class FaDiskDbInit implements DbInit {

    @Override
    public String getNo() {
        return "fa-disk";
    }

    @Override
    public String getName() {
        return "网盘模块";
    }

    @Override
    public List<FaDdl> getDdlList() {
        List<FaDdl> list = new ArrayList<>();

        list.add(new FaDdl(100_000_000L, "V1.0.0", "初始化")
                .addSql(new FaDdlSql("初始化", "sql/1.0.0_disk_full.sql")));

        return list;
    }
}
