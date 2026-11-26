package com.swizima.filesapi.service;

import com.swizima.filesapi.dto.GeneralFileAttachmentDTO;
import com.swizima.filesapi.dto.PaginatedResponseDTO;
import com.swizima.filesapi.dto.ResponseDTO;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

public interface GeneralFileAttachmentService {
    ResponseDTO<String> uploadFile(MultipartFile file, String organisationId, String entityRefId, String category, String subcategory, String subEntityRefId, String attachmentOrigin);
    ResponseDTO<String> uploadMultipleFiles(List<MultipartFile> files, String organisationId, String entityRefId, String category, String subcategory, String subEntityRefId, String attachmentOrigin);
    ResponseDTO<String> uploadUpdateFile(MultipartFile file, String organisationId, String entityRefId, String category, String subcategory, String subEntityRefId, String attachmentOrigin);
    ResponseDTO<String> uploadUpdateMultipleFiles(List<MultipartFile> files, String organisationId, String entityRefId, String category, String subcategory, String subEntityRefId, String attachmentOrigin);
    Resource downloadFile(String fileUuid);
    InputStream streamFile(String fileUuid);
    PaginatedResponseDTO<GeneralFileAttachmentDTO> searchList(Map<String, String> queryMap);

    Resource generateThumbnail(String fileUuid);
    Resource generatePreview(String fileUuid);

    int deleteAllForEntity(String organisationId, String entityRefId);
    ResponseDTO<String> deleteByFileUuid(String fileUuid);
    ResponseDTO<List<GeneralFileAttachmentDTO>> getFilesByEntityRefId(String entityRefId);
}
