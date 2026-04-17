package com.example.demo.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.producer.ChamadoProducer;
import com.example.demo.service.ProcessamentoService;

@RestController
public class TesteController {

    private final ChamadoProducer chamadoProducer;

    private final ProcessamentoService processamentoService;

    public TesteController(ChamadoProducer chamadoProducer, ProcessamentoService processamentoService) {
        this.chamadoProducer = chamadoProducer;
        this.processamentoService = processamentoService;
    }

    @GetMapping("/enviar")
    public String enviarMensagem() {
        chamadoProducer.enviar("Novo chamado criado com sucesso!");
        return "Mensagem enviada para a fila.";
    }

    @GetMapping("/lote")
    public String enviarLote() {

        int quantidade = 10;
        processamentoService.iniciar(quantidade);

        for (int i = 1; i <= quantidade; i++) {
            chamadoProducer.enviar("Chamado #" + i);
        }

        return "Mensagens enviadas!";
    }

    @GetMapping("/status")
    public Map<String, Integer> status() {

        Map<String, Integer> retorno = new HashMap<>();
        retorno.put("total", processamentoService.getTotal());
        retorno.put("processados", processamentoService.getProcessados());

        return retorno;
    }
}
