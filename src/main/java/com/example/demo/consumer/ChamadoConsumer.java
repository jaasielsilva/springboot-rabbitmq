package com.example.demo.consumer;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.example.demo.service.ProcessamentoService;

@Component
public class ChamadoConsumer {

    private final ProcessamentoService processamentoService;

    public ChamadoConsumer(ProcessamentoService processamentoService) {
        this.processamentoService = processamentoService;
    }

    @RabbitListener(queues = "fila.chamados")
    public void receber(String mensagem) throws InterruptedException {
        System.out.println("Recebendo: " + mensagem);

        Thread.sleep(2000);

        processamentoService.incrementar();

        System.out.println("Finalizado: " + mensagem);
    }
}