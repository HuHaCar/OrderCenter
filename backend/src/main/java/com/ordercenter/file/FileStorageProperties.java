package com.ordercenter.file;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.unit.DataSize;

@Component
@ConfigurationProperties(prefix = "app.file")
public class FileStorageProperties {

    /**
     * 相对于后端启动目录的 uploads 路径。
     * 默认 ../uploads，即项目根目录 uploads。
     */
    private String uploadDir = "../uploads";

    private DataSize maxFileSize = DataSize.ofMegabytes(200);

    private int maxAttachments = 3;

    public String getUploadDir() {
        return uploadDir;
    }

    public void setUploadDir(String uploadDir) {
        this.uploadDir = uploadDir;
    }

    public DataSize getMaxFileSize() {
        return maxFileSize;
    }

    public void setMaxFileSize(DataSize maxFileSize) {
        this.maxFileSize = maxFileSize;
    }

    public int getMaxAttachments() {
        return maxAttachments;
    }

    public void setMaxAttachments(int maxAttachments) {
        this.maxAttachments = maxAttachments;
    }
}
