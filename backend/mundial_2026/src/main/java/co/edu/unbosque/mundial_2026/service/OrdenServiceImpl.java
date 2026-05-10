package co.edu.unbosque.mundial_2026.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;

import co.edu.unbosque.mundial_2026.dto.request.AgregarItemDTO;
import co.edu.unbosque.mundial_2026.dto.request.ConfirmarOrdenDTO;
import co.edu.unbosque.mundial_2026.dto.response.ItemOrdenResponseDTO;
import co.edu.unbosque.mundial_2026.dto.response.OrdenResponseDTO;
import co.edu.unbosque.mundial_2026.entity.ItemOrden;
import co.edu.unbosque.mundial_2026.entity.MetodoPago;
import co.edu.unbosque.mundial_2026.entity.Orden;
import co.edu.unbosque.mundial_2026.entity.Producto;
import co.edu.unbosque.mundial_2026.entity.Usuario;
import co.edu.unbosque.mundial_2026.exception.CarritoVacioException;
import co.edu.unbosque.mundial_2026.exception.ItemNotFoundException;
import co.edu.unbosque.mundial_2026.exception.MetodoPagoInvalidoException;
import co.edu.unbosque.mundial_2026.exception.OrdenNotFoundException;
import co.edu.unbosque.mundial_2026.exception.PagoStripeException;
import co.edu.unbosque.mundial_2026.exception.ProductoNotFoundException;
import co.edu.unbosque.mundial_2026.exception.StockInsuficienteException;
import co.edu.unbosque.mundial_2026.repository.ItemOrdenRepository;
import co.edu.unbosque.mundial_2026.repository.OrdenRepository;

@Service
public class OrdenServiceImpl implements OrdenService {

    private final OrdenRepository ordenRepository;
    private final ItemOrdenRepository itemOrdenRepository;
    private final UsuarioService usuarioService;
    private final ProductoService productoService;
    private final MetodoPagoService metodoPagoService;
    private final EventoAuditoriaService auditoriaService;

    private static final String ESTADO_PENDIENTE = "PENDIENTE";
    private static final String ESTADO_PAGADA = "PAGADA";
    private static final String ESTADO_CANCELADA = "CANCELADA";
    private static final String TIPO_ORDEN = "Orden";
    private static final String PREFIJO_ORDEN = "ORDEN-";
    private static final String CARRITO_NO_ACTIVO = "No tienes un carrito activo";
    private static final String PREFIJO_USUARIO = "Usuario ";

    public OrdenServiceImpl(OrdenRepository ordenRepository,
            ItemOrdenRepository itemOrdenRepository,
            UsuarioService usuarioService,
            ProductoService productoService,
            MetodoPagoService metodoPagoService,
            EventoAuditoriaService auditoriaService,
            @Value("${stripe.api.key}") String stripeApiKey) {
        this.ordenRepository = ordenRepository;
        this.itemOrdenRepository = itemOrdenRepository;
        this.usuarioService = usuarioService;
        this.productoService = productoService;
        this.metodoPagoService = metodoPagoService;
        this.auditoriaService = auditoriaService;
        Stripe.apiKey = stripeApiKey;
    }

    @Override
    public OrdenResponseDTO agregarItem(String correo, AgregarItemDTO dto) {
        Usuario usuario = usuarioService.obtenerEntidadPorCorreo(correo);
        Producto producto = productoService.obtenerEntidadPorId(dto.getProductoId());
        validarProductoDisponible(producto, dto.getCantidad());
        Orden ordenActual = obtenerOCrearOrden(usuario);
        actualizarItemEnOrden(ordenActual, producto, dto.getCantidad());
        double total = calcularTotal(ordenActual.getId());
        ordenActual.setTotal(total);
        ordenRepository.save(ordenActual);
        auditoriaService.registrar(
                "ITEM_AGREGADO_CARRITO",
                PREFIJO_USUARIO + usuario.getId() + " agregó " + dto.getCantidad() + " x " + producto.getNombre(),
                usuario.getId(),
                PREFIJO_ORDEN + ordenActual.getId(),
                TIPO_ORDEN);
        return toOrdenDTO(ordenActual, itemOrdenRepository.findByOrdenId(ordenActual.getId()));
    }

    @Override
    public OrdenResponseDTO obtenerCarrito(String correo) {
        Usuario usuario = usuarioService.obtenerEntidadPorCorreo(correo);
        Orden orden = ordenRepository.findByUsuarioIdAndEstado(usuario.getId(), ESTADO_PENDIENTE)
                .orElseThrow(() -> new OrdenNotFoundException(CARRITO_NO_ACTIVO));
        return toOrdenDTO(orden, itemOrdenRepository.findByOrdenId(orden.getId()));
    }

