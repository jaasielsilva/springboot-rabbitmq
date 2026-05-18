package com.example.ifood.model;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class PedidoStatusConverter implements AttributeConverter<StatusPedido, String> {

	@Override
	public String convertToDatabaseColumn(StatusPedido status) {
		return status == null ? null : status.name();
	}

	@Override
	public StatusPedido convertToEntityAttribute(String dbData) {
		if (dbData == null) {
			return null;
		}
		return StatusPedido.fromLegado(dbData).orElse(StatusPedido.valueOf(dbData));
	}

}
