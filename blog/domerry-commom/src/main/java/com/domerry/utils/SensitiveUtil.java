package com.domerry.utils;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * @author: quequnlong
 * @date: 2025/3/4
 * @description: 敏感词过滤工具类（需要在resource下放置敏感词文件）
 */
@Component
public class SensitiveUtil {

    private static final Logger logger = LoggerFactory.getLogger(SensitiveUtil.class);

    /**
     * 敏感词文件
     */
    private static final String SENSITIVE_WORD = "sensitive-words.txt";

    /**
     * 替换符
     */
    private static final String REPLACEMENT = "***";

    /**
     * 根节点
     */
    private static final TrieNode ROOT_NODE = new TrieNode();

    @PostConstruct
    public void init() {
        try (
                InputStream is = this.getClass().getClassLoader().getResourceAsStream(SENSITIVE_WORD);
                BufferedReader reader = new BufferedReader(new InputStreamReader(Objects.requireNonNull(is)))
        ) {
            String keyword;
            while ((keyword = reader.readLine()) != null) {
                // 添加到前缀树
                this.addKeyword(keyword);
            }
        } catch (Exception e) {
            logger.warn("加载敏感词文件失败: " + e.getMessage());
        }
    }

    /**
     * 将一个敏感词添加到前缀树中
     *
     * @param keyword 敏感词
     */
    private void addKeyword(String keyword) {
        TrieNode tempNode = ROOT_NODE;
        for (int i = 0; i < keyword.length(); i++) {
            char c = keyword.charAt(i);
            TrieNode subNode = tempNode.getSubNode(c);

            if (subNode == null) {
                // 初始化子节点
                subNode = new TrieNode();
                // 记录从根到该节点的路径是否全为东亚文字：决定"词内跳过符号"是否安全
                subNode.setCjkOnly(tempNode.isCjkOnly() && isCjk(c));
                tempNode.addSubNode(c, subNode);
            }

            // 指向子节点,进入下一轮循环
            tempNode = subNode;

            // 设置结束标识
            if (i == keyword.length() - 1) {
                tempNode.setKeywordEnd(true);
            }
        }
    }

    /**
     * 过滤敏感词
     *
     * @param text 待过滤的文本
     * @return 过滤后的文本
     */
    public static String filter(String text) {

        if (StringUtils.isBlank(text)) {
            return null;
        }
        // 指针1
        TrieNode tempNode = ROOT_NODE;
        // 指针2
        int begin = 0;
        // 指针3
        int position = 0;
        // 结果
        StringBuilder sb = new StringBuilder();

        while (position < text.length()) {
            char c = text.charAt(position);

            // 符号的处理分三种情况，核心是"不要跨符号匹配 ASCII 词"
            if (isSymbol(c)) {
                // 1) 前缀树里正好有这个符号（如 "G-M"、"F_U_C_K"）：符号是词的一部分，正常推进
                TrieNode symbolNode = tempNode.getSubNode(c);
                if (symbolNode != null) {
                    tempNode = symbolNode;
                    if (tempNode.isKeywordEnd()) {
                        sb.append(REPLACEMENT);
                        begin = ++position;
                        tempNode = ROOT_NODE;
                    } else {
                        position++;
                    }
                    continue;
                }
                // 2) 不在匹配中：符号原样计入结果，指针2向下走一步
                if (tempNode == ROOT_NODE) {
                    sb.append(c);
                    begin++;
                    position++;
                    continue;
                }
                // 3) 匹配中途遇到符号：只有全中文词允许跳过（"法 轮 功" 这类拆字规避），
                //    ASCII 词不允许——否则 "https://baidubox..." 里的 "s://b" 会命中 "sb"，
                //    把正常 URL 打成 "http***aidubox..."
                if (tempNode.isCjkOnly()) {
                    position++;
                    continue;
                }
                sb.append(text.charAt(begin));
                position = ++begin;
                tempNode = ROOT_NODE;
                continue;
            }

            // 检查下级节点
            tempNode = tempNode.getSubNode(c);
            if (tempNode == null) {
                // 以begin开头的字符串不是敏感词
                sb.append(text.charAt(begin));
                // 进入下一个位置
                position = ++begin;
                // 重新指向根节点
                tempNode = ROOT_NODE;
            } else if (tempNode.isKeywordEnd()) {
                // 发现敏感词,将begin~position字符串替换掉
                sb.append(REPLACEMENT);
                // 进入下一个位置
                begin = ++position;
                // 重新指向根节点
                tempNode = ROOT_NODE;
            } else {
                // 检查下一个字符
                position++;
            }
        }

        // 将最后一批字符计入结果
        sb.append(text.substring(begin));

        return sb.toString();
    }

    /**
     * 判断是否为符号
     *
     * @param c 字符
     * @return 判断
     */
    private static boolean isSymbol(Character c) {
        // 0x2E80~0x9FFF 是东亚文字范围
        return !isAsciiAlphanumeric(c) && (c < 0x2E80 || c > 0x9FFF);
    }

    /**
     * 判断是否为东亚文字（与 isSymbol 的范围互为补集）
     *
     * @param c 字符
     * @return 判断
     */
    private static boolean isCjk(Character c) {
        return c >= 0x2E80 && c <= 0x9FFF;
    }

    public static boolean isAsciiAlpha(char ch) {
        return isAsciiAlphaUpper(ch) || isAsciiAlphaLower(ch);
    }

    public static boolean isAsciiAlphaUpper(char ch) {
        return ch >= 'A' && ch <= 'Z';
    }

    public static boolean isAsciiAlphaLower(char ch) {
        return ch >= 'a' && ch <= 'z';
    }

    public static boolean isAsciiNumeric(char ch) {
        return ch >= '0' && ch <= '9';
    }

    public static boolean isAsciiAlphanumeric(char ch) {
        return isAsciiAlpha(ch) || isAsciiNumeric(ch);
    }

    /**
     * 前缀树
     */
    private static class TrieNode {

        // 关键词结束标识
        private boolean isKeywordEnd = false;

        // 从根到本节点的路径是否全为东亚文字
        private boolean cjkOnly = true;

        // 子节点(key是下级字符,value是下级节点)
        private final Map<Character, TrieNode> subNodes = new HashMap<>();

        public boolean isKeywordEnd() {
            return isKeywordEnd;
        }

        public void setKeywordEnd(boolean keywordEnd) {
            isKeywordEnd = keywordEnd;
        }

        public boolean isCjkOnly() {
            return cjkOnly;
        }

        public void setCjkOnly(boolean cjkOnly) {
            this.cjkOnly = cjkOnly;
        }

        // 添加子节点
        public void addSubNode(Character c, TrieNode node) {
            subNodes.put(c, node);
        }

        // 获取子节点
        public TrieNode getSubNode(Character c) {
            return subNodes.get(c);
        }

    }

}
