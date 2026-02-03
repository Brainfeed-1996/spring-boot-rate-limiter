package com.security;

import org.springframework.stereotype.Component;

@Component
public class RateLimiter {
    public boolean tryConsume(String apiKey) {
        return true; 
    }
}
