package com.qych.utils;

import java.util.HashMap;
import java.util.Map;

/**
 * 企业状态数据字典工具类 (纯内存映射，极速解析)
 */
public class EntStateDictUtil {

    // 正向映射：中文 -> 状态码 (供高级查询入参使用)
    private static final Map<String, String> LABEL_TO_CODE_MAP = new HashMap<>();
    
    // 反向映射：状态码 -> 中文 (供列表查询出参使用)
    private static final Map<String, String> CODE_TO_LABEL_MAP = new HashMap<>();

    static {
        // 1=运营 相关
        addMapping("存续/在业", "1");
        addMapping("存续", "1");
        addMapping("在业", "1");
        addMapping("迁入", "1");
        addMapping("迁出", "1");
        // 2=破产清算
        addMapping("清算", "2");
        // 3=接受法律调查
        addMapping("接受法律调查", "3");
        // 4=终止运营 相关
        addMapping("注销", "4");
        addMapping("吊销", "4");
        addMapping("撤销", "4");
        addMapping("停业", "4");
        addMapping("已歇业", "4");
        addMapping("责令关闭", "4");
        addMapping("解散", "4");
    }

    /**
     * 反向映射时，我们约定 1="运营", 2="破产清算", 3="接受法律调查", 4="终止运营" 作为主词
     */
    private static void addMapping(String label, String code) {
        LABEL_TO_CODE_MAP.put(label, code);
    }
    
    // 手动初始化反向标准词典
    static {
        CODE_TO_LABEL_MAP.put("1", "存续");
        CODE_TO_LABEL_MAP.put("2", "破产清算");
        CODE_TO_LABEL_MAP.put("3", "接受法律调查");
        CODE_TO_LABEL_MAP.put("4", "终止运营");
    }

    /**
     * 中文转代码 ("存续/在业" -> "1")
     */
    public static String getCode(String label) {
        return LABEL_TO_CODE_MAP.get(label);
    }

    /**
     * 代码转中文 ("1" -> "运营")
     */
    public static String getLabel(String code) {
        return CODE_TO_LABEL_MAP.getOrDefault(code, "未知状态");
    }
}