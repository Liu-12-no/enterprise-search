package com.qych.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qych.entity.vo.EntBasicInfoVO;
import com.qych.entity.vo.EntDetailVO;

//异步缓存处理服务接口
public interface ICacheAsyncService {

    /**异步回填企业搜索结果缓存
     * @param cacheKey
     * @param entityPage
     */
    public void saveSearchCache(String cacheKey, Page<EntBasicInfoVO> entityPage);

    /**
     * 异步回填企业详情缓存
     * @param cacheKey
     * @param vo
     */
    public void saveDetailCache(String cacheKey, EntDetailVO vo);
}
