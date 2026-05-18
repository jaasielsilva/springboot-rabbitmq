package com.example.ifood.controller;

import com.example.ifood.dto.PedidoResponseDto;
import com.example.ifood.model.Pedido;
import com.example.ifood.service.PedidoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class PedidoController {

	private final PedidoService pedidoService;

	@GetMapping("/pedidos")
	public String listarPedidos(Model model) {
		model.addAttribute("pedidos", pedidoService.listarTodos());
		return "pedidos";
	}

	@GetMapping("/api/pedidos")
	@ResponseBody
	public List<PedidoResponseDto> listarPedidosApi(
			@RequestParam(defaultValue = "false") boolean apenasAtivos) {
		return apenasAtivos ? pedidoService.listarAtivos() : pedidoService.listarTodos();
	}

	@PostMapping("/pedido/{id}/confirmar")
	@ResponseBody
	public ResponseEntity<Map<String, Object>> confirmar(@PathVariable Long id) {
		return respostaAcao(pedidoService.confirmarPedido(id));
	}

	@PostMapping("/pedido/{id}/iniciar-preparo")
	@ResponseBody
	public ResponseEntity<Map<String, Object>> iniciarPreparo(@PathVariable Long id) {
		return respostaAcao(pedidoService.iniciarPreparo(id));
	}

	@PostMapping("/pedido/{id}/pronto")
	@ResponseBody
	public ResponseEntity<Map<String, Object>> marcarPronto(@PathVariable Long id) {
		return respostaAcao(pedidoService.marcarPronto(id));
	}

	@PostMapping("/pedido/{id}/despachar")
	@ResponseBody
	public ResponseEntity<Map<String, Object>> despachar(@PathVariable Long id) {
		return respostaAcao(pedidoService.despachar(id));
	}

	@PostMapping("/pedido/{id}/concluir")
	@ResponseBody
	public ResponseEntity<Map<String, Object>> concluir(@PathVariable Long id) {
		return respostaAcao(pedidoService.concluir(id));
	}

	@PostMapping("/pedido/{id}/aceitar")
	@ResponseBody
	public ResponseEntity<Map<String, Object>> aceitar(@PathVariable Long id) {
		return respostaAcao(pedidoService.aceitarPedido(id));
	}

	private ResponseEntity<Map<String, Object>> respostaAcao(Pedido pedido) {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("message", "Ação realizada");
		body.put("pedidoId", pedido.getId());
		body.put("status", pedido.getStatus().name());
		body.put("statusLabel", pedido.getStatus().getLabel());
		return ResponseEntity.ok(body);
	}

}
