package com.qych.entity.vo;

import lombok.Data;

import java.io.Serializable;


    /**
     * 企业列表展示 VO
    */
    @Data
    public class EntBasicInfoVO implements Serializable {

        private Long id;

        /**
         * 关联id
         */
        private String institutionId;

        /**
         * 企业 Logo
         */
        private String logo;

        /**
         * 企业名称
         */
        private String name;

        /**
         * 经营状态
         */
        private String statusTag;

        /**
         * 特性（如：小微企业、高新企业）
         */
        private String typeTag;

        /**
         * 法定代表人
         */
        private String legalPerson;

        /**
         * 注册资本
         */
        private String regCapital;

        /**
         * 成立日期
         */
        private String establishDate;

        /**
         * 统一社会信用代码
         */
        private String creditCode;

        /**
         * 电话
         */
        private String tel;

        /**
         * 邮箱
         */
        private String email;

        /**
         * 注册地址
         */
        private String address;


    }

