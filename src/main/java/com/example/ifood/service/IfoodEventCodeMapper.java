package com.example.ifood.service;

/**
 * Códigos curtos retornados no polling (ex.: PLC, CFM) → nomes usados no sistema.
 */
public final class IfoodEventCodeMapper {

	private IfoodEventCodeMapper() {
	}

	public static String mapShortCode(String code) {
		if (code == null || code.isBlank()) {
			return null;
		}
		return switch (code.trim().toUpperCase()) {
			case "PLC" -> "PLACED";
			case "CFM" -> "CONFIRMED";
			case "CAN" -> "CANCELLED";
			case "CON" -> "CONCLUDED";
			case "SPS" -> "SEPARATION_STARTED";
			case "SPE" -> "SEPARATION_ENDED";
			case "RTP" -> "READY_TO_PICKUP";
			case "DSP" -> "DISPATCHED";
			default -> code.trim().toUpperCase();
		};
	}

}
