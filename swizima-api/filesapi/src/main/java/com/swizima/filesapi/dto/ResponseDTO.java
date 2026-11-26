package com.swizima.filesapi.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Generic API Response Wrapper")
public class ResponseDTO<T> {
    
    @Schema(description = "Indicates if the operation was successful", example = "true")
    private boolean success;
    
    @Schema(description = "Response message", example = "Operation completed successfully")
    private String message;
    
    @Schema(description = "Response data payload")
    private T data;
    
    public ResponseDTO(T data) {
        this.data = data;
    }
    
    public static <T> ResponseDTO<T> success(T data) {
        return ResponseDTO.<T>builder()
                .success(true)
                .data(data)
                .build();
    }
    
    public static <T> ResponseDTO<T> success(T data, String message) {
        return ResponseDTO.<T>builder()
                .success(true)
                .data(data)
                .message(message)
                .build();
    }
    
    public static <T> ResponseDTO<T> error(String message) {
        return ResponseDTO.<T>builder()
                .success(false)
                .message(message)
                .build();
    }
    
    public static <T> ResponseDTO<T> error(String message, String errorCode) {
        return ResponseDTO.<T>builder()
                .success(false)
                .message(message)
                .build();
    }
}
