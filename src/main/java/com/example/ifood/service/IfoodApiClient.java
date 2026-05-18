package com.example.ifood.service;

import com.example.ifood.config.IfoodProperties;
import com.example.ifood.dto.IfoodEventDto;
import com.example.ifood.dto.IfoodOrderDto;
import com.example.ifood.model.TipoEntrega;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.function.Supplier;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class IfoodApiClient {

	private final IfoodProperties properties;
	private final IfoodAuthService authService;
	private final RestClient restClient = RestClient.create();

	public List<IfoodEventDto> pollEvents() {
		return withTokenRetry(() -> {
			String token = authService.getAccessToken();
			String uri = properties.eventsBaseUrl() + "/events:polling?categories=FOOD";

			var request = restClient.get()
					.uri(uri)
					.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);

			if (properties.getMerchantId() != null && !properties.getMerchantId().isBlank()) {
				request = request.header("x-polling-merchants", properties.getMerchantId());
			}

			List<IfoodEventDto> eventos = request.retrieve().body(new ParameterizedTypeReference<>() {});

			if (eventos == null || eventos.isEmpty()) {
				return List.of();
			}

			return eventos.stream()
					.sorted(Comparator.comparing(IfoodEventDto::getCreatedAt,
							Comparator.nullsLast(Comparator.naturalOrder())))
					.toList();
		});
	}

	public void acknowledgeEvents(List<String> eventIds) {
		if (eventIds == null || eventIds.isEmpty()) {
			return;
		}

		withTokenRetry(() -> {
			String token = authService.getAccessToken();
			List<Map<String, String>> body = eventIds.stream()
					.map(id -> Map.of("id", id))
					.toList();

			restClient.post()
					.uri(properties.eventsBaseUrl() + "/events/acknowledgment")
					.header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
					.contentType(MediaType.APPLICATION_JSON)
					.body(body)
					.retrieve()
					.toBodilessEntity();

			log.debug("ACK enviado para {} evento(s)", eventIds.size());
			return null;
		});
	}

	public IfoodOrderDto getOrderDetails(String orderId) {
		return withTokenRetry(() -> {
			String token = authService.getAccessToken();
			JsonNode root = restClient.get()
					.uri(properties.orderBaseUrl() + "/orders/{orderId}", orderId)
					.header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
					.retrieve()
					.body(JsonNode.class);
			return mapOrder(root, orderId);
		});
	}

	public void confirmarPedido(String orderId) {
		postOrderAction(orderId, "/confirm");
	}

	public void iniciarPreparo(String orderId) {
		postOrderAction(orderId, "/startPreparation");
	}

	public void marcarPronto(String orderId) {
		postOrderAction(orderId, "/readyToPickup");
	}

	public void despachar(String orderId) {
		postOrderAction(orderId, "/dispatch");
	}

	private void postOrderAction(String orderId, String action) {
		withTokenRetry(() -> {
			String token = authService.getAccessToken();
			restClient.post()
					.uri(properties.orderBaseUrl() + "/orders/{orderId}" + action, orderId)
					.header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
					.contentType(MediaType.APPLICATION_JSON)
					.body("{}")
					.retrieve()
					.toBodilessEntity();
			log.info("iFood POST /orders/{}{} OK", orderId, action);
			return null;
		});
	}

	private <T> T withTokenRetry(Supplier<T> call) {
		try {
			return call.get();
		} catch (HttpClientErrorException.Unauthorized e) {
			log.warn("401 na API iFood — renovando token e tentando novamente...");
			authService.invalidateCache();
			authService.refreshAccessToken();
			return call.get();
		}
	}

	private IfoodOrderDto mapOrder(JsonNode root, String orderId) {
		if (root == null) {
			throw new IllegalStateException("Pedido não encontrado na API iFood: " + orderId);
		}

		JsonNode customer = root.path("customer");
		if (customer.isMissingNode()) {
			customer = root.path("delivery").path("deliveryAddress");
		}

		String nome = text(customer, "name");
		if (nome.isBlank()) {
			nome = "Cliente iFood";
		}

		String telefone = text(customer, "phone");
		if (telefone.isBlank()) {
			telefone = text(customer, "phoneNumber");
		}

		TipoEntrega tipo = TipoEntrega.TAKEOUT;
		String orderType = text(root, "orderType");
		if (orderType.isBlank()) {
			orderType = text(root.path("delivery"), "mode");
		}
		if (orderType.toUpperCase().contains("DELIVERY")) {
			tipo = TipoEntrega.DELIVERY;
		}

		BigDecimal total = extractTotal(root);
		List<IfoodOrderDto.ItemDto> itens = extractItems(root);

		return IfoodOrderDto.builder()
				.id(orderId)
				.orderType(tipo)
				.customer(IfoodOrderDto.CustomerDto.builder()
						.name(nome)
						.phone(telefone)
						.build())
				.items(itens)
				.total(total)
				.build();
	}

	private BigDecimal extractTotal(JsonNode root) {
		JsonNode total = root.path("total");
		if (!total.isMissingNode()) {
			if (total.path("orderAmount").isNumber()) {
				return total.path("orderAmount").decimalValue();
			}
			if (total.isNumber()) {
				return total.decimalValue();
			}
		}
		if (root.path("payments").path("total").isNumber()) {
			return root.path("payments").path("total").decimalValue();
		}
		return BigDecimal.ZERO;
	}

	private List<IfoodOrderDto.ItemDto> extractItems(JsonNode root) {
		List<IfoodOrderDto.ItemDto> itens = new ArrayList<>();
		JsonNode items = root.path("items");
		if (!items.isArray()) {
			return itens;
		}
		items.forEach(item -> {
			String name = text(item, "name");
			if (name.isBlank()) {
				name = text(item.path("product"), "name");
			}
			int qty = item.path("quantity").asInt(1);
			String obs = text(item, "observations");
			if (obs.isBlank()) {
				obs = text(item, "observation");
			}
			itens.add(IfoodOrderDto.ItemDto.builder()
					.name(name.isBlank() ? "Item" : name)
					.quantity(Math.max(1, qty))
					.observation(obs.isBlank() ? null : obs)
					.build());
		});
		return itens;
	}

	private String text(JsonNode node, String field) {
		if (node == null || node.isMissingNode()) {
			return "";
		}
		JsonNode value = node.path(field);
		return value.isMissingNode() || value.isNull() ? "" : value.asText("");
	}

}
