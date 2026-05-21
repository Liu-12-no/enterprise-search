package com.qych.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qych.entity.dtos.EntAdvancedQueryDTO;
import com.qych.entity.vo.EntBasicInfoVO;

public interface EntSearchService {

    public Page<EntBasicInfoVO> advancedSearch(EntAdvancedQueryDTO dto);
}
