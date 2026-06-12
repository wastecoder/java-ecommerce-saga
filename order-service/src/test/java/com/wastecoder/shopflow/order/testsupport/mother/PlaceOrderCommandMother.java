package com.wastecoder.shopflow.order.testsupport.mother;

import com.wastecoder.shopflow.order.application.viewmodel.PlaceOrderCommand;

import java.math.BigDecimal;
import java.util.List;

public final class PlaceOrderCommandMother {

	private PlaceOrderCommandMother() {
	}

	/** Single item: quantity 2, unitPrice 10.50 -> totalAmount 21.00. */
	public static PlaceOrderCommand aValidCommand() {
		return new PlaceOrderCommand(
				OrderMother.CUSTOMER_ID,
				List.of(new PlaceOrderCommand.Item(OrderMother.PRODUCT_ID, 2, new BigDecimal("10.50"))));
	}

	/** Two items: 2 * 10.50 + 1 * 4.00 = 25.00. */
	public static PlaceOrderCommand aCommandWithTwoItems() {
		return new PlaceOrderCommand(
				OrderMother.CUSTOMER_ID,
				List.of(
						new PlaceOrderCommand.Item(OrderMother.PRODUCT_ID, 2, new BigDecimal("10.50")),
						new PlaceOrderCommand.Item(OrderMother.SECOND_PRODUCT_ID, 1, new BigDecimal("4.00"))));
	}
}
