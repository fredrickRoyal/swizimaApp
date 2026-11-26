package com.swizima.filesapi.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import com.swizima.filesapi.service.OrganisationLogoFileService;
import com.swizima.filesapi.dto.ResponseDTO;
import com.swizima.filesapi.dto.OrganisationLogoFileDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.web.bind.annotation.PostMapping;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@RestController
@RequestMapping({
        "/api/v1/files/images",
        "/api/files/images",
        "/files/images"
})
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Organisation Logo File Management", description = "APIs for managing organisation logo file operations")
public class OrganisationLogoFileController {

    private final OrganisationLogoFileService organisationLogoFileService;

    @Operation(summary = "Upload Single File", description = "Upload a single file for a specific request.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "File uploaded successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Bad request - Invalid file or metadata", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class)))
    })
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ResponseDTO<String>> uploadFile(
            @Parameter(description = "File to upload", required = true) @RequestPart("file") MultipartFile file,
            @Parameter(description = "Organisation ID for the file", required = true) @RequestParam("organisationId") String organisationId) {
        try {
            return ResponseEntity.ok(organisationLogoFileService.uploadFile(file, organisationId));
        } catch (Exception e) {
            log.error("Error uploading file: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(ResponseDTO.error("Failed to upload file: " + e.getMessage()));
        }
    }

    @Operation(summary = "Download File", description = "Download a file by its UUID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "File downloaded successfully", content = @Content(mediaType = "application/octet-stream")),
            @ApiResponse(responseCode = "404", description = "File not found", content = @Content(mediaType = "application/json"))
    })
    @GetMapping("/download/{fileUuid}")
    public ResponseEntity<Resource> downloadFile(
            @Parameter(description = "Unique identifier of the file to download", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890", required = true) @PathVariable String fileUuid) {
        try {
            log.info("Downloading file: {}", fileUuid);
            Resource resource = organisationLogoFileService.downloadFile(fileUuid);
            return ResponseEntity.ok()
                    .contentType(MediaTypeFactory.getMediaType(resource).orElse(MediaType.APPLICATION_OCTET_STREAM))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                    .body(resource);
        } catch (Exception e) {
            log.error("Error downloading file: {}", e.getMessage(), e);
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(summary = "Stream File", description = "Stream a file by its UUID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "File streamed successfully", content = @Content(mediaType = "application/octet-stream")),
            @ApiResponse(responseCode = "404", description = "File not found", content = @Content(mediaType = "application/json"))
    })
    @GetMapping("/stream/{fileUuid}")
    public ResponseEntity<Resource> streamFile(
            @Parameter(description = "Unique identifier of the file to stream", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890", required = true) @PathVariable String fileUuid) {
        try {
            log.info("Streaming file: {}", fileUuid);
            Resource resource = organisationLogoFileService.downloadFile(fileUuid);
            return ResponseEntity.ok()
                    .contentType(MediaTypeFactory.getMediaType(resource).orElse(MediaType.APPLICATION_OCTET_STREAM))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                    .body(resource);
        } catch (Exception e) {
            log.error("Error streaming file: {}", e.getMessage(), e);
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(summary = "Search Files", description = "Search and retrieve files based on query parameters.")
    @GetMapping("/{organisationId}")
    public ResponseDTO<OrganisationLogoFileDTO> getFile(
            @Parameter(description = "Organisation ID for the file", required = true) @Valid @NotBlank @NotNull @PathVariable String organisationId) {
        return organisationLogoFileService.getFile(organisationId);
    }

    @Operation(summary = "Generate Thumbnail", description = "Generate a thumbnail for a file by its UUID.")
    @GetMapping("/thumbnail/{fileUuid}")
    public ResponseEntity<Resource> generateThumbnail(
            @Parameter(description = "Unique identifier of the file to generate thumbnail for", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890", required = true) @PathVariable String fileUuid) {
        try {
            log.info("Generating thumbnail for file: {}", fileUuid);
            Resource thumbnail = organisationLogoFileService.generateThumbnail(fileUuid);

            if (thumbnail != null && thumbnail.exists()) {
                return ResponseEntity.ok()
                        .contentType(MediaTypeFactory.getMediaType(thumbnail).orElse(MediaType.APPLICATION_OCTET_STREAM))
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"thumbnail_" + fileUuid + "\"")
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
            Resource preview = organisationLogoFileService.generatePreview(fileUuid);

            if (preview != null && preview.exists()) {
                return ResponseEntity.ok()
                        .contentType(MediaTypeFactory.getMediaType(preview).orElse(MediaType.APPLICATION_OCTET_STREAM))
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"preview_" + fileUuid + "\"")
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
}
