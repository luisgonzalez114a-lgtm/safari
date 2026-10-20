package com.example.productos.model;

import jakarta.persistence.*;

@Entity
@Table(name = "vistas_permiso",
        uniqueConstraints = @UniqueConstraint(name = "uq_vistas_permiso",
                columnNames = {"r_modulo_rol_id", "vista_id", "permiso_id"}))
public class VistaPermiso {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "r_modulo_rol_id", nullable = false)
    private RolModulo rolModulo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vista_id", nullable = false)
    private Vista vista;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "permiso_id", nullable = false)
    private Permiso permiso;

    @Column(nullable = false)
    private Boolean permitido = true;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public RolModulo getRolModulo() {
        return rolModulo;
    }

    public void setRolModulo(RolModulo rolModulo) {
        this.rolModulo = rolModulo;
    }

    public Vista getVista() {
        return vista;
    }

    public void setVista(Vista vista) {
        this.vista = vista;
    }

    public Permiso getPermiso() {
        return permiso;
    }

    public void setPermiso(Permiso permiso) {
        this.permiso = permiso;
    }

    public Boolean getPermitido() {
        return permitido;
    }

    public void setPermitido(Boolean permitido) {
        this.permitido = permitido;
    }
}
