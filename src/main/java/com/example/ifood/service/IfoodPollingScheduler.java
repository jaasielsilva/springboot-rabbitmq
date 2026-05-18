package com.example.ifood.service;

import com.example.ifood.config.IfoodProperties;
import com.example.ifood.dto.IfoodEventDto;
import com.example.ifood.dto.WebhookPayloadDto;
import com.example.ifood.model.EventoIfoodRegistro;
import com.example.ifood.repository.EventoIfoodRegistroRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
@ConditionalOnProperty(name = "ifood.polling-enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class IfoodPollingScheduler {

	private final IfoodProperties properties;
	private final IfoodApiClient apiClient;
	private final PedidoService pedidoService;
	private final EventoIfoodRegistroRepository eventoRepository;

	@Scheduled(fixedDelayString = "${ifood.polling-interval-ms:30000}")
	@Transactional
	public void poll() {
		if (properties.isMockEnabled()) {
			return;
		}

		try {
			List<IfoodEventDto> eventos = apiClient.pollEvents();
			if (eventos.isEmpty()) {
				return;
			}

			log.info("Polling iFood: {} evento(s) recebido(s)", eventos.size());
			List<String> idsParaAck = new ArrayList<>();

			for (IfoodEventDto evento : eventos) {
				idsParaAck.add(evento.getId());

				if (eventoRepository.existsById(evento.getId())) {
					continue;
				}

				String codigo = evento.resolveCodigoEvento();
				WebhookPayloadDto payload = WebhookPayloadDto.builder()
						.id(evento.getId())
						.code(codigo)
						.fullCode(evento.getFullCode())
						.orderId(evento.getOrderId())
						.createdAt(evento.getCreatedAt())
						.build();

				pedidoService.processarEvento(payload);

				eventoRepository.save(EventoIfoodRegistro.builder()
						.eventId(evento.getId())
						.orderId(evento.getOrderId())
						.codigoEvento(codigo)
						.processadoEm(LocalDateTime.now())
						.build());
			}

			apiClient.acknowledgeEvents(idsParaAck);

		} catch (Exception e) {
			log.error("Erro no polling iFood: {}", e.getMessage());
		}
	}

}
