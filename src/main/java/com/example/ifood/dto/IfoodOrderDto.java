package com.example.ifood.dto;

import com.example.ifood.model.TipoEntrega;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IfoodOrderDto {

	private String id;
	private TipoEntrega orderType;
	private CustomerDto customer;
	private List<ItemDto> items;
	private BigDecimal total;

	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	@Builder
	public static class CustomerDto {
		private String name;
		private String phone;
	}

	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	@Builder
	public static class ItemDto {
		private String name;
		private Integer quantity;
		private String observation;
	}

}
