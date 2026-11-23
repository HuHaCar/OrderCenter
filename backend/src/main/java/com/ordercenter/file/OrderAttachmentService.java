package com.ordercenter.file;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class OrderAttachmentService {

    private final FileAssetRepository fileAssetRepository;
    private final OrderAttachmentRepository orderAttachmentRepository;
    private final FileStorageProperties properties;

    public OrderAttachmentService(
            FileAssetRepository fileAssetRepository,
            OrderAttachmentRepository orderAttachmentRepository,
            FileStorageProperties properties
    ) {
        this.fileAssetRepository = fileAssetRepository;
        this.orderAttachmentRepository = orderAttachmentRepository;
        this.properties = properties;
    }

    @Transactional
    public void attachToOrder(
            Long orderId,
            List<Long> attachmentIds,
            Long currentUserId
    ) {
        if (attachmentIds == null || attachmentIds.isEmpty()) {
            return;
        }

        List<Long> distinctIds = attachmentIds.stream().distinct().toList();
        if (distinctIds.size() > properties.getMaxAttachments()) {
            throw new IllegalArgumentException(
                    "最多只能上传 " + properties.getMaxAttachments() + " 个附件"
            );
        }

        List<FileAsset> files = fileAssetRepository.findAllById(distinctIds);
        if (files.size() != distinctIds.size()) {
            throw new IllegalArgumentException("部分附件不存在，请重新上传");
        }

        if (files.stream().anyMatch(file -> !file.getUploadedBy().equals(currentUserId))) {
            throw new IllegalArgumentException("不能关联其他用户上传的附件");
        }

        List<OrderAttachment> attachments = distinctIds.stream()
                .map(fileId -> {
                    OrderAttachment attachment = new OrderAttachment();
                    attachment.setOrderId(orderId);
                    attachment.setFileId(fileId);
                    return attachment;
                })
                .toList();

        orderAttachmentRepository.saveAll(attachments);
    }
}
