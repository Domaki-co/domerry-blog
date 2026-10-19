package com.domerry.utils;

import org.apache.commons.lang3.StringUtils;

/**
 * @description: 富文本清洗工具：入库前剥离全部 HTML 标签并还原实体，只保留纯文本
 */
public class HtmlUtil {

    /**
     * 剥离/还原的迭代上限，只决定嵌套编码能解到第几层，不影响安全性（结尾必然再剥离一次）
     */
    private static final int MAX_UNESCAPE_ROUNDS = 3;

    /**
     * 单个实体的最大长度，超出即认为这个 & 只是普通字符
     */
    private static final int MAX_ENTITY_LENGTH = 32;

    /**
     * 把富文本降级为纯文本：剥离所有标签、丢弃注释与声明、还原 HTML 实体
     * 纯文本入库后，前端无论用 v-html 还是文本插值渲染都不可能再执行脚本
     *
     * @param html 原始富文本
     * @return 纯文本，入参为空时返回 null
     */
    public static String toPlainText(String html) {
        if (StringUtils.isBlank(html)) {
            return null;
        }

        String text = html;
        for (int i = 0; i < MAX_UNESCAPE_ROUNDS; i++) {
            String next = unescapeEntities(stripTags(text));
            if (next.equals(text)) {
                break;
            }
            text = next;
        }
        // 必须以剥离标签收尾：还原实体可能重新引入 <，只有再剥一次才能保证结果里不含任何标签
        return stripTags(text);
    }

    /**
     * 剥离标签：遇到 &lt; 后向后探测，只有确认是标签才丢弃，否则原样保留该字符
     * 这样 "5 &lt; 3" 这类正常文本不会被误删
     */
    private static String stripTags(String html) {
        StringBuilder sb = new StringBuilder(html.length());
        int length = html.length();
        int i = 0;

        while (i < length) {
            char c = html.charAt(i);
            if (c != '<') {
                sb.append(c);
                i++;
                continue;
            }

            // 只有 < 后面跟着字母、/、!、? 才可能是标签/注释/声明，否则这个 < 只是普通字符（如 "5 < 3"）
            int j = i + 1;
            if (j >= length || !isTagStart(html.charAt(j))) {
                sb.append(c);
                i++;
                continue;
            }

            int end = html.indexOf('>', j);
            if (end < 0) {
                // 未闭合的标签/注释，其余部分整体丢弃，避免 <!-- 把后面的内容整体吞掉
                break;
            }
            i = end + 1;
        }
        return sb.toString();
    }

    /**
     * 还原 HTML 实体：命名实体只认白名单，数字实体按码点还原
     * 还原后再由调用方重新剥离标签，避免 &amp;lt;script&amp;gt; 这类编码绕过存活
     */
    private static String unescapeEntities(String text) {
        if (text.indexOf('&') < 0) {
            return text;
        }

        StringBuilder sb = new StringBuilder(text.length());
        int length = text.length();
        int i = 0;

        while (i < length) {
            char c = text.charAt(i);
            if (c != '&') {
                sb.append(c);
                i++;
                continue;
            }

            // 数字实体：浏览器允许省略结尾的 ;，这里按同样的规则贪婪取数字，
            // 否则 &#60img src=x onerror=alert(1)> 会原样入库、再由浏览器解码成真标签
            if (i + 1 < length && text.charAt(i + 1) == '#') {
                int radix = 10;
                int end = i + 2;
                if (end < length && (text.charAt(end) == 'x' || text.charAt(end) == 'X')) {
                    radix = 16;
                    end++;
                }
                int digits = end;
                while (end < length && end - digits < MAX_ENTITY_LENGTH && isDigitForRadix(text.charAt(end), radix)) {
                    end++;
                }
                String decoded = end > digits ? decodeNumericEntity(text.substring(i + 1, end)) : null;
                if (decoded != null) {
                    sb.append(decoded);
                    i = end < length && text.charAt(end) == ';' ? end + 1 : end;
                    continue;
                }
                sb.append(c);
                i++;
                continue;
            }

            // 命名实体：必须带 ; 且在白名单内，否则这个 & 只是普通字符
            int semi = text.indexOf(';', i + 1);
            String decoded = semi > 0 && semi - i <= MAX_ENTITY_LENGTH ? decodeNamedEntity(text.substring(i + 1, semi)) : null;
            if (decoded == null) {
                sb.append(c);
                i++;
                continue;
            }

            sb.append(decoded);
            i = semi + 1;
        }
        return sb.toString();
    }

    /**
     * 解码命名实体（不含 & 和 ;），无法识别时返回 null
     */
    private static String decodeNamedEntity(String entity) {
        if (entity.isEmpty()) {
            return null;
        }

        switch (entity.toLowerCase()) {
            case "amp":
                return "&";
            case "lt":
                return "<";
            case "gt":
                return ">";
            case "quot":
                return "\"";
            case "apos":
                return "'";
            case "nbsp":
                return " ";
            case "ensp":
            case "emsp":
                return " ";
            case "copy":
                return "©";
            case "reg":
                return "®";
            case "trade":
                return "™";
            case "hellip":
                return "…";
            case "mdash":
                return "—";
            case "ndash":
                return "–";
            case "middot":
                return "·";
            case "laquo":
                return "«";
            case "raquo":
                return "»";
            default:
                return null;
        }
    }

    /**
     * 解码数字实体（形如 #60 / #x3C，不含结尾的 ;），非法码点或代理区码点一律丢弃（返回空串）
     */
    private static String decodeNumericEntity(String entity) {
        int radix = 10;
        String digits = entity.substring(1);
        if (!digits.isEmpty() && (digits.charAt(0) == 'x' || digits.charAt(0) == 'X')) {
            radix = 16;
            digits = digits.substring(1);
        }
        if (digits.isEmpty()) {
            return null;
        }

        int codePoint;
        try {
            codePoint = Integer.parseInt(digits, radix);
        } catch (NumberFormatException e) {
            return null;
        }

        // 排除代理区码点，避免产生落单的代理字符
        if (codePoint < 0 || codePoint > 0x10FFFF || (codePoint >= 0xD800 && codePoint <= 0xDFFF)) {
            return "";
        }
        return new String(Character.toChars(codePoint));
    }

    private static boolean isDigitForRadix(char c, int radix) {
        if (c >= '0' && c <= '9') {
            return true;
        }
        return radix == 16 && ((c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F'));
    }

    /**
     * 判断某个字符能否让 &lt; 成为一个标签/注释/声明的开头
     */
    private static boolean isTagStart(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || c == '/' || c == '!' || c == '?';
    }

}
