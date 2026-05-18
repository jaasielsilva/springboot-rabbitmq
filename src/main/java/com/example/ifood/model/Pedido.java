package com.example.ifood.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "pedidos")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Pedido {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true)
	private String idIfood;

	private String clienteNome;

	private String telefone;

	@Convert(converter = PedidoStatusConverter.class)
	@Column(nullable = false)
	private StatusPedido status;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	@Builder.Default
	private TipoEntrega tipoEntrega = TipoEntrega.TAKEOUT;

	private String ultimoEventoIfood;

	@Column(nullable = false)
	private BigDecimal valorTotal;

	@Column(nullable = false)
	private LocalDateTime criadoEm;

	private LocalDateTime atualizadoEm;

	@OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true)
	@Builder.Default
	private List<ItemPedido> itens = new ArrayList<>();

}
