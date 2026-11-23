package com.ordercenter.file;

import com.ordercenter.auth.AuthPrincipal;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class FileStorageService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "jpg", "jpeg", "png", "gif", "webp", "bmp",
            "doc", "docx", "xls", "xlsx",
            "zip", "rar", "7z", "tar", "gz"
    );

    private static final Set<String> IMAGE_EXTENSIONS = Set.of(
            "jpg", "jpeg", "png", "gif", "webp", "bmp"
    );

    private final FileStorageProperties properties;
    private final FileAssetRepository fileAssetRepository;
    private Path root;

    public FileStorageService(
            FileStorageProperties properties,
            FileAssetRepository fileAssetRepository
    ) {
        this.properties = properties;
        this.fileAssetRepository = fileAssetRepository;
    }

    @PostConstruct
    void initialize() {
        root = Path.of(properties.getUploadDir()).toAbsolutePath().normalize();
        try {
            Files.createDirectories(root);
        } catch (IOException exception) {
            throw new IllegalStateException("无法创建文件存储目录：" + root, exception);
        }
    }

    @Transactional
    public FileAsset store(MultipartFile file, Long uploadedBy) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("上传文件不能为空");
        }
        if (file.getSize() > properties.getMaxFileSize().toBytes()) {
            throw new IllegalArgumentException(
                    "文件大小不能超过 " + properties.getMaxFileSize().toMegabytes() + "MB"
            );
        }

        String originalName = sanitizeOriginalName(file.getOriginalFilename());
        String extension = extensionOf(originalName);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("不支持的文件类型");
        }

        String contentType = resolveContentType(file, originalName);
        String datePath = LocalDate.now(ZoneId.of("Asia/Shanghai"))
                .toString()
                .replace("-", "/");
        String storedName = UUID.randomUUID() + "." + extension;
        String storageKey = datePath + "/" + storedName;
        Path target = resolveStoragePath(storageKey);

        try {
            Files.createDirectories(target.getParent());
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream input = new DigestInputStream(file.getInputStream(), digest)) {
                Files.copy(input, target);
            }

            FileAsset asset = new FileAsset();
            asset.setOriginalName(originalName);
            asset.setStorageKey(storageKey);
            asset.setContentType(contentType);
            asset.setFileSize(file.getSize());
            asset.setSha256(HexFormat.of().formatHex(digest.digest()));
            asset.setUploadedBy(uploadedBy);

            try {
                return fileAssetRepository.save(asset);
            } catch (RuntimeException exception) {
                Files.deleteIfExists(target);
                throw exception;
            }
        } catch (IOException exception) {
            try {
                Files.deleteIfExists(target);
            } catch (IOException ignored) {
                // 保留原始异常
            }
            throw new IllegalStateException("文件保存失败", exception);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("文件哈希初始化失败", exception);
        }
    }

    public FileAsset requireReadable(Long fileId, AuthPrincipal principal) {
        FileAsset asset = fileAssetRepository.findById(fileId)
                .orElseThrow(() -> new IllegalArgumentException("文件不存在"));

        boolean owner = principal != null && principal.userId().equals(asset.getUploadedBy());
        boolean admin = principal != null && "ADMIN".equals(principal.role());
        if (!owner && !admin) {
            throw new IllegalArgumentException("没有权限访问该文件");
        }
        return asset;
    }

    public Path pathOf(FileAsset asset) {
        return resolveStoragePath(asset.getStorageKey());
    }

    public boolean isImage(FileAsset asset) {
        String contentType = asset.getContentType();
        if (contentType != null && contentType.toLowerCase(Locale.ROOT).startsWith("image/")) {
            return true;
        }
        return IMAGE_EXTENSIONS.contains(extensionOf(asset.getOriginalName()));
    }

    private Path resolveStoragePath(String storageKey) {
        Path path = root.resolve(storageKey).normalize();
        if (!path.startsWith(root)) {
            throw new IllegalArgumentException("非法文件路径");
        }
        return path;
    }

    private static String sanitizeOriginalName(String originalName) {
        String value = StringUtils.hasText(originalName) ? originalName : "file";
        value = StringUtils.cleanPath(value);
        int slash = Math.max(value.lastIndexOf('/'), value.lastIndexOf('\\'));
        value = slash >= 0 ? value.substring(slash + 1) : value;
        if (!StringUtils.hasText(value) || value.contains("..")) {
            value = "file";
        }
        return value.length() > 255 ? value.substring(value.length() - 255) : value;
    }

    private static String extensionOf(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot < 0 ? "" : fileName.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private static String resolveContentType(MultipartFile file, String originalName) {
        String contentType = file.getContentType();
        if (!StringUtils.hasText(contentType)) {
            contentType = URLConnection.guessContentTypeFromName(originalName);
        }
        if (!StringUtils.hasText(contentType)) {
            contentType = "application/octet-stream";
        }
        return contentType.length() > 100 ? contentType.substring(0, 100) : contentType;
    }
}
