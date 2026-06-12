package com.wastecoder.shopflow.order.adapter.persistence.database;

import com.wastecoder.shopflow.order.adapter.persistence.entity.OrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface OrderJpaDatabase extends JpaRepository<OrderEntity, UUID> {
}
