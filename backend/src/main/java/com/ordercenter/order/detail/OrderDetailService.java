package com.ordercenter.order.detail;

import com.ordercenter.file.FileAsset;
import com.ordercenter.file.FileAssetRepository;
import com.ordercenter.file.OrderAttachment;
import com.ordercenter.file.OrderAttachmentRepository;
import com.ordercenter.file.dto.FileAssetResponse;
import com.ordercenter.order.Order;
import com.ordercenter.order.OrderNotFoundException;
import com.ordercenter.order.OrderRepository;
import com.ordercenter.order.dto.OrderDetailResponse;
import com.ordercenter.shipping.ShippingMethodCatalog;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class OrderDetailService {

    private final OrderRepository orderRepository;
    private final OrderAttachmentRepository orderAttachmentRepository;
    private final FileAssetRepository fileAssetRepository;
    private final ShippingMethodCatalog shippingMethodCatalog;
    private final OrderDetailCacheService cacheService;

    public OrderDetailService(
            OrderRepository orderRepository,
            OrderAttachmentRepository orderAttachmentRepository,
            FileAssetRepository fileAssetRepository,
            ShippingMethodCatalog shippingMethodCatalog,
            OrderDetailCacheService cacheService
    ) {
        this.orderRepository = orderRepository;
        this.orderAttachmentRepository = orderAttachmentRepository;
        this.fileAssetRepository = fileAssetRepository;
        this.shippingMethodCatalog = shippingMethodCatalog;
        this.cacheService = cacheService;
    }

    public OrderDetailResponse getDetail(Long orderId, Long currentUserId) {
        // 先查 Redis，缓存命中时仍需校验订单归属
        OrderDetailResponse cached = cacheService.get(orderId);
        if (cached != null) {
            assertOwner(cached.createBy(), currentUserId);
            return cached;
        }

        // 缓存未命中，查询订单主表
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
        assertOwner(order.getCreateBy(), currentUserId);

        // 查询订单附件关系
        List<OrderAttachment> links = orderAttachmentRepository.findByOrderId(orderId);
        List<Long> fileIds = links.stream()
                .map(OrderAttachment::getFileId)
                .toList();

        // 批量查询文件元数据，避免逐个查询
        Map<Long, FileAsset> filesById = fileAssetRepository.findAllById(fileIds)
                .stream()
                .collect(Collectors.toMap(FileAsset::getId, Function.identity()));

        // 按附件关系顺序组装附件响应
        List<FileAssetResponse> attachments = links.stream()
                .map(link -> filesById.get(link.getFileId()))
                .filter(Objects::nonNull)
                .map(FileAssetResponse::from)
                .toList();

        // 组装订单详情并回写缓存
        OrderDetailResponse detail = OrderDetailResponse.from(
                order,
                shippingMethodCatalog.labelOf(order.getShippingMethod()),
                attachments
        );
        cacheService.put(detail);
        return detail;
    }

    private static void assertOwner(Long createBy, Long currentUserId) {
        if (currentUserId == null || !Objects.equals(createBy, currentUserId)) {
            throw new AccessDeniedException("无权查看该订单");
        }
    }
}
