package com.swizima.filesapi.service;

import com.swizima.filesapi.dto.GeneralFileAttachmentDTO;
import com.swizima.filesapi.dto.PaginatedResponseDTO;
import com.swizima.filesapi.dto.ResponseDTO;
import com.swizima.filesapi.entity.GeneralFileAttachment;
import com.swizima.filesapi.enums.ContentType;
import com.swizima.filesapi.repository.GeneralFileAttachmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import jakarta.transaction.Transactional;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class GeneralFileAttachmentServiceImpl implements GeneralFileAttachmentService {

    private final GeneralFileAttachmentRepository generalFileAttachmentRepository;
    private final Tika tika = new Tika();

    @Value("${file.storage.root-path}")
    private String rootStoragePath;

    @Value("${file.storage.max-file-size}")
    private String maxFileSize;

    @Value("${file.storage.allowed-extensions}")
    private String allowedExtensions;

    @Override
    @Transactional
    public ResponseDTO<String> uploadFile(MultipartFile file, String organisationId, String entityRefId,
            String category, String subcategory, String subEntityRefId, String attachmentOrigin) {
        try {
            validateFile(file);

            String originalFilename = file.getOriginalFilename();

            deleteDuplicates(organisationId, entityRefId, originalFilename);

            String fileUuid = null;

            do {
                fileUuid = UUID.randomUUID().toString();
            } while (generalFileAttachmentRepository.existsByFileUuid(fileUuid));

            String fileExtension = getFileExtension(originalFilename);
            String uniqueFilename = fileUuid + "." + fileExtension;
            String safeOrgId = (organisationId == null || organisationId.trim().isEmpty()) ? "shared" : organisationId.trim();
            String safeEntityRefId = (entityRefId == null || entityRefId.trim().isEmpty()) ? "general" : entityRefId.trim();

            Path projectStoragePath = Paths.get(rootStoragePath.trim(), "organisations", safeOrgId,
                    "generalattachments", safeEntityRefId);
            Files.createDirectories(projectStoragePath);

            Path filePath = projectStoragePath.resolve(uniqueFilename);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
            if (!Files.exists(filePath)) {
                throw new RuntimeException("File write could not be verified on disk: " + filePath);
            }

            String mimeType = tika.detect(filePath.toFile());
            ContentType contentType = determineContentType(fileExtension, mimeType);

            GeneralFileAttachment fileEntity = GeneralFileAttachment.builder()
                    .originalFilename(originalFilename)
                    .filename(uniqueFilename)
                    .filePath(filePath.toString())
                    .fileSize(file.getSize())
                    .fileType(fileExtension)
                    .mimeType(mimeType)
                    .contentType(contentType)
                    .entityRefId(safeEntityRefId)
                    .organisationId(safeOrgId)
                    .fileUuid(fileUuid)
                    .category(category)
                    .subcategory(subcategory)
                    .subEntityRefId(subEntityRefId)
                    .attachmentOrigin(attachmentOrigin)
                    .build();

            GeneralFileAttachment savedEntity = generalFileAttachmentRepository.save(fileEntity);

            log.info("File uploaded successfully: {}", originalFilename);

            return ResponseDTO.success(savedEntity.getFileUuid());

        } catch (Exception e) {
            log.error("Error uploading file: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to upload file: " + e.getMessage());
        }
    }

    @Override
    public ResponseDTO<String> uploadUpdateFile(MultipartFile file, String organisationId, String entityRefId,
            String category, String subcategory, String subEntityRefId, String attachmentOrigin) {
        try {
            validateFile(file);

            String originalFilename = file.getOriginalFilename();

            Optional<GeneralFileAttachment> existingFile = generalFileAttachmentRepository
                    .findByOrganisationIdAndEntityRefIdAndOriginalFilenameIgnoreCase(organisationId, entityRefId,
                            originalFilename);

            if (existingFile.isEmpty()) {
                return uploadFile(file, organisationId, entityRefId, category, subcategory, subEntityRefId,
                        attachmentOrigin);
            }

            GeneralFileAttachment existingFileAttachment = existingFile.get();
            String fileExtension = getFileExtension(originalFilename);
            String uniqueFilename = existingFileAttachment.getFileUuid() + "." + fileExtension;
            String safeOrgId = (organisationId == null || organisationId.trim().isEmpty()) ? "shared" : organisationId.trim();
            String safeEntityRefId = (entityRefId == null || entityRefId.trim().isEmpty()) ? "general" : entityRefId.trim();

            Path projectStoragePath = Paths.get(rootStoragePath.trim(), "organisations", safeOrgId,
                    "generalattachments", safeEntityRefId);
            Files.createDirectories(projectStoragePath);

            Path filePath = projectStoragePath.resolve(uniqueFilename);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
            if (!Files.exists(filePath)) {
                throw new RuntimeException("File write could not be verified on disk: " + filePath);
            }

            String mimeType = tika.detect(filePath.toFile());
            ContentType contentType = determineContentType(fileExtension, mimeType);

            existingFileAttachment.setFileSize(file.getSize());
            existingFileAttachment.setMimeType(mimeType);
            existingFileAttachment.setContentType(contentType);
            existingFileAttachment.setFileType(fileExtension);
            existingFileAttachment.setFilename(uniqueFilename);
            existingFileAttachment.setFilePath(filePath.toString());
            existingFileAttachment.setEntityRefId(entityRefId);
            existingFileAttachment.setCategory(category);
            existingFileAttachment.setSubcategory(subcategory);
            existingFileAttachment.setAttachmentOrigin(attachmentOrigin);
            existingFileAttachment.setSubEntityRefId(subEntityRefId);

            GeneralFileAttachment savedEntity = generalFileAttachmentRepository.save(existingFileAttachment);

            log.info("File updated successfully: {}", originalFilename);

            return ResponseDTO.success(savedEntity.getFileUuid());

        } catch (Exception e) {
            log.error("Error updating file: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to update file: " + e.getMessage());
        }
    }

    @Override
    public ResponseDTO<String> uploadMultipleFiles(List<MultipartFile> files, String organisationId, String entityRefId,
            String category, String subcategory, String subEntityRefId, String attachmentOrigin) {
        try {
            if (files == null || files.isEmpty()) {
                throw new RuntimeException("No files provided for upload");
            }

            files.forEach(file ->
                    uploadFile(file, organisationId, entityRefId, category, subcategory, subEntityRefId, attachmentOrigin));

            return ResponseDTO.success("Multiple files uploaded successfully", "Multiple files uploaded successfully");

        } catch (Exception e) {
            log.error("Error in multiple file upload: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to upload multiple files: " + e.getMessage());
        }
    }

    @Override
    public ResponseDTO<String> uploadUpdateMultipleFiles(List<MultipartFile> files, String organisationId,
            String entityRefId, String category, String subcategory, String subEntityRefId, String attachmentOrigin) {
        try {
            if (files == null || files.isEmpty()) {
                throw new RuntimeException("No files provided for upload");
            }

            files.forEach(file ->
                    uploadUpdateFile(file, organisationId, entityRefId, category, subcategory, subEntityRefId, attachmentOrigin));

            return ResponseDTO.success("Multiple files updated successfully", "Multiple files updated successfully");

        } catch (Exception e) {
            log.error("Error in multiple file update: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to update multiple files: " + e.getMessage());
        }
    }

    @Override
    public Resource downloadFile(String fileUuid) {
        GeneralFileAttachment fileEntity = generalFileAttachmentRepository.findByFileUuid(fileUuid)
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
            GeneralFileAttachment fileEntity = generalFileAttachmentRepository.findByFileUuid(fileUuid)
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
    public PaginatedResponseDTO<GeneralFileAttachmentDTO> searchList(Map<String, String> queryMap) {
        int page = Integer.parseInt(queryMap.getOrDefault("page", "0"));
        int size = Integer.parseInt(queryMap.getOrDefault("size", "10"));
        String sortBy = queryMap.getOrDefault("sortBy", "originalFilename");
        String sortDirection = queryMap.getOrDefault("sortDirection", "asc");
        String entityRefId = queryMap.getOrDefault("entityRefId", null);
        String organisationId = queryMap.getOrDefault("organisationId", null);

        Sort sort = Sort.by(Sort.Direction.fromString(sortDirection), sortBy);
        PageRequest pageable = PageRequest.of(page == 0 ? 0 : page - 1, size, sort);

        Page<GeneralFileAttachment> filePage;
        if (organisationId != null && !organisationId.isEmpty() && entityRefId != null && !entityRefId.isEmpty()) {
            filePage = generalFileAttachmentRepository.findByOrganisationIdAndEntityRefId(organisationId, entityRefId,
                    pageable);
        } else if (organisationId != null && !organisationId.isEmpty()) {
            filePage = generalFileAttachmentRepository.findByOrganisationId(organisationId, pageable);
        } else if (entityRefId != null && !entityRefId.isEmpty()) {
            filePage = generalFileAttachmentRepository.findByEntityRefId(entityRefId, pageable);
        } else {
            filePage = generalFileAttachmentRepository.findAll(pageable);
        }

        List<GeneralFileAttachmentDTO> fileDTOs = filePage.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        PaginatedResponseDTO<GeneralFileAttachmentDTO> paginatedResponseDTO = new PaginatedResponseDTO<>();
        paginatedResponseDTO.setPage(page);
        paginatedResponseDTO.setSize(size);
        paginatedResponseDTO.setTotalElements(filePage.getTotalElements());
        paginatedResponseDTO.setTotalPages(filePage.getTotalPages());
        paginatedResponseDTO.setData(fileDTOs);
        paginatedResponseDTO.setHasNext(filePage.hasNext());
        paginatedResponseDTO.setHasPrevious(filePage.hasPrevious());
        paginatedResponseDTO.setMessage("Files fetched successfully");
        paginatedResponseDTO.setSuccess(true);
        return paginatedResponseDTO;
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
            GeneralFileAttachment fileEntity = generalFileAttachmentRepository.findByFileUuid(fileUuid)
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
            GeneralFileAttachment fileEntity = generalFileAttachmentRepository.findByFileUuid(fileUuid)
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
        if (mimeType.startsWith("image/"))
            return ContentType.IMAGE;
        if (mimeType.startsWith("video/"))
            return ContentType.VIDEO;
        if (mimeType.startsWith("audio/"))
            return ContentType.AUDIO;
        if (mimeType.startsWith("text/"))
            return ContentType.OTHER;

        switch (extension.toLowerCase()) {
            case "pdf", "doc", "docx":
                return ContentType.DOCUMENT;
            case "xls", "xlsx":
                return ContentType.DOCUMENT;
            case "ppt", "pptx":
                return ContentType.DOCUMENT;
            case "zip", "rar", "7z":
                return ContentType.OTHER;
            default:
                return ContentType.OTHER;
        }
    }

    private void deleteDuplicates(String organisationId, String entityRefId, String originalFilename) {
        List<GeneralFileAttachment> duplicates = generalFileAttachmentRepository
                .findAllByOrganisationIdAndEntityRefIdAndOriginalFilenameIgnoreCase(organisationId, entityRefId,
                        originalFilename);
        if (!duplicates.isEmpty()) {
            log.info("Removing {} duplicate(s) for filename '{}' in entity '{}'",
                    duplicates.size(), originalFilename, entityRefId);
            duplicates.forEach(this::deleteFileFromStorage);
            generalFileAttachmentRepository.deleteAll(duplicates);
        }
    }

    @Override
    @Transactional
    public int deleteAllForEntity(String organisationId, String entityRefId) {
        List<GeneralFileAttachment> files = generalFileAttachmentRepository
                .findByOrganisationIdAndEntityRefId(organisationId, entityRefId, Pageable.unpaged())
                .getContent();
        if (files.isEmpty()) {
            return 0;
        }
        files.forEach(this::deleteFileFromStorage);
        generalFileAttachmentRepository.deleteAll(files);
        log.info("Deleted {} general attachment(s) for orgId={} entityRefId={}",
                files.size(), organisationId, entityRefId);
        return files.size();
    }

    @Override
    @Transactional
    public ResponseDTO<String> deleteByFileUuid(String fileUuid) {
        GeneralFileAttachment fileEntity = generalFileAttachmentRepository.findByFileUuid(fileUuid)
                .orElseThrow(() -> new RuntimeException("File not found with UUID: " + fileUuid));
        deleteFileFromStorage(fileEntity);
        generalFileAttachmentRepository.delete(fileEntity);
        log.info("Deleted general attachment fileUuid={}", fileUuid);
        return ResponseDTO.success("File deleted successfully", fileUuid);
    }

    private void deleteFileFromStorage(GeneralFileAttachment attachment) {
        try {
            Path filePath = Paths.get(attachment.getFilePath());
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            log.error("Failed to delete file from storage: {}", e.getMessage());
        }
    }

    private GeneralFileAttachmentDTO convertToDTO(GeneralFileAttachment generalFileAttachment) {

        return GeneralFileAttachmentDTO.builder()
                .entityRefId(generalFileAttachment.getEntityRefId())
                .category(generalFileAttachment.getCategory())
                .subcategory(generalFileAttachment.getSubcategory())
                .fileUuid(generalFileAttachment.getFileUuid())
                .originalFilename(generalFileAttachment.getOriginalFilename())
                .filename(generalFileAttachment.getFilename())
                .fileSize(generalFileAttachment.getFileSize())
                .fileType(generalFileAttachment.getFileType())
                .mimeType(generalFileAttachment.getMimeType())
                .contentType(generalFileAttachment.getContentType().name())
                .filePath(generalFileAttachment.getFilePath())
                .subEntityRefId(generalFileAttachment.getSubEntityRefId())
                .attachmentOrigin(generalFileAttachment.getAttachmentOrigin())
                .date(generalFileAttachment.getCreatedDateTime() != null ?
                        generalFileAttachment.getCreatedDateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : null)
                .build();
    }

    private long parseFileSize(String sizeString) {
        if (sizeString.endsWith("MB")) {
            return Long.parseLong(sizeString.replace("MB", "")) * 1024 * 1024;
        } else if (sizeString.endsWith("GB")) {
            return Long.parseLong(sizeString.replace("GB", "")) * 1024 * 1024 * 1024;
        }
        return Long.parseLong(sizeString);
    }

    private Path getPhysicalPath(GeneralFileAttachment fileEntity) {
        return Paths.get(rootStoragePath.trim(), "organisations",
                fileEntity.getOrganisationId(), "generalattachments",
                fileEntity.getEntityRefId(), fileEntity.getFilename());
    }

    @Override
    public ResponseDTO<List<GeneralFileAttachmentDTO>> getFilesByEntityRefId(String entityRefId) {
        List<GeneralFileAttachment> list = generalFileAttachmentRepository.findByEntityRefId(entityRefId);
        List<GeneralFileAttachmentDTO> dtoList = list.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        return ResponseDTO.success(dtoList, "Files fetched successfully");
    }

}
