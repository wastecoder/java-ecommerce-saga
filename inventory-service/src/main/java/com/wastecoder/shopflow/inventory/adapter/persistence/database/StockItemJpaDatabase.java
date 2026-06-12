package com.wastecoder.shopflow.inventory.adapter.persistence.database;

import com.wastecoder.shopflow.inventory.adapter.persistence.entity.StockItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface StockItemJpaDatabase extends JpaRepository<StockItemEntity, UUID> {
}
