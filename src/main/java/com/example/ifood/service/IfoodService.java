package com.example.ifood.service;

import com.example.ifood.config.IfoodProperties;
import com.example.ifood.dto.IfoodOrderDto;
import com.example.ifood.model.TipoEntrega;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class IfoodService {

	private final IfoodProperties properties;
	private final IfoodAuthService authService;
	private final IfoodApiClient apiClient;

	public String getAccessToken() {
		return authService.getAccessToken();
	}

	public IfoodOrderDto getOrderDetails(String orderId) {
		if (properties.isMockEnabled()) {
			return mockOrder(orderId);
		}
		return apiClient.getOrderDetails(orderId);
	}

	public void confirmarPedido(String orderId) {
		if (properties.isMockEnabled()) {
			log.info("Simulando POST /orders/{}/confirm", orderId);
			return;
		}
		apiClient.confirmarPedido(orderId);
	}

	public void iniciarPreparo(String orderId) {
		if (properties.isMockEnabled()) {
			log.info("Simulando POST /orders/{}/startPreparation", orderId);
			return;
		}
		apiClient.iniciarPreparo(orderId);
	}

	public void marcarPronto(String orderId) {
		if (properties.isMockEnabled()) {
			log.info("Simulando POST /orders/{}/readyToPickup", orderId);
			return;
		}
		apiClient.marcarPronto(orderId);
	}

	public void despachar(String orderId) {
		if (properties.isMockEnabled()) {
			log.info("Simulando POST /orders/{}/dispatch", orderId);
			return;
		}
		apiClient.despachar(orderId);
	}

	private IfoodOrderDto mockOrder(String orderId) {
		log.info("Mock: detalhes do pedido {}", orderId);
		return IfoodOrderDto.builder()
				.id(orderId)
				.orderType(TipoEntrega.TAKEOUT)
				.customer(IfoodOrderDto.CustomerDto.builder()
						.name("João Silva")
						.phone("11999999999")
						.build())
				.items(List.of(
						IfoodOrderDto.ItemDto.builder()
								.name("X-Burger")
								.quantity(2)
								.observation("Sem cebola")
								.build()
				))
				.total(new BigDecimal("35.90"))
				.build();
	}

}
