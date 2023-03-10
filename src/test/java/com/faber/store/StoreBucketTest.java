package com.faber.store;


import com.faber.FaTestApp;
import com.faber.api.disk.store.biz.StoreBucketBiz;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;

@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest(classes = {FaTestApp.class}, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class StoreBucketTest {

    @Resource
    StoreBucketBiz storeBucketBiz;

    @Test
    public void testSyncBucketSize() {
        storeBucketBiz.syncBucketSize();
    }

}
