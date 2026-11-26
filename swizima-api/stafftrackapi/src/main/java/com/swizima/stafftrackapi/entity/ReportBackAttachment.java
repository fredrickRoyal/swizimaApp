package com.swizima.stafftrackapi.entity;

import com.swizima.stafftrackapi.entity.enums.AttachmentKind;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;

/**
 * Field photo, PDF report, or supporting document attached to a Report Back submission.
 */
@Entity
@Table(name = "report_back_attachments")
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class ReportBackAttachment extends ParentEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "report_back_id", nullable = false)
    private OutOfOfficeReportBack reportBack;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "mime_type", length = 120)
    private String mimeType;

    @Column(name = "size_bytes")
    private long sizeBytes;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "kind", nullable = false, length = 20)
    private AttachmentKind kind = AttachmentKind.DOCUMENT;

    @Column(name = "data_url", columnDefinition = "TEXT")
    private String dataUrl;

    @Column(name = "uploaded_at")
    private LocalDate uploadedAt;
}

