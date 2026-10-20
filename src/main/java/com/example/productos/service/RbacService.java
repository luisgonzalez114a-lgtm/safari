package com.example.productos.service;

import com.example.productos.model.Modulo;
import com.example.productos.model.Permiso;
import com.example.productos.model.Rol;
import com.example.productos.model.RolModulo;
import com.example.productos.model.Vista;
import com.example.productos.model.VistaPermiso;
import com.example.productos.service.AuditService;
import com.example.productos.repository.ModuloRepository;
import com.example.productos.repository.PermisoRepository;
import com.example.productos.repository.RolModuloRepository;
import com.example.productos.repository.RolRepository;
import com.example.productos.repository.VistaPermisoRepository;
import com.example.productos.repository.VistaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
public class RbacService {
    public static final String CONSULTAR = "CONSULTAR";
    public static final String CREAR = "CREAR";
    public static final String EDITAR = "EDITAR";
    public static final String ELIMINAR = "ELIMINAR";
    private static final List<String> PERMISOS = List.of(CONSULTAR, CREAR, EDITAR, ELIMINAR);
    private static final List<ModuloDef> MODULOS = List.of(
            new ModuloDef("INVENTARIO", "Inventario", "Productos y existencias", "box", 1),
            new ModuloDef("OPERACIONES", "Operaciones", "Entradas y ventas", "arrows-exchange", 2),
            new ModuloDef("CATALOGOS", "Catálogos", "Proveedores, marcas y categorías", "tags", 3),
            new ModuloDef("SEGURIDAD", "Seguridad", "Usuarios, roles y auditoría", "shield-lock", 4));
    private static final List<VistaDef> VISTAS = List.of(
            new VistaDef("Inventario", "/inventario", "Consulta y administración de productos.", "INVENTARIO"),
            new VistaDef("Entradas", "/entradas", "Recepciones de mercancía.", "OPERACIONES"),
            new VistaDef("Ventas y salidas", "/salidas", "Ventas y movimientos de salida.", "OPERACIONES"),
            new VistaDef("Proveedores", "/catalogos/proveedores", "Catálogo de proveedores.", "CATALOGOS"),
            new VistaDef("Marcas", "/catalogos/marcas", "Catálogo de marcas.", "CATALOGOS"),
            new VistaDef("Categorías", "/catalogos/categorias", "Catálogo de categorías.", "CATALOGOS"),
            new VistaDef("Usuarios", "/usuarios", "Administración de cuentas.", "SEGURIDAD"),
            new VistaDef("Roles", "/roles", "Catálogo de roles.", "SEGURIDAD"),
            new VistaDef("Permisos", "/roles/permisos", "Matriz de permisos por módulo y vista.", "SEGURIDAD"),
            new VistaDef("Auditoría", "/auditoria", "Historial de acciones del sistema.", "SEGURIDAD"));

    private final ModuloRepository moduloRepository;
    private final VistaRepository vistaRepository;
    private final PermisoRepository permisoRepository;
    private final RolRepository rolRepository;
    private final RolModuloRepository rolModuloRepository;
    private final VistaPermisoRepository vistaPermisoRepository;
    private final AuditService auditService;

