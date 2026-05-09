package com.smartagent.aiservice.exception;

public class RateLimitException extends LLMException {
    public RateLimitException(String message) {
        super(message);
    }
}