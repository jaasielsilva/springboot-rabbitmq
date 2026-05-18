package com.example.ifood.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Aceita formato simplificado (Postman) ou estrutura real do iFood.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class WebhookPayloadDto {

	private String id;
	private String code;
	private String fullCode;
	private String orderId;
	private String createdAt;

	/** Formato legado de testes: { "orderId", "status" } */
	private String status;

	public String resolveEventCode() {
		if (code != null && !code.isBlank()) {
			return code.trim();
		}
		if (status != null && !status.isBlank()) {
			return status.trim();
		}
		return null;
	}

	public String resolveOrderId() {
		return orderId;
	}

}
