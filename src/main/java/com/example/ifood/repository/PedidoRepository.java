package com.example.ifood.repository;

import com.example.ifood.model.Pedido;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {

	Optional<Pedido> findByIdIfood(String idIfood);

}
