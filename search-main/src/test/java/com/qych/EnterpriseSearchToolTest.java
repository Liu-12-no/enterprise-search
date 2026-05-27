package com.qych;

import com.qych.tool.EnterpriseSearchTool;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Slf4j
public class EnterpriseSearchToolTest {

    @Autowired
    private EnterpriseSearchTool enterpriseSearchTool;

    @Test
    public void searchEnterpriseTest(){

        String result = enterpriseSearchTool.searchEnterprise(
                "科技",    // keyword
                1,        // pageNum
                5,        // pageSize
                null,     // statusTags
                null,     // capitalTags
                null      // yearTags
        );

        assertNotNull(result);
        // 有数据时应包含分隔符"|"，无数据时返回"未查询到"
        assertTrue(result.contains("|") || result.contains("未查询到"));
        log.info("searchEnterprise 返回：\n{}", result);
    }
}
