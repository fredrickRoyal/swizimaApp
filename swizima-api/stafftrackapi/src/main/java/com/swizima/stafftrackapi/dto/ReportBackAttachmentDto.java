package com.swizima.stafftrackapi.dto;

import com.swizima.stafftrackapi.entity.ReportBackAttachment;
import com.swizima.stafftrackapi.entity.enums.AttachmentKind;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;
import java.util.UUID;

public record ReportBackAttachmentDto(
        UUID id,
        @NotBlank(message = "attachment name is required")
        String name,
        String mimeType,
        long sizeBytes,
        AttachmentKind kind,
        String dataUrl,
        LocalDate uploadedAt
) {

    public static ReportBackAttachmentDto from(ReportBackAttachment entity) {
        return new ReportBackAttachmentDto(
                entity.getId(),
                entity.getName(),
                entity.getMimeType(),
                entity.getSizeBytes(),
                entity.getKind(),
                entity.getDataUrl(),
                entity.getUploadedAt()
        );
    }
}

