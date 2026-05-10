package co.edu.unbosque.mundial_2026.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import co.edu.unbosque.mundial_2026.entity.ItemOrden;

public interface ItemOrdenRepository extends JpaRepository<ItemOrden, Long> {
    List<ItemOrden> findByOrdenId(Long ordenId);
    Optional<ItemOrden> findByOrdenIdAndProductoId(Long ordenId, Long productoId);
}