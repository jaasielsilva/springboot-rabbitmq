package com.example.ifood.model;

import lombok.Getter;

import java.util.Arrays;
import java.util.Optional;

@Getter
public enum StatusPedido {

	AGUARDANDO_CONFIRMACAO(1, "Novo pedido", "Aguardando confirmação da loja (até 8 min no iFood)"),
	CONFIRMADO(2, "Confirmado", "Loja aceitou — enviar para cozinha"),
	EM_PREPARO(3, "Em preparo", "Cozinha preparando"),
	PRONTO(4, "Pronto", "Aguardando retirada ou entrega"),
	DESPACHADO(5, "Despachado", "Saiu para entrega"),
	CONCLUIDO(6, "Concluído", "Pedido finalizado"),
	CANCELADO(99, "Cancelado", "Pedido cancelado");

	private final int ordem;
	private final String label;
	private final String descricao;

	StatusPedido(int ordem, String label, String descricao) {
		this.ordem = ordem;
		this.label = label;
		this.descricao = descricao;
	}

	public boolean isTerminal() {
		return this == CONCLUIDO || this == CANCELADO;
	}

	public boolean podeAvancarPara(StatusPedido novo) {
		if (this == novo) {
			return false;
		}
		if (isTerminal()) {
			return false;
		}
		if (novo == CANCELADO) {
			return true;
		}
		return novo.ordem > this.ordem;
	}

	public static Optional<StatusPedido> fromLegado(String valor) {
		if (valor == null) {
			return Optional.empty();
		}
		return switch (valor.toUpperCase()) {
			case "PLACED" -> Optional.of(AGUARDANDO_CONFIRMACAO);
			case "CONFIRMED" -> Optional.of(CONFIRMADO);
			case "PREPARING", "SEPARATION_STARTED", "PREPARATION_STARTED" -> Optional.of(EM_PREPARO);
			case "READY_TO_PICKUP", "SEPARATION_ENDED" -> Optional.of(PRONTO);
			case "DISPATCHED" -> Optional.of(DESPACHADO);
			case "CONCLUDED" -> Optional.of(CONCLUIDO);
			case "CANCELLED", "CANCELED" -> Optional.of(CANCELADO);
			default -> Arrays.stream(values())
					.filter(s -> s.name().equalsIgnoreCase(valor))
					.findFirst();
		};
	}

}
