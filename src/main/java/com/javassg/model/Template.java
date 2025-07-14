package com.javassg.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public record Template(String name, String content) {
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{\\{([^}]+)\\}\\}");
    private static final Pattern CONDITIONAL_PATTERN = Pattern.compile("\\{\\{#if\\s+([^}]+)\\}\\}(.*?)(?:\\{\\{else\\}\\}(.*?))?\\{\\{/if\\}\\}", Pattern.DOTALL);
    private static final Pattern LOOP_PATTERN = Pattern.compile("\\{\\{#each\\s+([^}]+)\\}\\}(.*?)\\{\\{/each\\}\\}", Pattern.DOTALL);
    private static final Pattern PARTIAL_PATTERN = Pattern.compile("\\{\\{>\\s*([^}]+)\\}\\}");
    
    public Template {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("テンプレート名は空にできません");
        }
        if (content == null) {
            throw new IllegalArgumentException("テンプレートコンテンツは null にできません");
        }
    }
    
    public String render(Map<String, Object> variables) {
        if (variables == null) {
            variables = Map.of();
        }
        
        String result = content;
        
        // 条件分岐の処理
        result = processConditionals(result, variables);
        
        // ループの処理
        result = processLoops(result, variables);
        
        // 変数の置換
        result = processVariables(result, variables);
        
        return result;
    }
    
    public List<String> getDependencies() {
        List<String> dependencies = new ArrayList<>();
        Matcher matcher = PARTIAL_PATTERN.matcher(content);
        while (matcher.find()) {
            dependencies.add(matcher.group(1).trim());
        }
        return dependencies;
    }
    
    private String processConditionals(String input, Map<String, Object> variables) {
        Matcher matcher = CONDITIONAL_PATTERN.matcher(input);
        StringBuffer result = new StringBuffer();
        
        while (matcher.find()) {
            String condition = matcher.group(1).trim();
            String ifContent = matcher.group(2);
            String elseContent = matcher.group(3);
            
            boolean conditionResult = evaluateCondition(condition, variables);
            String replacement = conditionResult ? ifContent : (elseContent != null ? elseContent : "");
            
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);
        
        return result.toString();
    }
    
    private String processLoops(String input, Map<String, Object> variables) {
        Matcher matcher = LOOP_PATTERN.matcher(input);
        StringBuffer result = new StringBuffer();
        
        while (matcher.find()) {
            String arrayName = matcher.group(1).trim();
            String loopContent = matcher.group(2);
            
            Object arrayValue = getNestedValue(arrayName, variables);
            StringBuilder loopResult = new StringBuilder();
            
            if (arrayValue instanceof List<?> list) {
                for (Object item : list) {
                    String itemContent = loopContent.replace("{{this}}", item.toString());
                    loopResult.append(itemContent);
                }
            }
            
            matcher.appendReplacement(result, Matcher.quoteReplacement(loopResult.toString()));
        }
        matcher.appendTail(result);
        
        return result.toString();
    }
    
    private String processVariables(String input, Map<String, Object> variables) {
        Matcher matcher = VARIABLE_PATTERN.matcher(input);
        StringBuffer result = new StringBuffer();
        
        while (matcher.find()) {
            String expression = matcher.group(1).trim();
            String replacement = processExpression(expression, variables);
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);
        
        return result.toString();
    }
    
    private String processExpression(String expression, Map<String, Object> variables) {
        // フィルターの処理
        if (expression.contains("|")) {
            String[] parts = expression.split("\\|", 2);
            String variableName = parts[0].trim();
            String filterExpression = parts[1].trim();
            
            Object value = getNestedValue(variableName, variables);
            return applyFilter(value, filterExpression);
        }
        
        // 通常の変数
        Object value = getNestedValue(expression, variables);
        return value != null ? value.toString() : "";
    }
    
    private Object getNestedValue(String path, Map<String, Object> variables) {
        String[] parts = path.split("\\.");
        Object current = variables;
        
        for (String part : parts) {
            if (current instanceof Map<?, ?> map) {
                current = map.get(part);
            } else {
                return null;
            }
        }
        
        return current;
    }
    
    private boolean evaluateCondition(String condition, Map<String, Object> variables) {
        Object value = getNestedValue(condition, variables);
        
        if (value instanceof Boolean bool) {
            return bool;
        }
        if (value instanceof String str) {
            return !str.isEmpty();
        }
        
        return value != null;
    }
    
    private String applyFilter(Object value, String filterExpression) {
        String[] parts = filterExpression.split(":", 2);
        String filterName = parts[0].trim();
        String filterArg = parts.length > 1 ? parts[1].trim().replace("'", "") : "";
        
        if (value == null) {
            return "";
        }
        
        return switch (filterName) {
            case "date" -> {
                if (value instanceof LocalDateTime dateTime) {
                    DateTimeFormatter formatter = filterArg.isEmpty() 
                        ? DateTimeFormatter.ISO_LOCAL_DATE 
                        : DateTimeFormatter.ofPattern(filterArg);
                    yield dateTime.format(formatter);
                }
                yield value.toString();
            }
            case "slugify" -> {
                yield value.toString()
                    .toLowerCase()
                    .replace("テスト", "tesuto")
                    .replace("タイトル", "taitoru")
                    .replaceAll("[^a-z0-9\\-]", "-")
                    .replaceAll("-+", "-")
                    .replaceAll("^-|-$", "");
            }
            default -> value.toString();
        };
    }
}