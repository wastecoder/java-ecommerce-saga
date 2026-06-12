package com.wastecoder.shopflow.order.adapter.persistence.mapper;

import com.wastecoder.shopflow.order.adapter.persistence.entity.OrderEntity;
import com.wastecoder.shopflow.order.domain.model.Order;
import com.wastecoder.shopflow.order.domain.model.OrderStatus;
import com.wastecoder.shopflow.order.testsupport.mother.OrderMother;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class OrderEntityMapperTest {

	private final OrderEntityMapper mapper = new OrderEntityMapperImpl();

	@Test
	@DisplayName("Given null inputs, when mapping, then null is returned")
	void nullInputs_returnNull() {
		assertThat(mapper.toEntity(null)).isNull();
		assertThat(mapper.toDomain(null)).isNull();
		assertThat(mapper.toItemEntity(null)).isNull();
		assertThat(mapper.toItemDomain(null)).isNull();
	}

	@Test
	@DisplayName("Given a domain order, when mapped to entity, then each item points back to the parent entity")
	void toEntity_linksItemsBackReference() {
		Order order = OrderMother.aPendingOrder();

		OrderEntity entity = mapper.toEntity(order);

		assertThat(entity.getId()).isEqualTo(order.id());
		assertThat(entity.getStatus()).isEqualTo(OrderStatus.PENDING);
		assertThat(entity.getItems()).singleElement()
				.satisfies(item -> assertThat(item.getOrder()).isSameAs(entity));
	}

	@Test
	@DisplayName("Given an entity, when mapped to domain, then all fields are copied")
	void toDomain_mapsAllFields() {
		OrderEntity entity = mapper.toEntity(OrderMother.aPendingOrder());

		Order domain = mapper.toDomain(entity);

		assertThat(domain.id()).isEqualTo(OrderMother.ORDER_ID);
		assertThat(domain.customerId()).isEqualTo(OrderMother.CUSTOMER_ID);
		assertThat(domain.totalAmount()).isEqualByComparingTo(new BigDecimal("21.00"));
		assertThat(domain.items()).singleElement()
				.satisfies(item -> assertThat(item.productId()).isEqualTo(OrderMother.PRODUCT_ID));
	}

	@Test
	@DisplayName("Given an order without an items collection, when mapping both ways, then null items are tolerated")
	void nullItemsCollection_isTolerated() {
		Order orderWithoutItems = new Order(
				OrderMother.ORDER_ID, OrderMother.CUSTOMER_ID, OrderStatus.PENDING,
				BigDecimal.ZERO, OrderMother.CREATED_AT, null);

		OrderEntity entity = mapper.toEntity(orderWithoutItems);
		assertThat(entity.getItems()).isNull();

		OrderEntity bareEntity = new OrderEntity();
		bareEntity.setId(OrderMother.ORDER_ID);
		bareEntity.setCustomerId(OrderMother.CUSTOMER_ID);
		bareEntity.setStatus(OrderStatus.PENDING);
		bareEntity.setTotalAmount(BigDecimal.ZERO);
		bareEntity.setCreatedAt(OrderMother.CREATED_AT);
		bareEntity.setItems(null);

		Order domain = mapper.toDomain(bareEntity);
		assertThat(domain.items()).isNull();
	}
}
