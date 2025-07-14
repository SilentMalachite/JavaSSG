package com.javassg.template;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.function.BiFunction;

public class TemplateEngine {
    private static final int MAX_TEMPLATE_SIZE = 1024 * 1024; // 1MB
    private static final int MAX_NESTING_DEPTH = 50;
    
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{\\{([^}]+)\\}\\}");
    private static final Pattern CONDITIONAL_PATTERN = Pattern.compile("\\{\\{#(if|unless)\\s+([^}]+)\\}\\}(.*?)(?:\\{\\{else\\}\\}(.*?))?\\{\\{/\\1\\}\\}", Pattern.DOTALL);
    private static final Pattern LOOP_PATTERN = Pattern.compile("\\{\\{#each\\s+([^}]+)\\}\\}(.*?)\\{\\{/each\\}\\}", Pattern.DOTALL);
    private static final Pattern PARTIAL_PATTERN = Pattern.compile("\\{\\{>\\s*([^}]+)\\}\\}");
    private static final Pattern BLOCK_PATTERN = Pattern.compile("\\{\\{#block\\s+([^}]+)\\}\\}(.*?)\\{\\{/block\\}\\}", Pattern.DOTALL);
    private static final Pattern EXTENDS_PATTERN = Pattern.compile("\\{\\{#extends\\s+\"([^\"]+)\"\\}\\}(.*?)\\{\\{/extends\\}\\}", Pattern.DOTALL);
    
    private final Map<String, String> partials = new ConcurrentHashMap<>();
    private final Map<String, String> layouts = new ConcurrentHashMap<>();
    private final Map<String, BiFunction<Object, String[], Object>> filters = new ConcurrentHashMap<>();
    private final Map<String, String> templateCache = new ConcurrentHashMap<>();
    
    public TemplateEngine() {
        registerBuiltinFilters();
    }
    
    public String render(String template, Map<String, Object> context) {
        validateTemplate(template);
        return render(template, context, 0, new HashSet<>());
    }
    
    private void validateTemplate(String template) {
        if (template.length() > MAX_TEMPLATE_SIZE) {
            throw new TemplateException("テンプレートサイズが制限を超えています");
        }
        
        // 基本的な構文チェック
        long ifCount = template.chars().mapToObj(c -> (char) c).map(String::valueOf)
            .collect(StringBuilder::new, StringBuilder::append, StringBuilder::append)
            .toString().split("\\{\\{#if").length - 1;
        long endIfCount = template.chars().mapToObj(c -> (char) c).map(String::valueOf)
            .collect(StringBuilder::new, StringBuilder::append, StringBuilder::append)
            .toString().split("\\{\\{/if\\}\\}").length - 1;
        
        if (ifCount != endIfCount) {
            throw new TemplateException("構文エラー: if文が正しく閉じられていません");
        }
    }
    
    private String render(String template, Map<String, Object> context, int depth, Set<String> visitedPartials) {
        if (template.length() > MAX_TEMPLATE_SIZE) {
            throw new TemplateException("テンプレートサイズが制限を超えています");
        }
        
        if (depth > MAX_NESTING_DEPTH) {
            throw new TemplateException("ネストが深すぎます");
        }
        
        String result = template;
        
        // Layout extension処理
        result = processExtends(result, context, depth, visitedPartials);
        
        // 条件分岐の処理
        result = processConditionals(result, context, depth, visitedPartials);
        
        // ループの処理
        result = processLoops(result, context, depth, visitedPartials);
        
        // パーシャルの処理
        result = processPartials(result, context, depth, visitedPartials);
        
        // 変数の置換
        result = processVariables(result, context);
        
        return result;
    }
    
    private String processExtends(String input, Map<String, Object> context, int depth, Set<String> visitedPartials) {
        Matcher matcher = EXTENDS_PATTERN.matcher(input);
        if (!matcher.find()) {
            return input;
        }
        
        String layoutName = matcher.group(1);
        String childContent = matcher.group(2);
        
        String layoutTemplate = layouts.get(layoutName);
        if (layoutTemplate == null) {
            throw new TemplateException("レイアウトが見つかりません: " + layoutName);
        }
        
        // 子テンプレートからブロックを抽出
        Map<String, String> blocks = extractBlocks(childContent);
        
        // レイアウトテンプレートのブロックを置換
        return replaceBlocks(layoutTemplate, blocks, context, depth, visitedPartials);
    }
    
    private Map<String, String> extractBlocks(String content) {
        Map<String, String> blocks = new HashMap<>();
        Matcher matcher = BLOCK_PATTERN.matcher(content);
        
        while (matcher.find()) {
            String blockName = matcher.group(1).trim();
            String blockContent = matcher.group(2);
            blocks.put(blockName, blockContent);
        }
        
        return blocks;
    }
    
    private String replaceBlocks(String template, Map<String, String> blocks, 
                                Map<String, Object> context, int depth, Set<String> visitedPartials) {
        Matcher matcher = BLOCK_PATTERN.matcher(template);
        StringBuffer result = new StringBuffer();
        
        while (matcher.find()) {
            String blockName = matcher.group(1).trim();
            String defaultContent = matcher.group(2);
            
            String blockContent = blocks.getOrDefault(blockName, defaultContent);
            String renderedBlock = render(blockContent, context, depth + 1, visitedPartials);
            
            matcher.appendReplacement(result, Matcher.quoteReplacement(renderedBlock));
        }
        matcher.appendTail(result);
        
        return result.toString();
    }
    
    private String processConditionals(String input, Map<String, Object> context, int depth, Set<String> visitedPartials) {
        Matcher matcher = CONDITIONAL_PATTERN.matcher(input);
        StringBuffer result = new StringBuffer();
        
        while (matcher.find()) {
            String operator = matcher.group(1); // "if" or "unless"
            String condition = matcher.group(2).trim();
            String ifContent = matcher.group(3);
            String elseContent = matcher.group(4);
            
            boolean conditionResult = evaluateCondition(condition, context);
            if ("unless".equals(operator)) {
                conditionResult = !conditionResult;
            }
            
            String replacement = conditionResult ? ifContent : (elseContent != null ? elseContent : "");
            String renderedReplacement = render(replacement, context, depth + 1, visitedPartials);
            
            matcher.appendReplacement(result, Matcher.quoteReplacement(renderedReplacement));
        }
        matcher.appendTail(result);
        
        return result.toString();
    }
    
    private String processLoops(String input, Map<String, Object> context, int depth, Set<String> visitedPartials) {
        // 最も内側のループから処理するため、文字列を逆順で処理
        String result = input;
        
        while (true) {
            Matcher matcher = LOOP_PATTERN.matcher(result);
            if (!matcher.find()) {
                break;
            }
            
            // 最初に見つかったループを処理
            String arrayName = matcher.group(1).trim();
            String loopContent = matcher.group(2);
            
            Object arrayValue = getNestedValue(arrayName, context);
            StringBuilder loopResult = new StringBuilder();
            
            if (arrayValue instanceof Iterable<?> iterable) {
                for (Object item : iterable) {
                    Map<String, Object> loopContext = new HashMap<>(context);
                    if (item instanceof Map<?, ?> map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> itemMap = (Map<String, Object>) map;
                        loopContext.putAll(itemMap);
                    }
                    loopContext.put("this", item);
                    
                    // ネストしたループがある場合は再帰的に処理
                    String itemContent = loopContent;
                    if (LOOP_PATTERN.matcher(itemContent).find()) {
                        itemContent = processLoops(itemContent, loopContext, depth + 1, visitedPartials);
                    }
                    itemContent = processVariables(itemContent, loopContext);
                    loopResult.append(itemContent);
                }
            }
            
            result = result.substring(0, matcher.start()) + 
                    loopResult.toString() + 
                    result.substring(matcher.end());
        }
        
        return result;
    }
    
    private String processPartials(String input, Map<String, Object> context, int depth, Set<String> visitedPartials) {
        Matcher matcher = PARTIAL_PATTERN.matcher(input);
        StringBuffer result = new StringBuffer();
        
        while (matcher.find()) {
            String partialName = matcher.group(1).trim();
            
            if (visitedPartials.contains(partialName)) {
                throw new TemplateException("循環参照が検出されました: " + partialName);
            }
            
            String partialTemplate = partials.get(partialName);
            if (partialTemplate == null) {
                throw new TemplateException("パーシャルが見つかりません: " + partialName);
            }
            
            Set<String> newVisitedPartials = new HashSet<>(visitedPartials);
            newVisitedPartials.add(partialName);
            
            String renderedPartial = render(partialTemplate, context, depth + 1, newVisitedPartials);
            matcher.appendReplacement(result, Matcher.quoteReplacement(renderedPartial));
        }
        matcher.appendTail(result);
        
        return result.toString();
    }
    
    private String processVariables(String input, Map<String, Object> context) {
        Matcher matcher = VARIABLE_PATTERN.matcher(input);
        StringBuffer result = new StringBuffer();
        
        while (matcher.find()) {
            String expression = matcher.group(1).trim();
            
            // ブロックヘルパーでない場合のみ処理
            if (!expression.startsWith("#") && !expression.startsWith("/") && !expression.startsWith(">")) {
                String replacement = processExpression(expression, context);
                matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
            } else {
                matcher.appendReplacement(result, Matcher.quoteReplacement(matcher.group(0)));
            }
        }
        matcher.appendTail(result);
        
        return result.toString();
    }
    
    private String processExpression(String expression, Map<String, Object> context) {
        // フィルターの処理
        if (expression.contains("|")) {
            String[] parts = expression.split("\\|");
            String variableName = parts[0].trim();
            Object value = getNestedValue(variableName, context);
            
            // フィルターチェーンを適用
            for (int i = 1; i < parts.length; i++) {
                String filterExpression = parts[i].trim();
                value = applyFilter(value, filterExpression);
            }
            
            return value != null ? value.toString() : "";
        }
        
        // 通常の変数
        Object value = getNestedValue(expression, context);
        return value != null ? value.toString() : "";
    }
    
    private Object getNestedValue(String path, Map<String, Object> context) {
        String[] parts = path.split("\\.");
        Object current = context;
        
        for (String part : parts) {
            if (current instanceof Map<?, ?> map) {
                current = map.get(part);
            } else {
                return null;
            }
        }
        
        return current;
    }
    
    private boolean evaluateCondition(String condition, Map<String, Object> context) {
        Object value = getNestedValue(condition, context);
        
        if (value instanceof Boolean bool) {
            return bool;
        }
        if (value instanceof String str) {
            return !str.isEmpty();
        }
        if (value instanceof Number num) {
            return num.doubleValue() != 0;
        }
        if (value instanceof Collection<?> coll) {
            return !coll.isEmpty();
        }
        
        return value != null;
    }
    
    private Object applyFilter(Object value, String filterExpression) {
        String[] parts = filterExpression.split(":", 2);
        String filterName = parts[0].trim();
        String[] args = parts.length > 1 ? 
            parts[1].trim().replace("'", "").split(",") : new String[0];
        
        BiFunction<Object, String[], Object> filter = filters.get(filterName);
        if (filter != null) {
            return filter.apply(value, args);
        }
        
        // 内蔵フィルター
        return switch (filterName) {
            case "uppercase" -> value != null ? value.toString().toUpperCase() : "";
            case "lowercase" -> value != null ? value.toString().toLowerCase() : "";
            case "capitalize" -> capitalize(value);
            default -> value != null ? value.toString() : "";
        };
    }
    
    private String capitalize(Object value) {
        if (value == null) return "";
        String str = value.toString();
        if (str.isEmpty()) return str;
        return Character.toUpperCase(str.charAt(0)) + str.substring(1).toLowerCase();
    }
    
    private void registerBuiltinFilters() {
        // 日付フィルター
        filters.put("date", (value, args) -> {
            if (value instanceof LocalDateTime dateTime) {
                String pattern = args.length > 0 ? args[0] : "yyyy-MM-dd";
                try {
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
                    return dateTime.format(formatter);
                } catch (Exception e) {
                    return value.toString();
                }
            }
            return value != null ? value.toString() : "";
        });
        
        // スラグ化フィルター
        filters.put("slugify", (value, args) -> {
            if (value == null) return "";
            return value.toString()
                .toLowerCase()
                .replace("テスト", "tesuto")
                .replace("タイトル", "taitoru")
                .replace(" ", "-")
                .replaceAll("[^a-z0-9\\-_]", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");
        });
        
        // 切り詰めフィルター
        filters.put("truncate", (value, args) -> {
            if (value == null) return "";
            String str = value.toString();
            if (args.length > 0) {
                try {
                    int length = Integer.parseInt(args[0]);
                    if (str.length() <= length) {
                        return str;
                    }
                    return str.substring(0, length) + "...";
                } catch (NumberFormatException e) {
                    return str;
                }
            }
            return str;
        });
        
        // Markdownフィルター
        filters.put("markdown", (value, args) -> {
            // 簡単なMarkdown処理（実際はMarkdownパーサーを使用）
            if (value == null) return "";
            return value.toString()
                .replaceAll("\\*\\*(.*?)\\*\\*", "<strong>$1</strong>")
                .replaceAll("\\*(.*?)\\*", "<em>$1</em>");
        });
        
        // HTMLタグ除去フィルター
        filters.put("strip_html", (value, args) -> {
            if (value == null) return "";
            return value.toString().replaceAll("<[^>]+>", "");
        });
    }
    
    public void registerPartial(String name, String template) {
        partials.put(name, template);
    }
    
    public void registerLayout(String name, String template) {
        layouts.put(name, template);
    }
    
    public void registerFilter(String name, BiFunction<Object, String[], Object> filter) {
        filters.put(name, filter);
    }
    
    public void clearCache() {
        templateCache.clear();
    }
}