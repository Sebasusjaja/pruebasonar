package co.edu.unbosque.mundial_2026;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;

import co.edu.unbosque.mundial_2026.exception.*;

class ErrorHandlerExceptionTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleUsuarioNotFound_retorna404() {
        ResponseEntity<?> res = handler.handleUsuarioNotFound(new UsuarioNotFoundException("no existe"));
        assertEquals(HttpStatus.NOT_FOUND, res.getStatusCode());
    }

    @Test
    void handleCorreoEnUso_retorna409() {
        ResponseEntity<?> res = handler.handleCorreoEnUso(new CorreoEnUsoException("en uso"));
        assertEquals(HttpStatus.CONFLICT, res.getStatusCode());
    }

    @Test
    void handleContrasenaIncorrecta_retorna401() {
        ResponseEntity<?> res = handler.handleContrasenaIncorrecta(new ContrasenaIncorrectaException("incorrecta"));
        assertEquals(HttpStatus.UNAUTHORIZED, res.getStatusCode());
    }

    @Test
    void handleRolNotFound_retorna404() {
        ResponseEntity<?> res = handler.handleRolNotFound(new RolNotFoundException("no existe"));
        assertEquals(HttpStatus.NOT_FOUND, res.getStatusCode());
    }

    @Test
    void handleIllegalState_retorna400() {
        ResponseEntity<?> res = handler.handleIllegalState(new IllegalStateException("error"));
        assertEquals(HttpStatus.BAD_REQUEST, res.getStatusCode());
    }

    @Test
    void handleIllegalArgument_retorna400() {
        ResponseEntity<?> res = handler.handleIllegalArgument(new IllegalArgumentException("error"));
        assertEquals(HttpStatus.BAD_REQUEST, res.getStatusCode());
    }

    @Test
    void handlePartidoNotFound_retorna404() {
        ResponseEntity<?> res = handler.handlePartidoNotFound(new PartidoNotFoundException("no existe"));
        assertEquals(HttpStatus.NOT_FOUND, res.getStatusCode());
    }

    @Test
    void handleEntradaNotFound_retorna404() {
        ResponseEntity<?> res = handler.handleEntradaNotFound(new EntradaNotFoundException("no existe"));
        assertEquals(HttpStatus.NOT_FOUND, res.getStatusCode());
    }

    @Test
    void handleCupoNoDisponible_retorna409() {
        ResponseEntity<?> res = handler.handleCupoNoDisponible(new CupoNoDisponibleException("sin cupo"));
        assertEquals(HttpStatus.CONFLICT, res.getStatusCode());
    }

    @Test
    void handleLimiteSuperado_retorna429() {
        ResponseEntity<?> res = handler.handleLimiteSuperado(new LimiteSuperadoException("limite"));
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, res.getStatusCode());
    }

    @Test
    void handleEstadoInvalido_retorna400() {
        ResponseEntity<?> res = handler.handleEstadoInvalido(new EstadoInvalidoException("invalido"));
        assertEquals(HttpStatus.BAD_REQUEST, res.getStatusCode());
    }

    @Test
    void handlePagoStripe_retorna402() {
        ResponseEntity<?> res = handler.handlePagoStripe(new PagoStripeException("error pago"));
        assertEquals(HttpStatus.PAYMENT_REQUIRED, res.getStatusCode());
    }

    @Test
    void handleMetodoPagoNotFound_retorna404() {
        ResponseEntity<?> res = handler.handleMetodoPagoNotFound(new MetodoPagoNotFoundException("no existe"));
        assertEquals(HttpStatus.NOT_FOUND, res.getStatusCode());
    }

    @Test
    void handleProductoNotFound_retorna404() {
        ResponseEntity<?> res = handler.handleProductoNotFound(new ProductoNotFoundException("no existe"));
        assertEquals(HttpStatus.NOT_FOUND, res.getStatusCode());
    }

    @Test
    void handleOrdenNotFound_retorna404() {
        ResponseEntity<?> res = handler.handleOrdenNotFound(new OrdenNotFoundException("no existe"));
        assertEquals(HttpStatus.NOT_FOUND, res.getStatusCode());
    }

    @Test
    void handleStockInsuficiente_retorna409() {
        ResponseEntity<?> res = handler.handleStockInsuficiente(new StockInsuficienteException("sin stock"));
        assertEquals(HttpStatus.CONFLICT, res.getStatusCode());
    }

    @Test
    void handleCategoriaNotFound_retorna404() {
        ResponseEntity<?> res = handler.handleCategoriaNotFound(new CategoriaNotFoundException("no existe"));
        assertEquals(HttpStatus.NOT_FOUND, res.getStatusCode());
    }

    @Test
    void handleCarritoVacio_retorna400() {
        ResponseEntity<?> res = handler.handleCarritoVacio(new CarritoVacioException("vacio"));
        assertEquals(HttpStatus.BAD_REQUEST, res.getStatusCode());
    }

    @Test
    void handleMetodoPagoInvalido_retorna400() {
        ResponseEntity<?> res = handler.handleMetodoPagoInvalido(new MetodoPagoInvalidoException("invalido"));
        assertEquals(HttpStatus.BAD_REQUEST, res.getStatusCode());
    }

    @Test
    void handleItemNotFound_retorna404() {
        ResponseEntity<?> res = handler.handleItemNotFound(new ItemNotFoundException("no existe"));
        assertEquals(HttpStatus.NOT_FOUND, res.getStatusCode());
    }

    @Test
    void handleCategoriaYaExiste_retorna409() {
        ResponseEntity<?> res = handler.handleCategoriaYaExiste(new CategoriaYaExisteException("ya existe"));
        assertEquals(HttpStatus.CONFLICT, res.getStatusCode());
    }
    @Test
