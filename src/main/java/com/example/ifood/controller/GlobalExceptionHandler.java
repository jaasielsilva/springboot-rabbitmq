package com.example.ifood.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.HttpStatusCodeException;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<Map<String, String>> badRequest(IllegalArgumentException ex) {
		return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
	}

	@ExceptionHandler(IllegalStateException.class)
	public ResponseEntity<Map<String, String>> conflict(IllegalStateException ex) {
		return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", ex.getMessage()));
	}

	@ExceptionHandler(HttpStatusCodeException.class)
	public ResponseEntity<Map<String, String>> ifoodApiError(HttpStatusCodeException ex) {
		int code = ex.getStatusCode().value();
		String message = code == 401
				? "Token iFood inválido (401). Pare o Spring, corrija IFOOD_CLIENT_SECRET e IFOOD_REFRESH_TOKEN e suba de novo."
				: "Erro na API iFood (" + code + ")";
		return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of("error", message));
	}

}
