package com.qych.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qych.entity.dtos.EntAdvancedQueryDTO;
import com.qych.entity.dtos.EntBasicInfoQueryDTO;
import com.qych.entity.vo.EntBasicInfoVO;
import com.qych.entity.vo.EntDetailVO;
import com.qych.service.EntSearchService;
import com.qych.service.IEntBasicInfoService;
import com.qych.utils.pojo.BaseResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/enterprise")
public class EntBasicInfoController {

    @Autowired
    private IEntBasicInfoService entBasicInfoService;

    @Autowired
    private EntSearchService entSearchService;;



    @PostMapping("/search")
    public BaseResponse<Page<EntBasicInfoVO>> searchEnterprise (@RequestBody EntBasicInfoQueryDTO dto){

        //查询基本信息
        Page<EntBasicInfoVO> entBasicInfoVOPage = entBasicInfoService.searchByKeyword(dto);

        return BaseResponse.success(entBasicInfoVOPage);
    }


    @GetMapping("/detail/{id}")
    public BaseResponse<EntDetailVO> getEnterpriseDetail(@PathVariable("id") Long id){

        //根据id查询详情信息
        EntDetailVO enterpriseDetail = entBasicInfoService.getEnterpriseDetailById(id);

        return BaseResponse.success(enterpriseDetail);
    }

    @PostMapping("/advancedSearch")
    public BaseResponse<Page<EntBasicInfoVO>> advancedSearch(@RequestBody EntAdvancedQueryDTO dto){

        //高级搜索
        Page<EntBasicInfoVO> entBasicInfoVOPage = entSearchService.advancedSearch(dto);

        return BaseResponse.success(entBasicInfoVOPage);
    }


    /**
     * 自然语言搜索接口
     */
/*    @GetMapping("/ai-search")
    public BaseResponse<Page<EntBasicInfoVO>> aiSearch(@RequestParam String sentence){
        EntAdvancedQueryDTO dto = nl2SearchAgent.extractQueryDto(sentence);

        //补全分页参数
        if (dto.getPageNum() == null) dto.setPageNum(1);
        if (dto.getPageSize() == null) dto.setPageSize(10);

        //执行es搜索
        Page<EntBasicInfoVO> entBasicInfoVOPage = entSearchService.advancedSearch(dto);

        return BaseResponse.success(entBasicInfoVOPage);


    }*/





}
