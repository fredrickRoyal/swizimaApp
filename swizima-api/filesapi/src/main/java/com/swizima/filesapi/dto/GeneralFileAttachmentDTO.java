package com.swizima.filesapi.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Data
@EqualsAndHashCode(callSuper = true)
public class GeneralFileAttachmentDTO extends FileDTO {
    private String organisationId; // organisation folder name
    private String entityRefId; // makes folder name for its files
    private String category;
    private String subcategory;

    private String subEntityRefId;
    private String attachmentOrigin;
    private String date;

}
