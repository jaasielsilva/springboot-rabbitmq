package com.example.ifood.service;

import com.example.ifood.dto.IfoodOrderDto;
import com.example.ifood.dto.PedidoResponseDto;
import com.example.ifood.dto.WebhookPayloadDto;
import com.example.ifood.model.*;
import com.example.ifood.repository.PedidoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PedidoService {

	private final IfoodService ifoodService;
	private final PedidoRepository pedidoRepository;

	@Transactional
	public ResultadoEvento processarEvento(WebhookPayloadDto payload) {
		String codigoEvento = payload.resolveEventCode();
		String orderId = payload.resolveOrderId();

		if (codigoEvento == null || orderId == null) {
			throw new IllegalArgumentException("Payload inválido: informe code/status e orderId");
		}

		Optional<EventoPedidoIfood> eventoOpt = EventoPedidoIfood.fromCodigo(codigoEvento);
		if (eventoOpt.isEmpty()) {
			log.info("Evento iFood ignorado (não mapeado): {}", codigoEvento);
			return ResultadoEvento.ignorado(codigoEvento, "Evento não faz parte do fluxo operacional principal");
		}

		EventoPedidoIfood evento = eventoOpt.get();
		Optional<Pedido> existente = pedidoRepository.findByIdIfood(orderId);

		if (existente.isEmpty()) {
			if (!evento.isCriarPedidoSeNaoExistir()) {
				log.warn("Pedido {} não encontrado para evento {}", orderId, codigoEvento);
				return ResultadoEvento.ignorado(codigoEvento, "Pedido não existe no sistema");
			}
			Pedido criado = criarPedidoNovo(orderId, evento.getStatusResultante(), codigoEvento);
			return ResultadoEvento.criado(criado, codigoEvento);
		}

		Pedido pedido = existente.get();
		StatusPedido novoStatus = evento.getStatusResultante();

		if (!pedido.getStatus().podeAvancarPara(novoStatus)) {
			log.info("Transição ignorada: pedido {} {} → {}", orderId, pedido.getStatus(), novoStatus);
			return ResultadoEvento.ignorado(codigoEvento,
					"Pedido já está em " + pedido.getStatus().getLabel());
		}

		aplicarStatus(pedido, novoStatus, codigoEvento);
		Pedido atualizado = pedidoRepository.save(pedido);
		logTransicao(atualizado, "evento iFood: " + codigoEvento);
		return ResultadoEvento.atualizado(atualizado, codigoEvento);
	}

	@Transactional
	public Pedido confirmarPedido(Long id) {
		return executarAcaoLoja(id, StatusPedido.CONFIRMADO, ProximaAcao.CONFIRMAR,
				() -> ifoodService.confirmarPedido(buscar(id).getIdIfood()));
	}

	@Transactional
	public Pedido iniciarPreparo(Long id) {
		return executarAcaoLoja(id, StatusPedido.EM_PREPARO, ProximaAcao.INICIAR_PREPARO,
				() -> ifoodService.iniciarPreparo(buscar(id).getIdIfood()));
	}

	@Transactional
	public Pedido marcarPronto(Long id) {
		return executarAcaoLoja(id, StatusPedido.PRONTO, ProximaAcao.MARCAR_PRONTO,
				() -> ifoodService.marcarPronto(buscar(id).getIdIfood()));
	}

	@Transactional
	public Pedido despachar(Long id) {
		return executarAcaoLoja(id, StatusPedido.DESPACHADO, ProximaAcao.DESPACHAR,
				() -> ifoodService.despachar(buscar(id).getIdIfood()));
	}

	@Transactional
	public Pedido concluir(Long id) {
		return executarAcaoLoja(id, StatusPedido.CONCLUIDO, ProximaAcao.CONCLUIR, () -> {});
	}

	/** Compatibilidade com endpoint antigo */
	@Transactional
	public Pedido aceitarPedido(Long id) {
		return confirmarPedido(id);
	}

	@Transactional(readOnly = true)
	public List<PedidoResponseDto> listarTodos() {
		return pedidoRepository.findAll().stream()
				.sorted((a, b) -> b.getCriadoEm().compareTo(a.getCriadoEm()))
				.map(this::toResponseDto)
				.toList();
	}

	@Transactional(readOnly = true)
	public List<PedidoResponseDto> listarAtivos() {
		return listarTodos().stream()
				.filter(p -> !p.isFinalizado())
				.toList();
	}

	private Pedido executarAcaoLoja(Long id, StatusPedido novoStatus, ProximaAcao acaoEsperada, Runnable ifoodCall) {
		Pedido pedido = buscar(id);
		ProximaAcao proxima = ProximaAcao.de(pedido.getStatus(), pedido.getTipoEntrega());

		if (proxima != acaoEsperada) {
			throw new IllegalStateException(
					"Ação inválida para status atual: " + pedido.getStatus().getLabel());
		}

		ifoodService.getAccessToken();
		ifoodCall.run();

		if (!pedido.getStatus().podeAvancarPara(novoStatus)) {
			throw new IllegalStateException("Não é possível avançar para " + novoStatus.getLabel());
		}

		aplicarStatus(pedido, novoStatus, "ACAO_LOJA:" + acaoEsperada.name());
		Pedido salvo = pedidoRepository.save(pedido);
		logTransicao(salvo, "ação da loja: " + acaoEsperada.getLabel());
		return salvo;
	}

	private Pedido criarPedidoNovo(String orderId, StatusPedido statusInicial, String codigoEvento) {
		ifoodService.getAccessToken();
		IfoodOrderDto detalhes = ifoodService.getOrderDetails(orderId);

		TipoEntrega tipo = detalhes.getOrderType() != null ? detalhes.getOrderType() : TipoEntrega.TAKEOUT;

		var customer = detalhes.getCustomer() != null ? detalhes.getCustomer()
				: IfoodOrderDto.CustomerDto.builder().name("Cliente iFood").phone("").build();

		Pedido pedido = Pedido.builder()
				.idIfood(detalhes.getId())
				.clienteNome(customer.getName())
				.telefone(customer.getPhone())
				.status(statusInicial)
				.tipoEntrega(tipo)
				.ultimoEventoIfood(codigoEvento)
				.valorTotal(detalhes.getTotal())
				.criadoEm(LocalDateTime.now())
				.atualizadoEm(LocalDateTime.now())
				.build();

		detalhes.getItems().forEach(item -> pedido.getItens().add(ItemPedido.builder()
				.pedido(pedido)
				.nomeProduto(item.getName())
				.quantidade(item.getQuantity())
				.observacao(item.getObservation())
				.build()));

		Pedido salvo = pedidoRepository.save(pedido);
		logTransicao(salvo, "novo pedido — evento: " + codigoEvento);
		return salvo;
	}

	private void aplicarStatus(Pedido pedido, StatusPedido novoStatus, String evento) {
		pedido.setStatus(novoStatus);
		pedido.setUltimoEventoIfood(evento);
		pedido.setAtualizadoEm(LocalDateTime.now());
	}

	private Pedido buscar(Long id) {
		return pedidoRepository.findById(id)
				.orElseThrow(() -> new IllegalArgumentException("Pedido não encontrado: " + id));
	}

	private void logTransicao(Pedido pedido, String origem) {
		log.info("=== PEDIDO {} | {} | {} ===", pedido.getIdIfood(), pedido.getStatus().getLabel(), origem);
		log.info("Cliente: {} | {} | R$ {}", pedido.getClienteNome(), pedido.getTelefone(), pedido.getValorTotal());
	}

	private PedidoResponseDto toResponseDto(Pedido pedido) {
		ProximaAcao proxima = ProximaAcao.de(pedido.getStatus(), pedido.getTipoEntrega());

		return PedidoResponseDto.builder()
				.id(pedido.getId())
				.idIfood(pedido.getIdIfood())
				.clienteNome(pedido.getClienteNome())
				.telefone(pedido.getTelefone())
				.status(pedido.getStatus())
				.statusLabel(pedido.getStatus().getLabel())
				.statusDescricao(pedido.getStatus().getDescricao())
				.tipoEntrega(pedido.getTipoEntrega())
				.ultimoEventoIfood(pedido.getUltimoEventoIfood())
				.valorTotal(pedido.getValorTotal())
				.criadoEm(pedido.getCriadoEm())
				.atualizadoEm(pedido.getAtualizadoEm())
				.finalizado(pedido.getStatus().isTerminal())
				.proximaAcao(proxima)
				.proximaAcaoLabel(proxima.getLabel())
				.proximaAcaoEndpoint(proxima.getEndpoint())
				.itens(pedido.getItens().stream()
						.map(i -> PedidoResponseDto.ItemResponseDto.builder()
								.nomeProduto(i.getNomeProduto())
								.quantidade(i.getQuantidade())
								.observacao(i.getObservacao())
								.build())
						.toList())
				.build();
	}

	public record ResultadoEvento(
			String evento,
			String message,
			Long pedidoId,
			String idIfood,
			StatusPedido status,
			boolean criado,
			boolean ignorado
	) {
		static ResultadoEvento criado(Pedido p, String evento) {
			return new ResultadoEvento(evento, "Pedido criado", p.getId(), p.getIdIfood(), p.getStatus(), true, false);
		}

		static ResultadoEvento atualizado(Pedido p, String evento) {
			return new ResultadoEvento(evento, "Status atualizado", p.getId(), p.getIdIfood(), p.getStatus(), false, false);
		}

		static ResultadoEvento ignorado(String evento, String message) {
			return new ResultadoEvento(evento, message, null, null, null, false, true);
		}
	}

}
