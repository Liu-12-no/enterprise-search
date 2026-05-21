package com.qych.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qych.entity.dtos.EntAdvancedQueryDTO;
import com.qych.entity.dtos.EntSyncDTO;
import com.qych.entity.pojos.EntBasicInfo;
import com.qych.entity.vo.EntBasicInfoVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * <p>
 * 企业基础信息表 Mapper 接口
 * </p>
 *
 */
@Mapper
public interface EntBasicInfoMapper extends BaseMapper<EntBasicInfo> {

    /**
     *
     * @param page 分页参数
     * @param kw  关键词
     * @return
     */
    public Page<EntBasicInfoVO> searchEnterprise(Page<EntBasicInfoVO> page, @Param("kw") String kw);


    /**
     *
     * @param page 分页参数
     * @param dto 传入实体
     * @return
     */
    public Page<EntBasicInfoVO> advancedSearchEnterprise(Page<EntBasicInfoVO> page, @Param("dto") EntAdvancedQueryDTO dto);

    /**
     * 查询企业的基本信息
     * @return
     */
   public List<EntSyncDTO> selectAllForEsSync();
}
