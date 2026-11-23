package com.ordercenter.file.dto;

import com.ordercenter.file.FileAsset;

import java.util.Locale;
import java.util.Set;

public record FileAssetResponse(
        Long id,
        String originalName,
        String contentType,
        Long fileSize,
        boolean image
) {

    private static final Set<String> IMAGE_EXTENSIONS = Set.of(
            "jpg", "jpeg", "png", "gif", "webp", "bmp"
    );

    public static FileAssetResponse from(FileAsset file) {
        String name = file.getOriginalName() == null ? "" : file.getOriginalName();
        int dot = name.lastIndexOf('.');
        String extension = dot >= 0 ? name.substring(dot + 1).toLowerCase(Locale.ROOT) : "";
        boolean image = (file.getContentType() != null
                && file.getContentType().toLowerCase(Locale.ROOT).startsWith("image/"))
                || IMAGE_EXTENSIONS.contains(extension);

        return new FileAssetResponse(
                file.getId(),
                file.getOriginalName(),
                file.getContentType(),
                file.getFileSize(),
                image
        );
    }
}
