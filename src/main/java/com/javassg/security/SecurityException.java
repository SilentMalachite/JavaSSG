package com.javassg.security;

/**
 * セキュリティ関連の例外
 */
public class SecurityException extends Exception {
    
    private final String securityCode;
    
    public SecurityException(String message) {
        super(message);
        this.securityCode = "SECURITY_ERROR";
    }
    
    public SecurityException(String message, String securityCode) {
        super(message);
        this.securityCode = securityCode;
    }
    
    public SecurityException(String message, Throwable cause) {
        super(message, cause);
        this.securityCode = "SECURITY_ERROR";
    }
    
    public SecurityException(String message, String securityCode, Throwable cause) {
        super(message, cause);
        this.securityCode = securityCode;
    }
    
    public String getSecurityCode() {
        return securityCode;
    }
}