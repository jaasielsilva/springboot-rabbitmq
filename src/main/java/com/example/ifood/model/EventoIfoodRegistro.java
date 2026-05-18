package com.example.ifood.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "eventos_ifood")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventoIfoodRegistro {

	@Id
	private String eventId;

	@Column(nullable = false)
	private String orderId;

	private String codigoEvento;

	@Column(nullable = false)
	private LocalDateTime processadoEm;

}
