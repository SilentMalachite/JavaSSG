package com.javassg.template;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;

public class TemplateEngine {
    private static final int MAX_TEMPLATE_SIZE = 1024 * 1024; // 1MB
    private static final int MAX_NESTING_DEPTH = 50;
    
    private final Map<String, String> templates = new ConcurrentHashMap<>();
    private final Map<String, BiFunction<Object, String[], Object>> customFilters = new ConcurrentHashMap<>();
    
    public TemplateEngine() {
        registerCustomFilters();
    }
    
    public String render(String templateName, Map<String, Object> context) {
        try {
            validateTemplate(templateName);
            String templateContent = templates.getOrDefault(templateName, templateName);
            
            // 簡易テンプレートレンダリング
            return renderTemplate(templateContent, context, 0, new HashSet<>());
        } catch (Exception e) {
            throw new TemplateException("テンプレートのレンダリングに失敗しました: " + e.getMessage(), e);
        }
    }
    
    public void addTemplate(String name, String content) {
        validateTemplateContent(content);
        templates.put(name, content);
    }
    
    private void validateTemplate(String templateName) {
        if (templateName == null || templateName.trim().isEmpty()) {
            throw new TemplateException("テンプレート名は必須です");
        }
    }
    
    private void validateTemplateContent(String content) {
        if (content.length() > MAX_TEMPLATE_SIZE) {
            throw new TemplateException("テンプレートサイズが制限を超えています");
        }
        
        if (content.trim().isEmpty()) {
            throw new TemplateException("テンプレートコンテンツが空です");
        }
    }
    
    private String renderTemplate(String template, Map<String, Object> context, int depth, Set<String> visited) {
        if (depth > MAX_NESTING_DEPTH) {
            throw new TemplateException("ネストが深すぎます");
        }
        
        String result = template;
        
        // 変数置換
        result = replaceVariables(result, context);
        
        // 簡易if文処理
        result = processIfStatements(result, context);
        
        // 簡易ループ処理
        result = processLoops(result, context);
        
        return result;
    }
    
    private String replaceVariables(String template, Map<String, Object> context) {
        String result = template;
        
        // {{variable}} 形式の変数を置換
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\{\\{([^}]+)\\}\\}");
        java.util.regex.Matcher matcher = pattern.matcher(result);
        StringBuffer sb = new StringBuffer();
        
        while (matcher.find()) {
            String variable = matcher.group(1).trim();
            Object value = getValueFromContext(variable, context);
            String replacement = value != null ? value.toString() : "";
            matcher.appendReplacement(sb, java.util.regex.Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(sb);
        
        return sb.toString();
    }
    
    private Object getValueFromContext(String path, Map<String, Object> context) {
        String[] parts = path.split("\\.");
        Object current = context;
        
        for (String part : parts) {
            if (current instanceof Map) {
                current = ((Map<?, ?>) current).get(part);
            } else if (current != null) {
                // オブジェクトのプロパティアクセスは簡易的にスキップ
                current = null;
            }
            
            if (current == null) {
                break;
            }
        }
        
        return current;
    }
    
    private String processIfStatements(String template, Map<String, Object> context) {
        // 簡易的なif文処理 - {{#if variable}}content{{/if}}
        java.util.regex.Pattern ifPattern = java.util.regex.Pattern.compile(
            "\\{\\{#if\\s+([^}]+)\\}\\}(.*?)\\{\\{/if\\}\\}", java.util.regex.Pattern.DOTALL);
        
        String result = template;
        java.util.regex.Matcher matcher = ifPattern.matcher(result);
        
        while (matcher.find()) {
            String condition = matcher.group(1).trim();
            String content = matcher.group(2);
            
            Object value = getValueFromContext(condition, context);
            boolean shouldInclude = isTruthy(value);
            
            String replacement = shouldInclude ? content : "";
            result = result.replace(matcher.group(0), replacement);
            
            //matcherをリセットして再検索
            matcher = ifPattern.matcher(result);
        }
        
        return result;
    }
    
    private String processLoops(String template, Map<String, Object> context) {
        // 簡易的なループ処理 - {{#each items}}{{this}}{{/each}}
        java.util.regex.Pattern eachPattern = java.util.regex.Pattern.compile(
            "\\{\\{#each\\s+([^}]+)\\}\\}(.*?)\\{\\{/each\\}\\}", java.util.regex.Pattern.DOTALL);
        
        String result = template;
        java.util.regex.Matcher matcher = eachPattern.matcher(result);
        
        while (matcher.find()) {
            String variable = matcher.group(1).trim();
            String content = matcher.group(2);
            
            Object value = getValueFromContext(variable, context);
            StringBuilder loopOutput = new StringBuilder();
            
            if (value instanceof Iterable) {
                for (Object item : (Iterable<?>) value) {
                    Map<String, Object> loopContext = new HashMap<>(context);
                    loopContext.put("this", item);
                    
                    String itemContent = replaceVariables(content, loopContext);
                    loopOutput.append(itemContent);
                }
            }
            
            result = result.replace(matcher.group(0), loopOutput.toString());
            matcher = eachPattern.matcher(result);
        }
        
        return result;
    }
    
    private boolean isTruthy(Object value) {
        if (value == null) return false;
        if (value instanceof Boolean) return (Boolean) value;
        if (value instanceof Collection) return !((Collection<?>) value).isEmpty();
        if (value instanceof String) return !((String) value).isEmpty();
        return true;
    }
    
    private void registerCustomFilters() {
        customFilters.put("date", (input, args) -> {
            if (input instanceof LocalDateTime) {
                LocalDateTime dateTime = (LocalDateTime) input;
                String format = args.length > 0 ? args[0] : "yyyy-MM-dd";
                return dateTime.format(DateTimeFormatter.ofPattern(format));
            }
            return input;
        });
        
        customFilters.put("excerpt", (input, args) -> {
            if (input instanceof String) {
                String text = (String) input;
                int length = args.length > 0 ? Integer.parseInt(args[0]) : 150;
                if (text.length() <= length) {
                    return text;
                }
                return text.substring(0, length).trim() + "...";
            }
            return input;
        });
        
        customFilters.put("slugify", (input, args) -> {
            if (input instanceof String) {
                String text = (String) input;
                return text.toLowerCase()
                    .replaceAll("[^a-z0-9\\s-]", "")
                    .replaceAll("\\s+", "-")
                    .replaceAll("-+", "-")
                    .replaceAll("^-|-$", "");
            }
            return input;
        });
    }
}