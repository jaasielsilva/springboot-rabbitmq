package com.example.demo.producer;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
public class ChamadoProducer {

    private final RabbitTemplate rabbitTemplate;

    public ChamadoProducer(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void enviar(String mensagem) {
        rabbitTemplate.convertAndSend("fila.chamados", mensagem);
    }
}