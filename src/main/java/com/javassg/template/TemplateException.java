package com.javassg.template;

public class TemplateException extends RuntimeException {
    public TemplateException(String message) {
        super(message);
    }
    
    public TemplateException(String message, Throwable cause) {
        super(message, cause);
    }
}