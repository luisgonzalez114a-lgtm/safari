package com.example.productos.repository;

import com.example.productos.model.VistaPermiso;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface VistaPermisoRepository extends JpaRepository<VistaPermiso, Integer> {
    Optional<VistaPermiso> findByRolModulo_IdAndVista_IdAndPermiso_Id(
            Integer rolModuloId, Integer vistaId, Integer permisoId);

    @EntityGraph(attributePaths = {"rolModulo", "vista", "permiso"})
    List<VistaPermiso> findAllByRolModulo_Rol_Id(Integer rolId);

    void deleteAllByRolModulo_Rol_Id(Integer rolId);

    @Query("""
            select count(vp) > 0
            from VistaPermiso vp
            join vp.rolModulo rm
            join rm.rol r
            join r.usuarios u
            join vp.vista v
            join vp.permiso p
            where u.id = :usuarioId
              and v.rutaAcceso = :ruta
              and upper(p.nombre) = upper(:accion)
              and rm.habilitado = true
              and vp.permitido = true
            """)
    boolean usuarioTienePermiso(@Param("usuarioId") Integer usuarioId,
                                @Param("ruta") String ruta,
                                @Param("accion") String accion);

    @Query("""
            select vp
            from VistaPermiso vp
            join fetch vp.rolModulo rm
            join fetch rm.rol r
            join fetch rm.modulo m
            join fetch vp.vista v
            join fetch vp.permiso p
            order by r.nombre, m.orden, v.nombre, p.nombre
            """)
    List<VistaPermiso> listarAsignaciones();
}
