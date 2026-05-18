package com.example.ifood.dto;

import com.example.ifood.service.IfoodEventCodeMapper;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class IfoodEventDto {

	private String id;
	private String code;
	private String fullCode;
	private String orderId;
	private String merchantId;
	private String createdAt;

	public String resolveCodigoEvento() {
		if (fullCode != null && !fullCode.isBlank()) {
			return fullCode.trim();
		}
		return IfoodEventCodeMapper.mapShortCode(code);
	}

}
