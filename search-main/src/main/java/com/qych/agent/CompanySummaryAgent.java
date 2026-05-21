package com.qych.agent;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.UserMessage;

/**
 *专用于“企业智能速览”的 AI Agent
 */
public interface CompanySummaryAgent {

    @SystemMessage({
            "你是一位资深的商业分析师，精通企业尽调与数据分析。",
            "你的任务是根据用户提供的企业工商公开数据，撰写一份高度精炼、专业的【企业速览诊断】。",
            "【严格要求】：",
            "1. 语言必须客观、严谨、专业，带有商业报告的质感。",
            "2. 严格基于用户提供的数据进行总结，【绝对禁止】编造或发散数据以外的信息（零幻觉）。",
            "3. 直接输出正文，【绝对禁止】输出“好的”、“没问题”、“为您总结如下”等任何寒暄废话。",
            "4.彻底禁用 Markdown 语法：【严禁】使用星号(**)、井号(#)等任何排版符号，纯文本输出！",
            "5. 输出字数控制在 200 字左右，分段要清晰。"
    })
    TokenStream summarize(@UserMessage String companyDataPrompt);
}
