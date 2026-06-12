package com.wastecoder.shopflow.order.application.port.in;

import com.wastecoder.shopflow.order.application.viewmodel.PlaceOrderCommand;
import com.wastecoder.shopflow.order.domain.model.Order;

public interface PlaceOrderUseCase {

	Order execute(PlaceOrderCommand command);
}
