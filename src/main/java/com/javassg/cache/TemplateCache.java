package com.javassg.cache;

import com.javassg.model.Template;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class TemplateCache {
    
    private final ConcurrentMap<String, CacheEntry<Template>> templateCache = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Set<String>> dependencies = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, LocalDateTime> invalidationTimes = new ConcurrentHashMap<>();
    
    public void putTemplate(String name, Template template) {
        templateCache.put(name, new CacheEntry<>(template, LocalDateTime.now()));
        invalidationTimes.remove(name);
    }
    
    public Optional<Template> getTemplate(String name) {
        CacheEntry<Template> entry = templateCache.get(name);
        if (entry != null && !isInvalidated(name)) {
            return Optional.of(entry.content());
        }
        return Optional.empty();
    }
    
    public boolean isValid(String name, LocalDateTime lastModified) {
        CacheEntry<Template> entry = templateCache.get(name);
        if (entry == null) {
            return false;
        }
        
        if (isInvalidated(name)) {
            return false;
        }
        
        return !entry.timestamp().isBefore(lastModified);
    }
    
    public void addDependency(String template, String dependency) {
        dependencies.computeIfAbsent(template, k -> ConcurrentHashMap.newKeySet()).add(dependency);
    }
    
    public Set<String> getDependencies(String template) {
        return dependencies.getOrDefault(template, Collections.emptySet());
    }
    
    public void invalidateDependents(String templateName) {
        LocalDateTime now = LocalDateTime.now();
        
        for (Map.Entry<String, Set<String>> entry : dependencies.entrySet()) {
            if (entry.getValue().contains(templateName)) {
                invalidationTimes.put(entry.getKey(), now);
            }
        }
        
        invalidationTimes.put(templateName, now);
    }
    
    private boolean isInvalidated(String name) {
        return invalidationTimes.containsKey(name);
    }
    
    public void remove(String name) {
        templateCache.remove(name);
        dependencies.remove(name);
        invalidationTimes.remove(name);
    }
    
    public void clear() {
        templateCache.clear();
        dependencies.clear();
        invalidationTimes.clear();
    }
    
    public int size() {
        return templateCache.size();
    }
    
    public record CacheEntry<T>(T content, LocalDateTime timestamp) {}
}