package com.swizima.filesapi.service;

import java.io.InputStream;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import com.swizima.filesapi.dto.OrganisationLogoFileDTO;
import com.swizima.filesapi.dto.ResponseDTO;

public interface OrganisationLogoFileService {
   ResponseDTO<String> uploadFile(MultipartFile file, String organisationId);
   Resource downloadFile(String fileUuid);
   InputStream streamFile(String fileUuid);
   ResponseDTO<OrganisationLogoFileDTO> getFile(String organisationId);

   Resource generateThumbnail(String fileUuid);
   Resource generatePreview(String fileUuid);
}
