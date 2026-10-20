package com.example.productos.config;

import com.example.productos.model.Categoria;
import com.example.productos.model.Marca;
import com.example.productos.model.Rol;
import com.example.productos.model.Usuario;
import com.example.productos.model.Proveedor;
import com.example.productos.repository.CategoriaRepository;
import com.example.productos.repository.MarcaRepository;
import com.example.productos.repository.RolRepository;
import com.example.productos.repository.UsuarioRepository;
import com.example.productos.repository.ProveedorRepository;
import com.example.productos.service.RbacService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataSeeder {

    @Bean
    CommandLineRunner initData(RolRepository rolRepository,
                               UsuarioRepository usuarioRepository,
                               MarcaRepository marcaRepository,
                               CategoriaRepository categoriaRepository,
                               ProveedorRepository proveedorRepository,
                               RbacService rbacService,
                               PasswordEncoder passwordEncoder,
                               @Value("${safari.admin.initial-password:admin123}") String adminInitialPassword,
                               @Value("${safari.seed.demo-users:true}") boolean seedDemoUsers) {
        return args -> {
            Rol adminRol = rolRepository.findAll().stream()
                    .filter(r -> r.getNombre() != null && r.getNombre().equalsIgnoreCase("ADMIN"))
                    .findFirst()
                    .orElseGet(() -> {
                        Rol nuevoRol = new Rol();
                        nuevoRol.setNombre("ADMIN");
                        return rolRepository.save(nuevoRol);
                    });

            Rol userRol = rolRepository.findAll().stream()
                    .filter(r -> r.getNombre() != null && r.getNombre().equalsIgnoreCase("USER"))
                    .findFirst()
                    .orElseGet(() -> {
                        Rol nuevoRol = new Rol();
                        nuevoRol.setNombre("USER");
                        return rolRepository.save(nuevoRol);
                    });

            Rol editorRol = rolRepository.findAll().stream()
                    .filter(r -> r.getNombre() != null && r.getNombre().equalsIgnoreCase("EDITOR"))
                    .findFirst()
                    .orElseGet(() -> {
                        Rol nuevoRol = new Rol();
                        nuevoRol.setNombre("EDITOR");
                        return rolRepository.save(nuevoRol);
                    });

            Rol vendedorRol = rolRepository.findAll().stream()
                    .filter(r -> r.getNombre() != null && r.getNombre().equalsIgnoreCase("VENDEDOR"))
                    .findFirst()
                    .orElseGet(() -> {
                        Rol nuevoRol = new Rol();
                        nuevoRol.setNombre("VENDEDOR");
                        return rolRepository.save(nuevoRol);
                    });

            if (marcaRepository.findAll().isEmpty()) {
                Marca marcaGeneral = new Marca();
                marcaGeneral.setNombre("General");
                marcaGeneral.setEstatus(1);
                marcaRepository.save(marcaGeneral);
            }

            if (categoriaRepository.findAll().isEmpty()) {
                Categoria categoriaGeneral = new Categoria();
                categoriaGeneral.setNombre("General");
                categoriaGeneral.setDescripcion("Categoría por defecto");
                categoriaGeneral.setEstatus(1);
                categoriaRepository.save(categoriaGeneral);
            }

            if (proveedorRepository.findAll().isEmpty()) {
                Proveedor proveedorGeneral = new Proveedor();
                proveedorGeneral.setNombre("Proveedor de prueba");
                proveedorGeneral.setRifNit("DEMO-001");
                proveedorGeneral.setTelefono("555-0102");
                proveedorRepository.save(proveedorGeneral);
            }

            if (usuarioRepository.findByCorreo("admin@safari.com").isEmpty()) {
                Usuario admin = new Usuario();
                admin.setNombre("Administrador");
                admin.setTelefono("0000000000");
                admin.setCorreo("admin@safari.com");
                admin.setClave(passwordEncoder.encode(adminInitialPassword));
                admin.setRoles(java.util.Set.of(adminRol));
                admin.setEstatus(1);
                usuarioRepository.save(admin);
            }

            if (seedDemoUsers && usuarioRepository.findByCorreo("usuario@safari.com").isEmpty()) {
                Usuario usuario = new Usuario();
                usuario.setNombre("Usuario normal");
                usuario.setTelefono("1111111111");
                usuario.setCorreo("usuario@safari.com");
                usuario.setClave(passwordEncoder.encode("user123"));
                usuario.setRoles(java.util.Set.of(userRol));
                usuario.setEstatus(1);
                usuarioRepository.save(usuario);
            }

            if (seedDemoUsers && usuarioRepository.findByCorreo("vendedor@safari.com").isEmpty()) {
                Usuario vendedor = new Usuario();
                vendedor.setNombre("Vendedor de prueba");
                vendedor.setTelefono("555-0101");
                vendedor.setCorreo("vendedor@safari.com");
                vendedor.setClave(passwordEncoder.encode("vendedor123"));
                vendedor.setRoles(java.util.Set.of(vendedorRol));
                vendedor.setEstatus(1);
                usuarioRepository.save(vendedor);
            }

            rbacService.inicializarCatalogoYPermisos();
        };
    }
}
