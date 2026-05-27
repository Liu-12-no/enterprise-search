package com.qych;


import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qych.entity.dtos.EntAdvancedQueryDTO;
import com.qych.entity.vo.EntBasicInfoVO;
import com.qych.service.EntSearchService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@Slf4j
public class EntSearchServiceIntegrationTest {


    @Autowired
    private EntSearchService entSearchService;

    @Test
    public void advancedSearchTest(){

        EntAdvancedQueryDTO dto = new EntAdvancedQueryDTO();
        dto.setKeyword("科技");
        dto.setPageNum(1);
        dto.setPageSize(10);

        Page<EntBasicInfoVO> result = entSearchService.advancedSearch(dto);

        assertNotNull(result);

        log.info("搜索到 {} 条记录", result.getTotal());


    }

    @Test
    public void advancedSearchLimitTest(){

        EntAdvancedQueryDTO dto = new EntAdvancedQueryDTO();

        dto.setKeyword("有限公司");
        dto.setStatusTags(Arrays.asList("存续"));
        dto.setPageNum(1);
        dto.setPageSize(5);

        Page<EntBasicInfoVO> result = entSearchService.advancedSearch(dto);
        assertNotNull(result);
        log.info("带状态过滤后搜到 {} 条", result.getTotal());

    }
}
