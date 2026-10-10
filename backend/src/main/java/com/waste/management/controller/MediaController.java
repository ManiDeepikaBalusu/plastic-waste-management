package com.waste.management.controller;

import com.waste.management.dto.MediaUploadResponse;
import com.waste.management.service.MediaService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/media")
public class MediaController {

    private final MediaService mediaService;

    public MediaController(MediaService mediaService) {
        this.mediaService = mediaService;
    }

    @PostMapping("/upload")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MediaUploadResponse> uploadImage(@RequestParam("file") MultipartFile file) {
        MediaUploadResponse response = mediaService.uploadImage(file);
        return ResponseEntity.ok(response);
    }
}
