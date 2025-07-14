package com.javassg.cache;

import com.javassg.model.Page;
import com.javassg.model.Post;

import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.Optional;

public class ContentCache {
    
    private final ConcurrentMap<String, CacheEntry<Page>> pageCache = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, CacheEntry<Post>> postCache = new ConcurrentHashMap<>();
    
    public void putPage(String key, Page page) {
        pageCache.put(key, new CacheEntry<>(page, page.lastModified()));
    }
    
    public Optional<Page> getPage(String key) {
        CacheEntry<Page> entry = pageCache.get(key);
        return entry != null ? Optional.of(entry.content()) : Optional.empty();
    }
    
    public void putPost(String key, Post post) {
        postCache.put(key, new CacheEntry<>(post, post.lastModified()));
    }
    
    public Optional<Post> getPost(String key) {
        CacheEntry<Post> entry = postCache.get(key);
        return entry != null ? Optional.of(entry.content()) : Optional.empty();
    }
    
    public boolean isValid(String key, LocalDateTime lastModified) {
        CacheEntry<?> pageEntry = pageCache.get(key);
        if (pageEntry != null) {
            return !pageEntry.timestamp().isBefore(lastModified);
        }
        
        CacheEntry<?> postEntry = postCache.get(key);
        if (postEntry != null) {
            return !postEntry.timestamp().isBefore(lastModified);
        }
        
        return false;
    }
    
    public void remove(String key) {
        pageCache.remove(key);
        postCache.remove(key);
    }
    
    public void clear() {
        pageCache.clear();
        postCache.clear();
    }
    
    public int size() {
        return pageCache.size() + postCache.size();
    }
    
    public record CacheEntry<T>(T content, LocalDateTime timestamp) {}
}