package com.swizima.filesapi.controller;

import com.swizima.filesapi.dto.GeneralFileAttachmentDTO;
import com.swizima.filesapi.dto.PaginatedResponseDTO;
import com.swizima.filesapi.dto.ResponseDTO;
import com.swizima.filesapi.service.GeneralFileAttachmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping({
        "/api/v1/files/generalattachments",
        "/api/v1/files/attachments",
        "/api/files/generalattachments",
        "/api/files/attachments",
        "/files/generalattachments",
        "/files/attachments"
})
@RequiredArgsConstructor
@Slf4j
@Tag(name = "General File Attachments Management", description = "APIs for managing general file attachments")
public class GeneralFileAttachmentController {

        private final GeneralFileAttachmentService generalFileAttachmentService;

        @Operation(summary = "Upload Single File", description = "Upload a single file for a specific organisation General File Attachments.")
        @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
        public ResponseEntity<ResponseDTO<String>> uploadFile(
                        @Parameter(description = "File to upload", required = true) @RequestPart("file") MultipartFile file,
                        @Parameter(description = "Organisation ID for the file", required = false) @RequestParam(value = "organisationRef", required = false) String organisationRef,
                        @Parameter(description = "Organisation ID for the file", required = false) @RequestParam(value = "organisationId", required = false) String organisationId,
                        @Parameter(description = "entityRefId for the file", required = true) @RequestParam("entityRefId") String entityRefId,
                        @RequestParam("category") String category, @RequestParam("subcategory") String subcategory,
                        @RequestParam("subEntityRefId") String subEntityRefId,
                        @RequestParam("attachmentOrigin") String attachmentOrigin) {
                try {
                        if (organisationRef != null && !organisationRef.isBlank()) {
                                return ResponseEntity.ok(generalFileAttachmentService.uploadFile(file, organisationRef,
                                                entityRefId, category, subcategory, subEntityRefId, attachmentOrigin));
                        } else {
                                return ResponseEntity.ok(generalFileAttachmentService.uploadFile(file, organisationId,
                                                entityRefId, category, subcategory, subEntityRefId, attachmentOrigin));
                        }
                } catch (Exception e) {
                        log.error("Error uploading file: {}", e.getMessage(), e);
                        return ResponseEntity.badRequest()
                                        .body(ResponseDTO.error("Failed to upload file: " + e.getMessage()));
                }
        }