    @Override
    public OrdenResponseDTO eliminarItem(String correo, Long itemId) {
        Usuario usuario = usuarioService.obtenerEntidadPorCorreo(correo);
        Orden ordenActual = ordenRepository.findByUsuarioIdAndEstado(usuario.getId(), ESTADO_PENDIENTE)
                .orElseThrow(() -> new OrdenNotFoundException("Usuario no tiene orden activa"));
        ItemOrden itemAEliminar = itemOrdenRepository.findById(itemId)
                .orElseThrow(() -> new ItemNotFoundException("No existe ese item"));
        itemOrdenRepository.delete(itemAEliminar);
        ordenActual.setTotal(ordenActual.getTotal() - (itemAEliminar.getCantidad() * itemAEliminar.getPrecioUnitario()));
        List<ItemOrden> itemsRestantes = itemOrdenRepository.findByOrdenId(ordenActual.getId());
        if (itemsRestantes.isEmpty()) {
            ordenRepository.delete(ordenActual);
            return toOrdenDTO(ordenActual, itemsRestantes);
        }
        ordenRepository.save(ordenActual);
        return toOrdenDTO(ordenActual, itemsRestantes);
    }

    @Override
    public OrdenResponseDTO confirmarOrden(String correo, ConfirmarOrdenDTO dto) {
        Usuario usuario = usuarioService.obtenerEntidadPorCorreo(correo);
        Long usuarioId = usuario.getId();
        Orden ordenAPagar = ordenRepository.findByUsuarioIdAndEstado(usuarioId, ESTADO_PENDIENTE)
                .orElseThrow(() -> new OrdenNotFoundException(CARRITO_NO_ACTIVO));
        List<ItemOrden> items = itemOrdenRepository.findByOrdenId(ordenAPagar.getId());
        validarCarritoNoVacio(items);
        MetodoPago metodoPago = validarMetodoPago(dto.getMetodoPagoId(), usuarioId);
        validarStockItems(items);
        procesarPagoStripe(ordenAPagar, metodoPago, items, usuarioId);
        return toOrdenDTO(ordenAPagar, items);
    }

    @Override
    public List<OrdenResponseDTO> historial(String correo) {
        Usuario usuario = usuarioService.obtenerEntidadPorCorreo(correo);
        List<Orden> ordenes = ordenRepository.findByUsuarioIdAndEstadoNot(usuario.getId(), ESTADO_PENDIENTE);
        List<OrdenResponseDTO> responseDTOs = new ArrayList<>();
        for (int i = 0; i < ordenes.size(); i++) {
            Orden orden = ordenes.get(i);
            responseDTOs.add(toOrdenDTO(orden, itemOrdenRepository.findByOrdenId(orden.getId())));
        }
        return responseDTOs;
    }

    @Override
    public OrdenResponseDTO cancelarOrden(String correo) {
        Usuario usuario = usuarioService.obtenerEntidadPorCorreo(correo);
        Orden orden = ordenRepository.findByUsuarioIdAndEstado(usuario.getId(), ESTADO_PENDIENTE)
                .orElseThrow(() -> new OrdenNotFoundException(CARRITO_NO_ACTIVO));
        List<ItemOrden> items = itemOrdenRepository.findByOrdenId(orden.getId());
        orden.setEstado(ESTADO_CANCELADA);
        ordenRepository.save(orden);
        auditoriaService.registrar(
                "ORDEN_CANCELADA",
                PREFIJO_USUARIO + usuario.getId() + " canceló la orden " + orden.getId(),
                usuario.getId(),
                PREFIJO_ORDEN + orden.getId(),
                TIPO_ORDEN);
        return toOrdenDTO(orden, items);
    }



    private void validarProductoDisponible(Producto producto, int cantidad) {
        if (producto.getStock() < cantidad) {
            throw new StockInsuficienteException("Error no hay stock");
        }
        if (Boolean.FALSE.equals(producto.getActivo())) {
            throw new ProductoNotFoundException("Este producto no está disponible");
        }
    }

    private void validarCarritoNoVacio(List<ItemOrden> items) {
        if (items.isEmpty()) {
            throw new CarritoVacioException("El carrito está vacío");
        }
    }

    private MetodoPago validarMetodoPago(Long metodoPagoId, Long usuarioId) {
        MetodoPago metodoPago = metodoPagoService.obtenerEntidadPorId(metodoPagoId);
        if (!metodoPago.getUsuario().getId().equals(usuarioId)) {
            throw new MetodoPagoInvalidoException("El método de pago no pertenece al usuario");
        }
        return metodoPago;
    }

    private void validarStockItems(List<ItemOrden> items) {
        for (int i = 0; i < items.size(); i++) {
            ItemOrden item = items.get(i);
            if (item.getProducto().getStock() < item.getCantidad()) {
                throw new StockInsuficienteException("Stock insuficiente para " + item.getProducto().getNombre());
            }
        }
    }

    

