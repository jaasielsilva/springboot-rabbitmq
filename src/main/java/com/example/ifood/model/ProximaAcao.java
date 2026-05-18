package com.example.ifood.model;

import lombok.Getter;

@Getter
public enum ProximaAcao {

	CONFIRMAR("Confirmar pedido", "/confirmar"),
	INICIAR_PREPARO("Iniciar preparo", "/iniciar-preparo"),
	MARCAR_PRONTO("Marcar pronto", "/pronto"),
	DESPACHAR("Despachar", "/despachar"),
	CONCLUIR("Concluir", "/concluir"),
	NENHUMA(null, null);

	private final String label;
	private final String endpoint;

	ProximaAcao(String label, String endpoint) {
		this.label = label;
		this.endpoint = endpoint;
	}

	public static ProximaAcao de(StatusPedido status, TipoEntrega tipoEntrega) {
		return switch (status) {
			case AGUARDANDO_CONFIRMACAO -> CONFIRMAR;
			case CONFIRMADO -> INICIAR_PREPARO;
			case EM_PREPARO -> MARCAR_PRONTO;
			case PRONTO -> tipoEntrega == TipoEntrega.DELIVERY ? DESPACHAR : CONCLUIR;
			case DESPACHADO -> CONCLUIR;
			default -> NENHUMA;
		};
	}

}
