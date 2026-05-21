package com.qych.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.qych.entity.dtos.EntBasicInfoQueryDTO;
import com.qych.entity.pojos.EntBasicInfo;
import com.qych.entity.pojos.EntBusinessOverview;
import com.qych.entity.vo.*;

import java.util.List;

/**
 * <p>
 * 企业基础信息表 服务类
 * </p>
 *
 * @author byl
 * @since 2026-04-17
 */
public interface IEntBasicInfoService extends IService<EntBasicInfo> {

    public Page<EntBasicInfoVO> searchByKeyword(EntBasicInfoQueryDTO dto);

    public EntDetailVO getEnterpriseDetailById(Long id);

    public List<EntShareholderVO> getEntShareholderVOS(String institutionId);

    public List<EntExecutiveVO> getExecutiveVOS(String institutionId);

    public List<EntPatentVO> getEntPatentVOS(String institutionId);

    public List<EntQualificationVO> getEntQualificationVOS(String institutionName);

    public EntBusinessOverview getEntBusinessOverview(String institutionName);
}
