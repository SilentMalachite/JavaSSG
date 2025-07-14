package com.javassg.build;

public class AssetProcessingException extends RuntimeException {
    
    public AssetProcessingException(String message) {
        super(message);
    }
    
    public AssetProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}