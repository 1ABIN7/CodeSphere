package com.CodeSphere.backend.service;

public interface TokenDenylistService {

    void denylistToken(String tokenId, long expiryDurationMs);

    boolean isDenylisted(String tokenId);
}