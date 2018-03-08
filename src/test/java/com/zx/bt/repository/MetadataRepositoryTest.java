package com.zx.bt.repository;

import com.zx.bt.BtApplicationTests;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.junit.Assert.*;

/**
 * author:ZhengXing
 * datetime:2018-03-08 20:46
 */
public class MetadataRepositoryTest extends BtApplicationTests {

    @Autowired
    private MetadataRepository metadataRepository;

    @Test
    public void findInfoHash() throws Exception {
        List<String> infoHash = metadataRepository.findInfoHash(20, 10);
        System.out.println(infoHash);
    }

}