package co.edu.unbosque.mundial_2026.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import co.edu.unbosque.mundial_2026.entity.Orden;

public interface OrdenRepository extends JpaRepository<Orden, Long> {
    Optional<Orden> findByUsuarioIdAndEstado(Long usuarioId, String estado);
    List<Orden> findByUsuarioIdAndEstadoNot(Long usuarioId, String estado);
}