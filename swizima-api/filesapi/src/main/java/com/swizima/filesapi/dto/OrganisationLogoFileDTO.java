package com.swizima.filesapi.dto;

import com.swizima.filesapi.entity.BaseEntity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class OrganisationLogoFileDTO extends BaseEntity {
    private String organisationId;
}
