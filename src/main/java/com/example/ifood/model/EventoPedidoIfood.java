package com.example.ifood.model;

import lombok.Getter;

import java.util.Arrays;
import java.util.Optional;

/**
 * Eventos principais do ciclo de vida — referência iFood Developer (módulo Order/Events).
 */
@Getter
public enum EventoPedidoIfood {

	CONFIRMED(StatusPedido.AGUARDANDO_CONFIRMACAO, true),
	PLACED(StatusPedido.AGUARDANDO_CONFIRMACAO, true),
	SEPARATION_STARTED(StatusPedido.EM_PREPARO, false),
	PREPARATION_STARTED(StatusPedido.EM_PREPARO, false),
	SEPARATION_ENDED(StatusPedido.PRONTO, false),
	READY_TO_PICKUP(StatusPedido.PRONTO, false),
	DISPATCHED(StatusPedido.DESPACHADO, false),
	CONCLUDED(StatusPedido.CONCLUIDO, false),
	CANCELLED(StatusPedido.CANCELADO, false);

	private final StatusPedido statusResultante;
	private final boolean criarPedidoSeNaoExistir;

	EventoPedidoIfood(StatusPedido statusResultante, boolean criarPedidoSeNaoExistir) {
		this.statusResultante = statusResultante;
		this.criarPedidoSeNaoExistir = criarPedidoSeNaoExistir;
	}

	public static Optional<EventoPedidoIfood> fromCodigo(String codigo) {
		if (codigo == null || codigo.isBlank()) {
			return Optional.empty();
		}
		String normalizado = codigo.trim().toUpperCase();
		return Arrays.stream(values())
				.filter(e -> e.name().equals(normalizado))
				.findFirst();
	}

}
