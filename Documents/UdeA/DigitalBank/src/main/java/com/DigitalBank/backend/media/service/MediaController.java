package com.DigitalBank.backend.media.service;

import com.DigitalBank.backend.media.service.CloudinaryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/media")
public class MediaController {

    private final CloudinaryService cloudinaryService;

    public MediaController(CloudinaryService cloudinaryService) {
        this.cloudinaryService = cloudinaryService;
    }

    // Endpoint exclusivo para recibir archivos físicos
    @PostMapping("/upload")
    public ResponseEntity<Map<String, String>> uploadFile(@RequestParam("file") MultipartFile file) {
        try {
            // Mandamos el archivo a Cloudinary
            String imageUrl = cloudinaryService.uploadImage(file);
            
            // Devolvemos un JSON simple con la URL pública
            return ResponseEntity.ok(Map.of("url", imageUrl));
            
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Error al subir la imagen a la nube"));
        }
    }
}