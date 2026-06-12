package com.wastecoder.shopflow.order.application.port.in;

import com.wastecoder.shopflow.order.domain.model.Order;

import java.util.UUID;

public interface GetOrderUseCase {

	Order execute(UUID orderId);
}
