package com.qych.repository;

import com.qych.entity.es.EntBasicInfoDoc;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;



/**
 * 第一个参数：关联的 ES 实体类 (EntBasicInfoDoc)
 * 第二个参数：实体类主键的类型 (Long)
 */
@Repository
@Mapper
public interface EntBasicInfoRepository extends ElasticsearchRepository<EntBasicInfoDoc, Long> {

}