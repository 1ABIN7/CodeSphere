package com.CodeSphere.backend.service.impl;

import com.CodeSphere.backend.service.StorageService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class StorageServiceImpl implements StorageService {

    @Override
    public String store(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Failed to store empty file.");
        }
        return "/storage/uploads/" + System.currentTimeMillis() + "_" + file.getOriginalFilename();
    }
}