package com.example.ifood.dto;

import com.example.ifood.model.ProximaAcao;
import com.example.ifood.model.StatusPedido;
import com.example.ifood.model.TipoEntrega;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PedidoResponseDto {

	private Long id;
	private String idIfood;
	private String clienteNome;
	private String telefone;
	private StatusPedido status;
	private String statusLabel;
	private String statusDescricao;
	private TipoEntrega tipoEntrega;
	private String ultimoEventoIfood;
	private BigDecimal valorTotal;
	private LocalDateTime criadoEm;
	private LocalDateTime atualizadoEm;
	private boolean finalizado;
	private ProximaAcao proximaAcao;
	private String proximaAcaoLabel;
	private String proximaAcaoEndpoint;
	private List<ItemResponseDto> itens;

	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	@Builder
	public static class ItemResponseDto {
		private String nomeProduto;
		private Integer quantidade;
		private String observacao;
	}

}
