import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Producto {
    private static final Path DATA_FILE = Paths.get("productos.txt");
    private static final List<ProductoItem> productos = new ArrayList<>();
    private static int ultimoId = 0;

    public static void main(String[] args) {
        cargarProductos();

        try (Scanner scanner = new Scanner(System.in)) {
            while (true) {
                System.out.println("\n=== CRUD Productos ===");
                System.out.println("1. Listar productos");
                System.out.println("2. Insertar producto");
                System.out.println("3. Actualizar producto");
                System.out.println("4. Eliminar producto");
                System.out.println("5. Salir");
                System.out.print("Seleccione una opción: ");

                int opcion;
                try {
                    opcion = scanner.nextInt();
                } catch (Exception e) {
                    scanner.nextLine();
                    System.out.println("Opción inválida.");
                    continue;
                }
                scanner.nextLine();

                switch (opcion) {
                    case 1 -> listarProductos();
                    case 2 -> insertarProducto(scanner);
                    case 3 -> actualizarProducto(scanner);
                    case 4 -> eliminarProducto(scanner);
                    case 5 -> {
                        guardarProductos();
                        System.out.println("Saliendo...");
                        return;
                    }
                    default -> System.out.println("Opción inválida.");
                }
            }
        }
    }

    private static void cargarProductos() {
        if (!Files.exists(DATA_FILE)) {
            return;
        }

        try {
            List<String> lineas = Files.readAllLines(DATA_FILE);
            for (String linea : lineas) {
                if (linea.trim().isEmpty()) {
                    continue;
                }
                String[] parte = linea.split("\\|");
                if (parte.length != 4) {
                    continue;
                }
                int id = Integer.parseInt(parte[0]);
                String nombre = parte[1];
                double precio = Double.parseDouble(parte[2]);
                int stock = Integer.parseInt(parte[3]);
                productos.add(new ProductoItem(id, nombre, precio, stock));
                ultimoId = Math.max(ultimoId, id);
            }
        } catch (IOException e) {
            System.out.println("No se pudieron cargar los productos: " + e.getMessage());
        }
    }

    private static void guardarProductos() {
        try {
            List<String> lineas = new ArrayList<>();
            for (ProductoItem p : productos) {
                lineas.add(p.id + "|" + p.nombre + "|" + p.precio + "|" + p.stock);
            }
            Files.write(DATA_FILE, lineas, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            System.out.println("No se pudieron guardar los productos: " + e.getMessage());
        }
    }

    private static void listarProductos() {
        if (productos.isEmpty()) {
            System.out.println("No hay productos registrados.");
            return;
        }

        System.out.println("\nProductos:");
        for (ProductoItem p : productos) {
            System.out.println("ID: " + p.id +
                    " | Nombre: " + p.nombre +
                    " | Precio: " + p.precio +
                    " | Stock: " + p.stock);
        }
    }

    private static void insertarProducto(Scanner scanner) {
        System.out.print("Nombre: ");
        String nombre = scanner.nextLine();
        System.out.print("Precio: ");
        double precio = scanner.nextDouble();
        System.out.print("Stock: ");
        int stock = scanner.nextInt();
        scanner.nextLine();

        ultimoId++;
        productos.add(new ProductoItem(ultimoId, nombre, precio, stock));
        guardarProductos();
        System.out.println("Producto insertado correctamente.");
    }

    private static void actualizarProducto(Scanner scanner) {
        System.out.print("ID del producto a actualizar: ");
        int id = scanner.nextInt();
        scanner.nextLine();
        System.out.print("Nuevo nombre: ");
        String nombre = scanner.nextLine();
        System.out.print("Nuevo precio: ");
        double precio = scanner.nextDouble();
        System.out.print("Nuevo stock: ");
        int stock = scanner.nextInt();
        scanner.nextLine();

        for (ProductoItem p : productos) {
            if (p.id == id) {
                p.nombre = nombre;
                p.precio = precio;
                p.stock = stock;
                guardarProductos();
                System.out.println("Producto actualizado.");
                return;
            }
        }

        System.out.println("No se encontró el producto.");
    }

    private static void eliminarProducto(Scanner scanner) {
        System.out.print("ID del producto a eliminar: ");
        int id = scanner.nextInt();
        scanner.nextLine();

        for (int i = 0; i < productos.size(); i++) {
            if (productos.get(i).id == id) {
                productos.remove(i);
                guardarProductos();
                System.out.println("Producto eliminado.");
                return;
            }
        }

        System.out.println("No se encontró el producto.");
    }

    private static class ProductoItem {
        int id;
        String nombre;
        double precio;
        int stock;

        ProductoItem(int id, String nombre, double precio, int stock) {
            this.id = id;
            this.nombre = nombre;
            this.precio = precio;
            this.stock = stock;
        }
    }
}
