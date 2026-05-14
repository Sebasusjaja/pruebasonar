package co.edu.unbosque.mundial_2026.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import co.edu.unbosque.mundial_2026.entity.Pronostico;

@Repository
public interface PronosticoRepository extends JpaRepository<Pronostico, Long> {
    List<Pronostico> findByApuestaId(Long apuestaId);
    List<Pronostico> findByApuestaIdAndUsuarioId(Long apuestaId, Long usuarioId);
    void deleteByApuestaId(Long apuestaId);
}