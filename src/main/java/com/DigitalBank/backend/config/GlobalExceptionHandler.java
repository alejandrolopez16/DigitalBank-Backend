package com.DigitalBank.backend.config;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // Este método atrapa específicamente el error de tamaño de archivo excedido
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, String>> handleMaxSizeException(MaxUploadSizeExceededException exc) {
        
        // Devolvemos un código 413 (PAYLOAD_TOO_LARGE) y un mensaje JSON claro
        return ResponseEntity
                .status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(Map.of(
                        "error", "El archivo es demasiado grande.",
                        "detalle", "Por favor, sube una imagen que pese menos de 2MB."
                ));
    }
}
