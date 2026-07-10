package com.kato.pro.langchain.domain.prompt;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 模板变量替换器（D4=B：自实现 ${var}）。
 *
 * 规则：
 *   - vars 包含 key 且 value 非 null → 替换为 value.toString()
 *   - vars 包含 key 且 value 为 null → 替换为 ""
 *   - vars 不包含 key            → 保留原样 ${var}
 */
public class PromptRenderer {

    private static final Pattern VAR_PATTERN = Pattern.compile("\\$\\{([^}]+)\\}");

    public String substitute(String template, Map<String, Object> vars) {
        if (template == null) return "";
        if (vars == null || vars.isEmpty()) return template;
        Matcher m = VAR_PATTERN.matcher(template);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            String key = m.group(1).trim();
            String out;
            if (!vars.containsKey(key)) {
                // 缺失 → 保留原样
                out = Matcher.quoteReplacement(m.group(0));
            } else {
                Object val = vars.get(key);
                out = Matcher.quoteReplacement(val == null ? "" : val.toString());
            }
            m.appendReplacement(sb, out);
        }
        m.appendTail(sb);
        return sb.toString();
    }
}
