package com.swizima.filesapi.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "File Data Transfer Object")  
public abstract class FileDTO {
    @Schema(description = "Original filename as uploaded by user", example = "document.pdf")
    private String originalFilename;
    
    @Schema(description = "System generated unique filename", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890.pdf")
    private String filename;
    
    @Schema(description = "Unique identifier for the file", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
    private String fileUuid;
    
    @Schema(description = "File size in bytes", example = "1024000")
    private Long fileSize;
    
    @Schema(description = "File extension", example = "pdf")
    private String fileType;
    
    @Schema(description = "MIME type of the file", example = "application/pdf")
    private String mimeType;
    
    @Schema(description = "Content type category", example = "DOCUMENT", allowableValues = {"IMAGE", "VIDEO", "AUDIO", "TEXT", "DOCUMENT", "SPREADSHEET", "PRESENTATION", "ARCHIVE", "OTHER"})
    private String contentType;
    
    @Schema(description = "Full path to the file in storage", example = "/storage/requests/REQ-2024-001/a1b2c3d4-e5f6-7890-abcd-ef1234567890.pdf")
    private String filePath;
}
