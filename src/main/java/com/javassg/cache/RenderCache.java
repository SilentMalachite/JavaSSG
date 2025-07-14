package com.javassg.cache;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

public class RenderCache {
    
    private final ConcurrentMap<String, CacheEntry> renderCache;
    private final AtomicLong hitCount = new AtomicLong(0);
    private final AtomicLong missCount = new AtomicLong(0);
    private final int maxSize;
    
    public RenderCache() {
        this(1000);
    }
    
    public RenderCache(int maxSize) {
        this.maxSize = maxSize;
        this.renderCache = new ConcurrentHashMap<>();
    }
    
    public void putRendered(String key, String content, LocalDateTime timestamp) {
        evictIfNecessary();
        renderCache.put(key, new CacheEntry(content, timestamp));
    }
    
    public Optional<String> getRendered(String key) {
        CacheEntry entry = renderCache.get(key);
        if (entry != null) {
            hitCount.incrementAndGet();
            return Optional.of(entry.content());
        } else {
            missCount.incrementAndGet();
            return Optional.empty();
        }
    }
    
    public boolean isValid(String key, LocalDateTime lastModified) {
        CacheEntry entry = renderCache.get(key);
        return entry != null && !entry.timestamp().isBefore(lastModified);
    }
    
    public String generateKey(String template, String content, Map<String, Object> context) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            
            digest.update(template.getBytes(StandardCharsets.UTF_8));
            digest.update(content.getBytes(StandardCharsets.UTF_8));
            
            TreeMap<String, Object> sortedContext = new TreeMap<>(context);
            for (Map.Entry<String, Object> entry : sortedContext.entrySet()) {
                digest.update(entry.getKey().getBytes(StandardCharsets.UTF_8));
                digest.update(entry.getValue().toString().getBytes(StandardCharsets.UTF_8));
            }
            
            byte[] hash = digest.digest();
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256アルゴリズムが利用できません", e);
        }
    }
    
    public void invalidateByPattern(String pattern) {
        String regex = pattern.replace("*", ".*");
        renderCache.entrySet().removeIf(entry -> entry.getKey().matches(regex));
    }
    
    public void remove(String key) {
        renderCache.remove(key);
    }
    
    public void clear() {
        renderCache.clear();
        hitCount.set(0);
        missCount.set(0);
    }
    
    public CacheStatistics getStatistics() {
        return new CacheStatistics(
            renderCache.size(),
            hitCount.get(),
            missCount.get()
        );
    }
    
    private void evictIfNecessary() {
        if (renderCache.size() >= maxSize) {
            String oldestKey = renderCache.entrySet()
                .stream()
                .min(Map.Entry.comparingByValue((e1, e2) -> e1.timestamp().compareTo(e2.timestamp())))
                .map(Map.Entry::getKey)
                .orElse(null);
            
            if (oldestKey != null) {
                renderCache.remove(oldestKey);
            }
        }
    }
    
    public record CacheEntry(String content, LocalDateTime timestamp) {}
    
    public record CacheStatistics(
        int totalEntries,
        long hitCount,
        long missCount
    ) {
        public double getHitRatio() {
            long total = hitCount + missCount;
            return total > 0 ? (double) hitCount / total : 0.0;
        }
    }
}