        @Operation(summary = "Upload Multiple Files", description = "Upload multiple files.")
        @PostMapping(value = "/upload/multiple", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
        public ResponseEntity<ResponseDTO<String>> uploadMultipleFiles(
                        @Parameter(description = "List of files to upload", required = true) @RequestPart("files") List<MultipartFile> files,
                        @Parameter(description = "Organisation ID for the files", required = true) @RequestParam("organisationId") String organisationId,
                        @Parameter(description = "Project ID for the files", required = true) @RequestParam("entityRefId") String entityRefId,
                        @RequestParam("category") String category, @RequestParam("subcategory") String subcategory,
                        @RequestParam("subEntityRefId") String subEntityRefId,
                        @RequestParam("attachmentOrigin") String attachmentOrigin) {
                try {
                        return ResponseEntity.ok(generalFileAttachmentService.uploadMultipleFiles(files, organisationId,
                                        entityRefId, category, subcategory, subEntityRefId, attachmentOrigin));
                } catch (Exception e) {
                        log.error("Error uploading multiple files: {}", e.getMessage(), e);
                        return ResponseEntity.badRequest()
                                        .body(ResponseDTO.error("Failed to upload files: " + e.getMessage()));
                }
        }

        @Operation(summary = "Update File", description = "Update an existing file.")
        @PutMapping(value = "/update", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
        public ResponseEntity<ResponseDTO<String>> updateFile(
                        @Parameter(description = "File to update", required = true) @RequestPart("file") MultipartFile file,
                        @Parameter(description = "Organisation ID for the file", required = true) @RequestParam("organisationId") String organisationId,
                        @Parameter(description = "Project ID for the file", required = true) @RequestParam("entityRefId") String entityRefId,
                        @RequestParam("category") String category, @RequestParam("subcategory") String subcategory,
                        @RequestParam("subEntityRefId") String subEntityRefId,
                        @RequestParam("attachmentOrigin") String attachmentOrigin) {
                try {
                        return ResponseEntity.ok(generalFileAttachmentService.uploadUpdateFile(file, organisationId,
                                        entityRefId, category, subcategory, subEntityRefId, attachmentOrigin));
                } catch (Exception e) {
                        log.error("Error updating file: {}", e.getMessage(), e);
                        return ResponseEntity.badRequest()
                                        .body(ResponseDTO.error("Failed to update file: " + e.getMessage()));
                }
        }

        @Operation(summary = "Update Multiple Files", description = "Update multiple existing files.")
        @PutMapping(value = "/update/multiple", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
        public ResponseEntity<ResponseDTO<String>> updateMultipleFiles(
                        @Parameter(description = "List of files to update", required = true) @RequestPart("files") List<MultipartFile> files,
                        @Parameter(description = "Organisation ID for the files", required = true) @RequestParam("organisationId") String organisationId,
                        @Parameter(description = "Project ID for the files", required = true) @RequestParam("entityRefId") String entityRefId,
                        @RequestParam("category") String category, @RequestParam("subcategory") String subcategory,
                        @RequestParam("subEntityRefId") String subEntityRefId,
                        @RequestParam("attachmentOrigin") String attachmentOrigin) {
                try {
                        return ResponseEntity.ok(generalFileAttachmentService.uploadUpdateMultipleFiles(files,
                                        organisationId, entityRefId, category, subcategory, subEntityRefId,
                                        attachmentOrigin));
                } catch (Exception e) {
                        log.error("Error updating multiple files: {}", e.getMessage(), e);
                        return ResponseEntity.badRequest()
                                        .body(ResponseDTO.error("Failed to update files: " + e.getMessage()));
                }
        }

        @Operation(summary = "Download File", description = "Download a file by its UUID.")
        @GetMapping("/download/{fileUuid}")
        public ResponseEntity<Resource> downloadFile(
                        @Parameter(description = "Unique identifier of the file to download", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890", required = true) @PathVariable String fileUuid) {
                try {
                        log.info("Downloading file: {}", fileUuid);
                        Resource resource = generalFileAttachmentService.downloadFile(fileUuid);
                        return ResponseEntity.ok()
                                        .contentType(MediaTypeFactory.getMediaType(resource)
                                                        .orElse(MediaType.APPLICATION_OCTET_STREAM))
                                        .header(HttpHeaders.CONTENT_DISPOSITION,
                                                        "attachment; filename=\"" + resource.getFilename() + "\"")
                                        .body(resource);
                } catch (Exception e) {
                        log.error("Error downloading file: {}", e.getMessage(), e);
                        return ResponseEntity.notFound().build();
                }
        }

        @Operation(summary = "Stream File", description = "Stream a file by its UUID.")
        @GetMapping("/stream/{fileUuid}")
        public ResponseEntity<Resource> streamFile(
                        @Parameter(description = "Unique identifier of the file to stream", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890", required = true) @PathVariable String fileUuid) {
                try {
                        log.info("Streaming file: {}", fileUuid);
                        Resource resource = generalFileAttachmentService.downloadFile(fileUuid);
                        return ResponseEntity.ok()
                                        .contentType(MediaTypeFactory.getMediaType(resource)
                                                        .orElse(MediaType.APPLICATION_OCTET_STREAM))
                                        .header(HttpHeaders.CONTENT_DISPOSITION,
                                                        "inline; filename=\"" + resource.getFilename() + "\"")
                                        .body(resource);
                } catch (Exception e) {
                        log.error("Error streaming file: {}", e.getMessage(), e);
                        return ResponseEntity.notFound().build();
                }
        }

        @Operation(summary = "Search Files", description = "Search and retrieve files based on query parameters.")
        @GetMapping("/search")
        public PaginatedResponseDTO<GeneralFileAttachmentDTO> SearchList(
                        @Parameter(description = "Query parameters for filtering files") @RequestParam Map<String, String> queryMap) {
                return generalFileAttachmentService.searchList(queryMap);
        }

        @Operation(summary = "Generate Thumbnail", description = "Generate a thumbnail for a file by its UUID.")
        @GetMapping("/thumbnail/{fileUuid}")
        public ResponseEntity<Resource> generateThumbnail(
                        @Parameter(description = "Unique identifier of the file to generate thumbnail for", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890", required = true) @PathVariable String fileUuid) {
                try {
                        log.info("Generating thumbnail for file: {}", fileUuid);
                        Resource thumbnail = generalFileAttachmentService.generateThumbnail(fileUuid);

                        if (thumbnail != null && thumbnail.exists()) {
                                return ResponseEntity.ok()
                                                .contentType(MediaTypeFactory.getMediaType(thumbnail)
                                                                .orElse(MediaType.APPLICATION_OCTET_STREAM))
                                                .header(HttpHeaders.CONTENT_DISPOSITION,
                                                                "inline; filename=\"thumbnail_" + fileUuid + "\"")
                                                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=3600")
                                                .body(thumbnail);
                        } else {
                                log.warn("Thumbnail not found for file: {}", fileUuid);
                                return ResponseEntity.notFound().build();
                        }
                } catch (Exception e) {
                        log.error("Error generating thumbnail for file {}: {}", fileUuid, e.getMessage(), e);
                        return ResponseEntity.internalServerError().build();
                }
        }

        @Operation(summary = "Generate Preview", description = "Generate a preview for a file by its UUID.")
        @GetMapping("/preview/{fileUuid}")
        public ResponseEntity<Resource> generatePreview(
                        @Parameter(description = "Unique identifier of the file to generate preview for", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890", required = true) @PathVariable String fileUuid) {
                try {
                        log.info("Generating preview for file: {}", fileUuid);
                        Resource preview = generalFileAttachmentService.generatePreview(fileUuid);

                        if (preview != null && preview.exists()) {
                                return ResponseEntity.ok()
                                                .contentType(MediaTypeFactory.getMediaType(preview)
                                                                .orElse(MediaType.APPLICATION_OCTET_STREAM))
                                                .header(HttpHeaders.CONTENT_DISPOSITION,
                                                                "inline; filename=\"preview_" + fileUuid + "\"")
                                                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=3600")
                                                .body(preview);
                        } else {
                                log.warn("Preview not found for file: {}", fileUuid);
                                return ResponseEntity.notFound().build();
                        }
                } catch (Exception e) {
                        log.error("Error generating preview for file {}: {}", fileUuid, e.getMessage(), e);
                        return ResponseEntity.internalServerError().build();
                }
        }

        @Operation(summary = "Delete File by UUID", description = "Delete a general attachment file by its unique UUID.")
        @DeleteMapping("/{fileUuid}")
        public ResponseEntity<ResponseDTO<String>> deleteFile(
                        @Parameter(description = "Unique identifier of the file to delete", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890", required = true) @PathVariable String fileUuid) {
                try {
                        log.info("Deleting file: {}", fileUuid);
                        return ResponseEntity.ok(generalFileAttachmentService.deleteByFileUuid(fileUuid));
                } catch (Exception e) {
                        log.error("Error deleting file {}: {}", fileUuid, e.getMessage(), e);
                        return ResponseEntity.badRequest()
                                        .body(ResponseDTO.error("Failed to delete file: " + e.getMessage()));
                }
        }

        @Operation(summary = "Get Files by EntityRefId", description = "Retrieve all files for a specific entity reference ID.")
        @GetMapping("/entity/{entityRefId}")
        public ResponseEntity<ResponseDTO<List<GeneralFileAttachmentDTO>>> getFilesByEntityRefId(
                        @Parameter(description = "Entity reference ID", required = true) @PathVariable("entityRefId") String entityRefId) {
                try {
                        return ResponseEntity.ok(generalFileAttachmentService.getFilesByEntityRefId(entityRefId));
                } catch (Exception e) {
                        log.error("Error fetching files by entityRefId {}: {}", entityRefId, e.getMessage(), e);
                        return ResponseEntity.badRequest()
                                        .body(ResponseDTO.error("Failed to fetch files: " + e.getMessage()));
                }
        }
}
