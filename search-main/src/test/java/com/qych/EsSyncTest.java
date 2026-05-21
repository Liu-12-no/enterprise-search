package com.qych;


import com.qych.entity.dtos.EntSyncDTO;
import com.qych.entity.es.EntBasicInfoDoc;
import com.qych.entity.pojos.EntBasicInfo;
import com.qych.mapper.EntBasicInfoMapper;
import com.qych.repository.EntBasicInfoRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.stream.Collectors;

@SpringBootTest
@Slf4j
public class EsSyncTest {

    @Autowired
    private EntBasicInfoMapper entBasicInfoMapper;

    @Autowired
    private EntBasicInfoRepository entBasicInfoRepository;

    @Test
    public void syncDataToEs(){

        //查询ent_basic_info数据库所有数据
        List<EntSyncDTO> entBasicInfos = entBasicInfoMapper.selectAllForEsSync();

        log.info("从 MySQL 查到数据条数：" + entBasicInfos.size());

        if(entBasicInfos.isEmpty()){
            System.out.println("数据库为空，无需同步");
        }

        List<EntBasicInfoDoc> esDocList = entBasicInfos.stream().map(ent -> {
            EntBasicInfoDoc entBasicInfoDoc = new EntBasicInfoDoc();

            BeanUtils.copyProperties(ent, entBasicInfoDoc);
            entBasicInfoDoc.setName(ent.getInstitutionName());            // 公司名称
            entBasicInfoDoc.setCreditCode(ent.getSocialCreditCode());     // 统一社会信用代码
            entBasicInfoDoc.setAddress(ent.getRegisteredAddress());       // 注册地址
            entBasicInfoDoc.setTypeTag(ent.getEnterpriseNature());        // 公司登记注册类型
            entBasicInfoDoc.setLegalPerson(ent.getLegalPersonName());     // 法人


            if (ent.getBusinessState() != null) {
                entBasicInfoDoc.setStatusTag(ent.getBusinessState().toString());
            }

            // 注册资本: BigDecimal 转 Long (直接取整数部分)
            if (ent.getRegisteredCapital() != null) {
                entBasicInfoDoc.setRegCapital(ent.getRegisteredCapital().longValue());
            }

            // 成立日期: LocalDate 转 String (格式会变成 "yyyy-MM-dd")
            if (ent.getEstablishDate() != null) {
                entBasicInfoDoc.setEstablishDate(ent.getEstablishDate().toString());
            }




            return entBasicInfoDoc;
        }).collect(Collectors.toList());

        entBasicInfoRepository.saveAll(esDocList);
        System.out.println(esDocList);

        System.out.println("数据成功同步到 Elasticsearch！");


    }

    @Test
    public void checkDataInEs() {
        // 使用 Repository 自带的 count() 方法统计数量
        long count = entBasicInfoRepository.count();
        System.out.println("ES 中现在的总数据量是：" + count + " 条");

        // 查询所有数据并打印第一条看看效果
        Iterable<EntBasicInfoDoc> allDocs = entBasicInfoRepository.findAll();
        if (allDocs.iterator().hasNext()) {
            System.out.println("随便看一条 ES 里的数据：" + allDocs.iterator().next());
        }
    }

}
