package com.wastecoder.shopflow.order.adapter.persistence;

import com.wastecoder.shopflow.order.TestcontainersConfiguration;
import com.wastecoder.shopflow.order.application.port.out.OrderRepository;
import com.wastecoder.shopflow.order.domain.model.Order;
import com.wastecoder.shopflow.order.domain.model.OrderItem;
import com.wastecoder.shopflow.order.domain.model.OrderStatus;
import com.wastecoder.shopflow.order.testsupport.mother.OrderMother;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class OrderRepositoryImplIntegrationTest {

	@Autowired
	private OrderRepository repository;

	@Test
	@DisplayName("Given a PENDING order, when it is saved and read back, then all fields and items round-trip")
	void saveAndFindById_roundTrips() {
		Order order = OrderMother.aPendingOrder();

		repository.save(order);
		Optional<Order> found = repository.findById(OrderMother.ORDER_ID);

		assertThat(found).isPresent();
		Order persisted = found.get();
		assertThat(persisted.id()).isEqualTo(OrderMother.ORDER_ID);
		assertThat(persisted.customerId()).isEqualTo(OrderMother.CUSTOMER_ID);
		assertThat(persisted.status()).isEqualTo(OrderStatus.PENDING);
		assertThat(persisted.totalAmount()).isEqualByComparingTo(order.totalAmount());
		assertThat(persisted.createdAt()).isEqualTo(order.createdAt());
		assertThat(persisted.items()).singleElement().satisfies(item -> {
			OrderItem expected = order.items().get(0);
			assertThat(item.id()).isEqualTo(expected.id());
			assertThat(item.productId()).isEqualTo(expected.productId());
			assertThat(item.quantity()).isEqualTo(expected.quantity());
			assertThat(item.unitPrice()).isEqualByComparingTo(expected.unitPrice());
		});
	}

	@Test
	@DisplayName("Given no order with the given id, when findById is called, then an empty optional is returned")
	void findById_returnsEmptyWhenMissing() {
		Optional<Order> found = repository.findById(OrderMother.SECOND_ITEM_ID);

		assertThat(found).isEmpty();
	}
}
