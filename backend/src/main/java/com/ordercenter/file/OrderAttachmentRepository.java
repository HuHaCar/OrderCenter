package com.ordercenter.file;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderAttachmentRepository extends JpaRepository<OrderAttachment, Long> {

    List<OrderAttachment> findByOrderId(Long orderId);
}
