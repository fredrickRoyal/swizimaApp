package com.swizima.filesapi.service;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.io.IOException;
import java.util.UUID;

import org.apache.tika.Tika;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.stereotype.Service;
import org.springframework.core.io.Resource;
import org.springframework.core.io.FileSystemResource;

import com.swizima.filesapi.dto.OrganisationLogoFileDTO;
import com.swizima.filesapi.dto.ResponseDTO;
import com.swizima.filesapi.repository.OrganisationLogoRepository;
import com.swizima.filesapi.entity.OrganisationLogoFile;
import com.swizima.filesapi.enums.ContentType;
import com.swizima.filesapi.globleexceptions.NotFoundException;
import org.springframework.util.StringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrganisationLogoFileServiceImpl implements OrganisationLogoFileService {
    private final OrganisationLogoRepository organisationLogoRepository;
    private final Tika tika = new Tika();
    
    @Value("${file.storage.root-path}")
    private String rootStoragePath;
    
    @Value("${file.storage.max-file-size}")
    private String maxFileSize;
    
    @Value("${file.storage.allowed-extensions}")
    private String allowedExtensions;
    
    @Override
    public ResponseDTO<String> uploadFile(MultipartFile file, String organisationId) {
        try {
            validateFile(file);
            
            String fileUuid = null;

            do {
                fileUuid = UUID.randomUUID().toString();
            } while (organisationLogoRepository.existsByFileUuid(fileUuid));

            String originalFilename = file.getOriginalFilename();
            String fileExtension = getFileExtension(originalFilename);
            String uniqueFilename = fileUuid + "." + fileExtension;
            
            Path organisationLogoStoragePath = Paths.get(rootStoragePath.trim(), "organisations", organisationId, "logo");
            Files.createDirectories(organisationLogoStoragePath);
            
            Path filePath = organisationLogoStoragePath.resolve(uniqueFilename);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
            if (!Files.exists(filePath)) {
                throw new RuntimeException("File write could not be verified on disk: " + filePath);
            }

            String mimeType = tika.detect(filePath.toFile());
            ContentType contentType = determineContentType(fileExtension, mimeType);
            
            OrganisationLogoFile fileEntity = OrganisationLogoFile.builder()
                    .originalFilename(originalFilename)
                    .filename(uniqueFilename)
                    .filePath(filePath.toString())
                    .fileSize(file.getSize())
                    .fileType(fileExtension)
                    .mimeType(mimeType)
                    .contentType(contentType)
                    .organisationId(organisationId)
                    .fileUuid(fileUuid)
                    .build();
            
            OrganisationLogoFile savedEntity = organisationLogoRepository.save(fileEntity);
            
            log.info("File uploaded successfully: {}", originalFilename);
            
            return ResponseDTO.success(savedEntity.getFileUuid());
            
        } catch (Exception e) {
            log.error("Error uploading file: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to upload file: " + e.getMessage());
        }
    }
 
    @Override
    public Resource downloadFile(String fileUuid) {
        OrganisationLogoFile fileEntity = organisationLogoRepository.findByFileUuid(fileUuid)
        .orElseThrow(() -> new RuntimeException("File not found"));

        Path filePath = getPhysicalPath(fileEntity);
        if (!Files.exists(filePath)) {
            log.error("File not found on disk at path: {}", filePath);
            throw new RuntimeException("File not found on server");
        }

        return new FileSystemResource(filePath);
    }

    @Override
    public InputStream streamFile(String fileUuid) {
        try {
            OrganisationLogoFile fileEntity = organisationLogoRepository.findByFileUuid(fileUuid)
            .orElseThrow(() -> new RuntimeException("File not found"));

            Path filePath = getPhysicalPath(fileEntity);
            if (!Files.exists(filePath)) {
                log.error("File not found on disk at path: {}", filePath);
                throw new RuntimeException("File not found on server");
            }

            return Files.newInputStream(filePath);
        } catch (IOException e) {
            throw new RuntimeException("Failed to stream file: " + e.getMessage());
        }
    }
    
    @Override
    public ResponseDTO<OrganisationLogoFileDTO> getFile(String organisationId) {
        OrganisationLogoFile fileEntity = organisationLogoRepository.findByOrganisationId(organisationId)
        .orElseThrow(() -> new NotFoundException("File not found"));

        return ResponseDTO.success(convertToOrganisationLogoDTO(fileEntity));
    }

    private boolean isValidFileType(String filename) {
        String extension = getFileExtension(filename).toLowerCase();
        return allowedExtensions.toLowerCase().contains(extension);
    }

    private boolean isValidFileSize(long fileSize) {
        long maxSize = parseFileSize(maxFileSize);
        return fileSize <= maxSize;
    }

    @Override
    public Resource generateThumbnail(String fileUuid) {
        try {
            OrganisationLogoFile fileEntity = organisationLogoRepository.findByFileUuid(fileUuid)
                    .orElseThrow(() -> new RuntimeException("File not found"));
            
            Path filePath = getPhysicalPath(fileEntity);
            if (!Files.exists(filePath)) {
                log.warn("Original file not found for thumbnail generation: {}", filePath);
                return null;
            }
            return new FileSystemResource(filePath);
        } catch (Exception e) {
            log.error("Error generating thumbnail for file: {}", fileUuid, e);
            throw new RuntimeException("Failed to generate thumbnail: " + e.getMessage());
        }
    }

    @Override
    public Resource generatePreview(String fileUuid) {
        try {
            OrganisationLogoFile fileEntity = organisationLogoRepository.findByFileUuid(fileUuid)
                    .orElseThrow(() -> new RuntimeException("File not found"));
            
            Path filePath = getPhysicalPath(fileEntity);
            if (!Files.exists(filePath)) {
                log.warn("Original file not found for preview generation: {}", filePath);
                return null;
            }
            return new FileSystemResource(filePath);
        } catch (Exception e) {
            log.error("Error generating preview for file: {}", fileUuid, e);
            throw new RuntimeException("Failed to generate preview: " + e.getMessage());
        }
    }

    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new RuntimeException("File is empty");
        }
        
        if (!isValidFileType(file.getOriginalFilename())) {
            throw new RuntimeException("File type not allowed");
        }
        
        if (!isValidFileSize(file.getSize())) {
            throw new RuntimeException("File size exceeds maximum allowed size");
        }
    }
    
    private String getFileExtension(String filename) {
        return StringUtils.getFilenameExtension(filename);
    }
    
    private ContentType determineContentType(String extension, String mimeType) {
        if (mimeType.startsWith("image/")) return ContentType.IMAGE;
        if (mimeType.startsWith("video/")) return ContentType.VIDEO;
        if (mimeType.startsWith("audio/")) return ContentType.AUDIO;
        if (mimeType.startsWith("text/")) return ContentType.OTHER;
        
        switch (extension.toLowerCase()) {
            case "pdf", "doc", "docx": return ContentType.DOCUMENT;
            case "xls", "xlsx": return ContentType.DOCUMENT;
            case "ppt", "pptx": return ContentType.DOCUMENT;
            case "zip", "rar", "7z": return ContentType.OTHER;
            default: return ContentType.OTHER;
        }
    }
    
    private OrganisationLogoFileDTO convertToOrganisationLogoDTO(OrganisationLogoFile organisationLogoFile) {
        return OrganisationLogoFileDTO.builder()
                .organisationId(organisationLogoFile.getOrganisationId())
                .fileUuid(organisationLogoFile.getFileUuid())
                .originalFilename(organisationLogoFile.getOriginalFilename())
                .filename(organisationLogoFile.getFilename())
                .fileSize(organisationLogoFile.getFileSize())
                .fileType(organisationLogoFile.getFileType())
                .mimeType(organisationLogoFile.getMimeType())
                .contentType(organisationLogoFile.getContentType())
                .filePath(organisationLogoFile.getFilePath())
                .build();
    }
    
    private Path getPhysicalPath(OrganisationLogoFile fileEntity) {
        return Paths.get(rootStoragePath.trim(), "organisations",
                fileEntity.getOrganisationId(), "logo", fileEntity.getFilename());
    }

    private long parseFileSize(String sizeString) {
        if (sizeString.endsWith("MB")) {
            return Long.parseLong(sizeString.replace("MB", "")) * 1024 * 1024;
        } else if (sizeString.endsWith("GB")) {
            return Long.parseLong(sizeString.replace("GB", "")) * 1024 * 1024 * 1024;
        }
        return Long.parseLong(sizeString);
    }
}
