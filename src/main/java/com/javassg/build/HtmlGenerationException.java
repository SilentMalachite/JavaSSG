package com.javassg.build;

public class HtmlGenerationException extends RuntimeException {
    
    public HtmlGenerationException(String message) {
        super(message);
    }
    
    public HtmlGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}