void apuestaCerradaException_tienemensaje() {
    ApuestaCerradaException e = new ApuestaCerradaException("cerrada");
    assertEquals("cerrada", e.getMessage());
}

@Test
void apuestaNotFoundException_tienemensaje() {
    ApuestaNotFoundException e = new ApuestaNotFoundException("no existe");
    assertEquals("no existe", e.getMessage());
}

@Test
void codigoInvalidoException_tienemensaje() {
    CodigoInvalidoException e = new CodigoInvalidoException("invalido");
    assertEquals("invalido", e.getMessage());
}

@Test
void participacionNotFoundException_tienemensaje() {
    ParticipacionNotFoundException e = new ParticipacionNotFoundException("no existe");
    assertEquals("no existe", e.getMessage());
}

@Test
void usuarioYaEnApuestaException_tienemensaje() {
    UsuarioYaEnApuestaException e = new UsuarioYaEnApuestaException("ya existe");
    assertEquals("ya existe", e.getMessage());
}

@Test
void pronosticoNotFoundException_tienemensaje() {
    PronosticoNotFoundException e = new PronosticoNotFoundException("no existe");
    assertEquals("no existe", e.getMessage());
}
@Test
void handleApuestaNotFound_retorna404() {
    ResponseEntity<?> res = handler.handleApuestaNotFound(new ApuestaNotFoundException("no existe"));
    assertEquals(HttpStatus.NOT_FOUND, res.getStatusCode());
}

@Test
void handlePronosticoNotFound_retorna404() {
    ResponseEntity<?> res = handler.handlePronosticoNotFound(new PronosticoNotFoundException("no existe"));
    assertEquals(HttpStatus.NOT_FOUND, res.getStatusCode());
}

@Test
void handleCodigoInvalido_retorna400() {
    ResponseEntity<?> res = handler.handleCodigoInvalido(new CodigoInvalidoException("invalido"));
    assertEquals(HttpStatus.BAD_REQUEST, res.getStatusCode());
}

@Test
void handleParticipacionNotFound_retorna404() {
    ResponseEntity<?> res = handler.handleParticipacionNotFound(new ParticipacionNotFoundException("no existe"));
    assertEquals(HttpStatus.NOT_FOUND, res.getStatusCode());
}

@Test
void handleUsuarioYaEnApuesta_retorna409() {
    ResponseEntity<?> res = handler.handleUsuarioYaEnApuesta(new UsuarioYaEnApuestaException("ya existe"));
    assertEquals(HttpStatus.CONFLICT, res.getStatusCode());
}

@Test
void handleApuestaCerrada_retorna400() {
    ResponseEntity<?> res = handler.handleApuestaCerrada(new ApuestaCerradaException("cerrada"));
    assertEquals(HttpStatus.BAD_REQUEST, res.getStatusCode());
}

@Test
void handleValidation_retorna400() {
    org.springframework.validation.BeanPropertyBindingResult bindingResult =
        new org.springframework.validation.BeanPropertyBindingResult(new Object(), "test");
    bindingResult.addError(new org.springframework.validation.FieldError("test", "campo", "requerido"));
    MethodArgumentNotValidException ex = new MethodArgumentNotValidException(null, bindingResult);
    ResponseEntity<?> res = handler.handleValidation(ex);
    assertEquals(HttpStatus.BAD_REQUEST, res.getStatusCode());
}


}