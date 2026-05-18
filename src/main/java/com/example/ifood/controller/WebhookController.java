package com.example.ifood.controller;

import com.example.ifood.dto.WebhookPayloadDto;
import com.example.ifood.service.PedidoService;
import com.example.ifood.service.PedidoService.ResultadoEvento;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/webhook")
@RequiredArgsConstructor
public class WebhookController {

	private final PedidoService pedidoService;

	@PostMapping("/ifood")
	public ResponseEntity<Map<String, Object>> receberWebhook(@RequestBody WebhookPayloadDto payload) {
		ResultadoEvento resultado = pedidoService.processarEvento(payload);

		Map<String, Object> body = new LinkedHashMap<>();
		body.put("evento", resultado.evento());
		body.put("message", resultado.message());

		if (!resultado.ignorado()) {
			body.put("pedidoId", resultado.pedidoId());
			body.put("idIfood", resultado.idIfood());
			body.put("status", resultado.status() != null ? resultado.status().name() : null);
			body.put("statusLabel", resultado.status() != null ? resultado.status().getLabel() : null);
			body.put("criado", resultado.criado());
		}

		return ResponseEntity.ok(body);
	}

}
