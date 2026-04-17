package com.example.demo.service;

import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Service;

@Service
public class ProcessamentoService {

    private final AtomicInteger total = new AtomicInteger(0);
    private final AtomicInteger processados = new AtomicInteger(0);

    public void iniciar(int quantidade) {
        total.set(quantidade);
        processados.set(0);
    }

    public void incrementar() {
        processados.incrementAndGet();
    }

    public int getTotal() {
        return total.get();
    }

    public int getProcessados() {
        return processados.get();
    }
}