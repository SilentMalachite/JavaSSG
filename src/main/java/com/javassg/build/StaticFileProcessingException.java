package com.javassg.build;

public class StaticFileProcessingException extends RuntimeException {
    
    public StaticFileProcessingException(String message) {
        super(message);
    }
    
    public StaticFileProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}