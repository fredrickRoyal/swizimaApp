package com.swizima.filesapi.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class GeneralFileAttachment extends BaseEntity {
    @Column(nullable = false)
    private String organisationId; // organisation folder name
    @Column(nullable = false)
    private String entityRefId; // makes folder name for its files

    @Column(nullable = false)
    private String category;

    private String subcategory;

    private String subEntityRefId; // makes folder name for its files
    private String attachmentOrigin;
}