    private Orden obtenerOCrearOrden(Usuario usuario) {
        Optional<Orden> orden = ordenRepository.findByUsuarioIdAndEstado(usuario.getId(), ESTADO_PENDIENTE);
        if (orden.isPresent()) {
            return orden.get();
        }
        Orden ordenNueva = new Orden();
        ordenNueva.setUsuario(usuario);
        ordenNueva.setEstado(ESTADO_PENDIENTE);
        ordenNueva.setFechaCreacion(LocalDateTime.now());
        ordenNueva.setTotal(0.0);
        return ordenRepository.save(ordenNueva);
    }

    private void actualizarItemEnOrden(Orden orden, Producto producto, int cantidad) {
        Optional<ItemOrden> item = itemOrdenRepository.findByOrdenIdAndProductoId(orden.getId(), producto.getId());
        ItemOrden itemOrdenActual;
        if (item.isPresent()) {
            itemOrdenActual = item.get();
            itemOrdenActual.setCantidad(itemOrdenActual.getCantidad() + cantidad);
            itemOrdenActual.setPrecioUnitario(itemOrdenActual.getPrecioUnitario());
        } else {
            itemOrdenActual = new ItemOrden();
            itemOrdenActual.setOrden(orden);
            itemOrdenActual.setProducto(producto);
            itemOrdenActual.setCantidad(cantidad);
            itemOrdenActual.setPrecioUnitario(producto.getPrecio());
        }
        itemOrdenRepository.save(itemOrdenActual);
    }

    private double calcularTotal(Long ordenId) {
        List<ItemOrden> items = itemOrdenRepository.findByOrdenId(ordenId);
        double total = 0.0;
        for (int i = 0; i < items.size(); i++) {
            ItemOrden item = items.get(i);
            total += item.getCantidad() * item.getPrecioUnitario();
        }
        return total;
    }

    private void procesarPagoStripe(Orden orden, MetodoPago metodoPago, List<ItemOrden> items, Long usuarioId) {
        try {
            long totalCentavos = (long) (orden.getTotal() * 100);
            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                    .setAmount(totalCentavos)
                    .setCurrency("usd")
                    .setPaymentMethod(metodoPago.getDetails())
                    .setConfirm(true)
                    .setAutomaticPaymentMethods(
                            PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                                    .setEnabled(true)
                                    .setAllowRedirects(
                                            PaymentIntentCreateParams.AutomaticPaymentMethods.AllowRedirects.NEVER)
                                    .build())
                    .build();
            PaymentIntent paymentIntent = PaymentIntent.create(params);
            orden.setEstado(ESTADO_PAGADA);
            orden.setFechaPago(LocalDateTime.now());
            orden.setPaymentRef(paymentIntent.getId());
            orden.setMetodoPago(metodoPago);
            actualizarStockProductos(items);
            ordenRepository.save(orden);
            auditoriaService.registrar(
                    "ORDEN_PAGADA",
                    PREFIJO_USUARIO + usuarioId + " pagó la orden " + orden.getId() + " por $" + orden.getTotal(),
                    usuarioId,
                    PREFIJO_ORDEN + orden.getId(),
                    TIPO_ORDEN);
        } catch (StripeException e) {
            throw new PagoStripeException("Error al procesar el pago: " + e.getMessage());
        }
    }

    private void actualizarStockProductos(List<ItemOrden> items) {
        for (int i = 0; i < items.size(); i++) {
            ItemOrden item = items.get(i);
            Producto producto = item.getProducto();
            producto.setStock(producto.getStock() - item.getCantidad());
            productoService.actualizarStock(item.getProducto().getId(), item.getCantidad());
        }
    }


    private ItemOrdenResponseDTO toItemDTO(ItemOrden item) {
        ItemOrdenResponseDTO response = new ItemOrdenResponseDTO();
        response.setId(item.getId());
        response.setProductoId(item.getProducto().getId());
        response.setProductoNombre(item.getProducto().getNombre());
        response.setProductoImagenUrl(item.getProducto().getImagenUrl());
        response.setCantidad(item.getCantidad());
        response.setPrecioUnitario(item.getPrecioUnitario());
        response.setSubtotal(item.getCantidad() * item.getPrecioUnitario());
        return response;
    }

    private OrdenResponseDTO toOrdenDTO(Orden orden, List<ItemOrden> items) {
        OrdenResponseDTO response = new OrdenResponseDTO();
        response.setId(orden.getId());
        response.setEstado(orden.getEstado());
        response.setTotal(orden.getTotal());
        response.setFechaCreacion(orden.getFechaCreacion());
        response.setFechaPago(orden.getFechaPago());
        response.setPaymentRef(orden.getPaymentRef());
        if (orden.getMetodoPago() != null) {
            response.setMetodoPagoLabel(orden.getMetodoPago().getLabel());
        }
        List<ItemOrdenResponseDTO> itemDTOs = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            itemDTOs.add(toItemDTO(items.get(i)));
        }
        response.setItems(itemDTOs);
        return response;
    }
}