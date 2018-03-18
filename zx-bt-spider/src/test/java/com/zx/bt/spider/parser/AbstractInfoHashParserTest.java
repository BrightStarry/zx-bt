package com.zx.bt.spider.parser;

import com.zx.bt.common.entity.Metadata;
import com.zx.bt.spider.SpiderApplicationTests;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.junit.Assert.*;

/**
 * author:ZhengXing
 * datetime:2018-03-18 16:31
 */
public class AbstractInfoHashParserTest extends SpiderApplicationTests{

    @Autowired
    private List<AbstractInfoHashParser> parserList;

    @Test
    public void parse() throws Exception {
        for (AbstractInfoHashParser item : parserList) {
            try {
                Metadata m = item.parse("89d296c86731a7012d63eae44cf8163512d23d53");
                System.out.println(item.metadataType  + "---" + m);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}