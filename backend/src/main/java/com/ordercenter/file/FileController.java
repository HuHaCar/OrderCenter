package com.ordercenter.file;

import com.ordercenter.auth.AuthPrincipal;
import com.ordercenter.common.ApiResponse;
import com.ordercenter.file.dto.FileAssetResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

@Tag(name = "文件接口")
@RestController
@RequestMapping("/api/files")
public class FileController {

    private final FileStorageService fileStorageService;

    public FileController(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    @Operation(summary = "上传文件")
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<FileAssetResponse> upload(
            @RequestPart("file") MultipartFile file,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        return ApiResponse.ok(
                FileAssetResponse.from(
                        fileStorageService.store(file, principal.userId())
                )
        );
    }

    @Operation(summary = "预览图片")
    @GetMapping("/{fileId}/preview")
    public ResponseEntity<Resource> preview(
            @PathVariable Long fileId,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        FileAsset asset = fileStorageService.requireReadable(fileId, principal);
        if (!fileStorageService.isImage(asset)) {
            throw new IllegalArgumentException("该文件不支持预览");
        }
        return fileResponse(asset, true);
    }

    @Operation(summary = "下载文件")
    @GetMapping("/{fileId}/download")
    public ResponseEntity<Resource> download(
            @PathVariable Long fileId,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        return fileResponse(
                fileStorageService.requireReadable(fileId, principal),
                false
        );
    }

    private ResponseEntity<Resource> fileResponse(FileAsset asset, boolean inline) {
        Path path = fileStorageService.pathOf(asset);
        if (!Files.exists(path)) {
            throw new IllegalArgumentException("文件不存在");
        }

        FileSystemResource resource = new FileSystemResource(path);
        ContentDisposition disposition = (inline
                ? ContentDisposition.inline()
                : ContentDisposition.attachment())
                .filename(asset.getOriginalName(), StandardCharsets.UTF_8)
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentType(parseMediaType(asset.getContentType()))
                .contentLength(asset.getFileSize())
                .body(resource);
    }

    private static MediaType parseMediaType(String value) {
        try {
            return MediaType.parseMediaType(value);
        } catch (RuntimeException exception) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }
}