    public RbacService(ModuloRepository moduloRepository,
                       VistaRepository vistaRepository,
                       PermisoRepository permisoRepository,
                       RolRepository rolRepository,
                       RolModuloRepository rolModuloRepository,
                       VistaPermisoRepository vistaPermisoRepository,
                       AuditService auditService) {
        this.moduloRepository = moduloRepository;
        this.vistaRepository = vistaRepository;
        this.permisoRepository = permisoRepository;
        this.rolRepository = rolRepository;
        this.rolModuloRepository = rolModuloRepository;
        this.vistaPermisoRepository = vistaPermisoRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public boolean usuarioTienePermiso(Integer usuarioId, String ruta, String accion) {
        return usuarioId != null && ruta != null && accion != null
                && vistaPermisoRepository.usuarioTienePermiso(usuarioId, ruta, accion);
    }

    @Transactional(readOnly = true)
    public List<Modulo> listarModulos() {
        return moduloRepository.findAll().stream()
                .sorted(java.util.Comparator.comparing(Modulo::getOrden).thenComparing(Modulo::getNombre))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Vista> listarVistas() {
        return vistaRepository.findAllByOrderByModulo_OrdenAscNombreAsc();
    }

    @Transactional(readOnly = true)
    public List<Permiso> listarPermisos() {
        return permisoRepository.findAll().stream()
                .sorted(java.util.Comparator.comparing(Permiso::getNombre))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<VistaPermiso> listarAsignaciones() {
        return vistaPermisoRepository.listarAsignaciones();
    }

    @Transactional(readOnly = true)
    public Set<String> clavesAsignadas(Integer rolId) {
        if (rolId == null) {
            return Set.of();
        }
        Set<String> asignadas = new HashSet<>();
        for (VistaPermiso asignacion : vistaPermisoRepository.findAllByRolModulo_Rol_Id(rolId)) {
            if (Boolean.TRUE.equals(asignacion.getPermitido())
                    && Boolean.TRUE.equals(asignacion.getRolModulo().getHabilitado())) {
                asignadas.add(clave(asignacion.getRolModulo().getModulo().getId(),
                        asignacion.getVista().getId(), asignacion.getPermiso().getId()));
            }
        }
        return asignadas;
    }

    @Transactional
    public void guardarAsignaciones(Integer rolId, List<String> claves, Integer actorId) {
        Rol rol = rolRepository.findById(rolId)
                .orElseThrow(() -> new IllegalArgumentException("No se encontró el rol seleccionado."));
        if (rol.getNombre().equalsIgnoreCase("ADMIN")) {
            throw new IllegalArgumentException("Los permisos del rol ADMIN están protegidos para evitar bloquear el acceso administrativo.");
        }

        Set<GrantKey> seleccionadas = parsearClaves(claves);
        Map<Integer, Modulo> modulos = moduloRepository.findAllById(
                        seleccionadas.stream().map(GrantKey::moduloId).distinct().toList())
                .stream().collect(java.util.stream.Collectors.toMap(Modulo::getId, m -> m));
        Map<Integer, Vista> vistas = vistaRepository.findAllById(
                        seleccionadas.stream().map(GrantKey::vistaId).distinct().toList())
                .stream().collect(java.util.stream.Collectors.toMap(Vista::getId, v -> v));
        Map<Integer, Permiso> permisos = permisoRepository.findAllById(
                        seleccionadas.stream().map(GrantKey::permisoId).distinct().toList())
                .stream().collect(java.util.stream.Collectors.toMap(Permiso::getId, p -> p));

        for (GrantKey key : seleccionadas) {
            Vista vista = vistas.get(key.vistaId());
            if (!modulos.containsKey(key.moduloId()) || vista == null || !permisos.containsKey(key.permisoId())
                    || !vista.getModulo().getId().equals(key.moduloId())) {
                throw new IllegalArgumentException("La selección contiene un módulo, vista o permiso inválido.");
            }
        }

        vistaPermisoRepository.deleteAllByRolModulo_Rol_Id(rolId);
        rolModuloRepository.deleteAllByRol_Id(rolId);

        Map<Integer, RolModulo> modulosPorId = new java.util.HashMap<>();
        for (GrantKey key : seleccionadas) {
            RolModulo rolModulo = modulosPorId.computeIfAbsent(key.moduloId(), moduloId -> {
                RolModulo nuevo = new RolModulo();
                nuevo.setRol(rol);
                nuevo.setModulo(modulos.get(moduloId));
                nuevo.setHabilitado(true);
                return rolModuloRepository.save(nuevo);
            });
            VistaPermiso asignacion = new VistaPermiso();
            asignacion.setRolModulo(rolModulo);
            asignacion.setVista(vistas.get(key.vistaId()));
            asignacion.setPermiso(permisos.get(key.permisoId()));
            asignacion.setPermitido(true);
            vistaPermisoRepository.save(asignacion);
        }
        auditService.registrar(actorId, "PERMISOS_ROL_ACTUALIZADOS", "r_modulo_rol", rolId);
    }

    @Transactional
    public void inicializarCatalogoYPermisos() {
        Map<String, Modulo> modulos = new java.util.HashMap<>();
        for (ModuloDef definicion : MODULOS) {
            Modulo modulo = moduloRepository.findByNombreIgnoreCase(definicion.nombre())
                    .orElseGet(() -> {
                        Modulo nuevo = new Modulo();
                        nuevo.setNombre(definicion.nombre());
                        return nuevo;
                    });
            modulo.setDescripcion(definicion.descripcion());
            modulo.setIcono(definicion.icono());
            modulo.setOrden(definicion.orden());
            modulos.put(definicion.codigo(), moduloRepository.save(modulo));
        }

        Map<String, Permiso> permisos = new java.util.HashMap<>();
        Map<String, String> descripciones = Map.of(
                CONSULTAR, "Consultar la vista y sus datos.",
                CREAR, "Crear registros desde la vista.",
                EDITAR, "Modificar registros de la vista.",
                ELIMINAR, "Eliminar registros de la vista.");
        for (String nombre : PERMISOS) {
            Permiso permiso = permisoRepository.findByNombreIgnoreCase(nombre)
                    .orElseGet(() -> {
                        Permiso nuevo = new Permiso();
                        nuevo.setNombre(nombre);
                        return nuevo;
                    });
            permiso.setDescripcion(descripciones.get(nombre));
            permisos.put(nombre, permisoRepository.save(permiso));
        }

        Map<String, Vista> vistas = new java.util.HashMap<>();
        for (VistaDef definicion : VISTAS) {
            Vista vista = vistaRepository.findByRutaAcceso(definicion.ruta())
                    .orElseGet(() -> {
                        Vista nueva = new Vista();
                        nueva.setRutaAcceso(definicion.ruta());
                        return nueva;
                    });
            vista.setNombre(definicion.nombre());
            vista.setDescripcion(definicion.descripcion());
            vista.setModulo(modulos.get(definicion.modulo()));
            vistas.put(definicion.ruta(), vistaRepository.save(vista));
        }

        grantAll("ADMIN", vistas, modulos, permisos);
        seedRoleDefaults("VENDEDOR", List.of(
                new DefaultGrant("/inventario", List.of(CONSULTAR)),
                new DefaultGrant("/entradas", List.of(CONSULTAR, CREAR)),
                new DefaultGrant("/salidas", List.of(CONSULTAR, CREAR))), vistas, modulos, permisos);
        seedRoleDefaults("USER", List.of(new DefaultGrant("/inventario", List.of(CONSULTAR))),
                vistas, modulos, permisos);
        seedRoleDefaults("EDITOR", List.of(new DefaultGrant("/inventario", List.of(CONSULTAR, CREAR, EDITAR))),
                vistas, modulos, permisos);
    }

    public static String clave(Integer moduloId, Integer vistaId, Integer permisoId) {
        return moduloId + ":" + vistaId + ":" + permisoId;
    }

    public static Optional<AccessRule> reglaPara(String path, String method, String productId) {
        String route = rutaVista(path);
        if (route == null) {
            return Optional.empty();
        }
        String action = accion(method);
        if (path.equals("/productos/guardar") && "POST".equalsIgnoreCase(method)) {
            action = productId == null || productId.isBlank() ? CREAR : EDITAR;
        }
        if (path.startsWith("/productos/editar/") && "GET".equalsIgnoreCase(method)) {
            action = EDITAR;
        }
        if (path.startsWith("/productos/eliminar/") && "POST".equalsIgnoreCase(method)) {
            action = ELIMINAR;
        }
        if (path.endsWith("/actualizar") && "POST".equalsIgnoreCase(method)) {
            action = EDITAR;
        }
        if (path.equals("/usuarios/rol") || path.equals("/usuarios/estado")
                || path.equals("/roles/permisos/guardar")) {
            action = EDITAR;
        }
        if (path.endsWith("/eliminar") && "POST".equalsIgnoreCase(method)
                || path.startsWith("/productos/eliminar/") && "POST".equalsIgnoreCase(method)) {
            action = ELIMINAR;
        }
        return Optional.of(new AccessRule(route, action));
    }

    private static String rutaVista(String path) {
        if (path.equals("/") || path.equals("/productos/guardar")
                || path.startsWith("/productos/editar/") || path.startsWith("/productos/eliminar/")
                || path.startsWith("/api/productos")) {
            return "/inventario";
        }
        if (path.equals("/entradas") || path.equals("/entradas/guardar")
                || path.startsWith("/api/entradas") || path.startsWith("/api/detalles-entradas")) {
            return "/entradas";
        }
        if (path.startsWith("/salidas")
                || path.startsWith("/api/salidas") || path.startsWith("/api/detalles-salidas")) {
            return "/salidas";
        }
        if (path.startsWith("/catalogos/proveedores") || path.startsWith("/api/proveedores")) {
            return "/catalogos/proveedores";
        }
        if (path.startsWith("/catalogos/marcas") || path.startsWith("/api/marcas")) {
            return "/catalogos/marcas";
        }
        if (path.startsWith("/catalogos/categorias") || path.startsWith("/api/categorias")) {
            return "/catalogos/categorias";
        }
        if (path.startsWith("/usuarios") || path.startsWith("/api/usuarios")) {
            return "/usuarios";
        }
        if (path.startsWith("/roles/permisos")) {
            return "/roles/permisos";
        }
        if (path.equals("/roles") || path.startsWith("/roles/") || path.startsWith("/api/roles")) {
            return "/roles";
        }
        if (path.equals("/auditoria")) {
            return "/auditoria";
        }
        return null;
    }

    private static String accion(String method) {
        return switch (method.toUpperCase(Locale.ROOT)) {
            case "GET", "HEAD" -> CONSULTAR;
            case "POST" -> CREAR;
            case "PUT", "PATCH" -> EDITAR;
            case "DELETE" -> ELIMINAR;
            default -> ELIMINAR;
        };
    }

    private void grantAll(String roleName, Map<String, Vista> vistas,
                          Map<String, Modulo> modulos, Map<String, Permiso> permisos) {
        for (Vista vista : vistas.values()) {
            for (String permiso : PERMISOS) {
                grant(roleName, vista.getRutaAcceso(), List.of(permiso), vistas, modulos, permisos);
            }
        }
    }

    private void grant(String roleName, String route, List<String> actions,
                       Map<String, Vista> vistas, Map<String, Modulo> modulos,
                       Map<String, Permiso> permisos) {
        Rol rol = rolRepository.findByNombreIgnoreCase(roleName).orElse(null);
        Vista vista = vistas.get(route);
        if (rol == null || vista == null) {
            return;
        }
        Modulo modulo = vista.getModulo();
        RolModulo rolModulo = rolModuloRepository.findByRol_IdAndModulo_Id(rol.getId(), modulo.getId())
                .orElseGet(() -> {
                    RolModulo nuevo = new RolModulo();
                    nuevo.setRol(rol);
                    nuevo.setModulo(modulo);
                    nuevo.setHabilitado(true);
                    return rolModuloRepository.save(nuevo);
                });
        rolModulo.setHabilitado(true);
        rolModulo = rolModuloRepository.save(rolModulo);
        RolModulo assignedModule = rolModulo;
        for (String action : actions) {
            Permiso permiso = permisos.get(action);
            VistaPermiso asignacion = vistaPermisoRepository
                    .findByRolModulo_IdAndVista_IdAndPermiso_Id(assignedModule.getId(), vista.getId(), permiso.getId())
                    .orElseGet(() -> {
                        VistaPermiso nuevo = new VistaPermiso();
                        nuevo.setRolModulo(assignedModule);
                        nuevo.setVista(vista);
                        nuevo.setPermiso(permiso);
                        return nuevo;
                    });
            asignacion.setPermitido(true);
            vistaPermisoRepository.save(asignacion);
        }
    }

    private void seedRoleDefaults(String roleName, List<DefaultGrant> defaults,
                                  Map<String, Vista> vistas, Map<String, Modulo> modulos,
                                  Map<String, Permiso> permisos) {
        Rol rol = rolRepository.findByNombreIgnoreCase(roleName).orElse(null);
        if (rol == null || !rolModuloRepository.findAllByRol_Id(rol.getId()).isEmpty()) {
            return;
        }
        for (DefaultGrant grant : defaults) {
            grant(roleName, grant.route(), grant.actions(), vistas, modulos, permisos);
        }
    }

    private Set<GrantKey> parsearClaves(List<String> keys) {
        Set<GrantKey> result = new LinkedHashSet<>();
        if (keys == null) {
            return result;
        }
        for (String key : keys) {
            if (key == null) {
                throw new IllegalArgumentException("La selección de permisos contiene un valor inválido.");
            }
            String[] parts = key.split(":", -1);
            if (parts.length != 3) {
                throw new IllegalArgumentException("La selección de permisos contiene un valor inválido.");
            }
            try {
                result.add(new GrantKey(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]),
                        Integer.parseInt(parts[2])));
            } catch (NumberFormatException exception) {
                throw new IllegalArgumentException("La selección de permisos contiene un valor inválido.", exception);
            }
        }
        return result;
    }

    public record AccessRule(String route, String action) {}
    private record GrantKey(Integer moduloId, Integer vistaId, Integer permisoId) {}
    private record ModuloDef(String codigo, String nombre, String descripcion, String icono, int orden) {}
    private record VistaDef(String nombre, String ruta, String descripcion, String modulo) {}
    private record DefaultGrant(String route, List<String> actions) {}
}
