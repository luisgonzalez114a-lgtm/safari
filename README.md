# SAFARI

Aplicación Java Spring Boot para gestionar productos, categorías, marcas, usuarios, proveedores, entradas de mercancía y salidas usando MySQL/MariaDB de XAMPP.

## Requisitos

- Java 17+
- Maven incluido en la carpeta `apache-maven-3.9.9`
- XAMPP con MySQL activo
- Base de datos `safari`

## Configuración de base de datos

La aplicación usa la base de datos `safari`. En phpMyAdmin:

1. Activa MySQL en XAMPP.
2. Importa `safari.sql` desde la pestaña **Importar**. El script crea y selecciona la base de datos `safari`.
3. Si existe una base de datos antigua `safari`, expórtala primero como respaldo. Para reemplazarla, elimínala desde phpMyAdmin y luego importa el archivo actualizado.
4. Verifica las credenciales en `src/main/resources/application.properties`. La configuración predeterminada de XAMPP es usuario `root` y contraseña vacía.

`crear_bd_safari.sql` solo crea la base de datos; para crear las tablas, importa `safari.sql`.

La aplicación valida las tablas al iniciar; no las crea ni las modifica automáticamente.

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/safari?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
spring.datasource.username=root
spring.datasource.password=
```

## Ejecutar

Con MySQL activo y `safari.sql` importado, puedes iniciar SAFARI haciendo
doble clic en `start-app.bat`. El archivo abre una ventana con los logs,
espera a que la aplicación esté lista y después abre el login en el navegador.
Para detenerla, pulsa `Ctrl+C` en la ventana **SAFARI - Servidor**.

También puedes iniciarla desde PowerShell:

```powershell
.\apache-maven-3.9.9\bin\mvn.cmd spring-boot:run
```

La app queda en:

- http://localhost:8096

Para ejecutar las pruebas automatizadas (usan H2 en memoria y no necesitan XAMPP):

```powershell
.\apache-maven-3.9.9\bin\mvn.cmd test
```

## Endpoints de la API

- `/api/productos`
- `/api/categorias`
- `/api/marcas`
- `/api/proveedores`
- `/api/roles`
- `/api/usuarios`
- `/api/entradas`
- `/api/detalles-entradas`
- `/api/salidas`
- `/api/detalles-salidas`

Los endpoints de detalles reciben las relaciones como objetos con su `id`; por ejemplo, una línea de entrada incluye `entrada: {"id": 1}` y `producto: {"id": 2}`.

## Interfaz

Las pantallas usan estilos locales de Tabler 1.6.1, adaptados al tema oscuro azul de SAFARI. No requieren Node.js ni conexión a internet al iniciar la aplicación. La licencia MIT de Tabler se incluye en `src/main/resources/static/css/TABLER-LICENSE.txt`.

## Roles y operación diaria

- **Cliente:** puede abrir `/` sin iniciar sesión y consultar el catálogo, precios y existencias. No puede operar la caja ni modificar inventario.
- **VENDEDOR:** puede registrar entradas de mercancía y ventas desde `/entradas` y `/salidas`. La entrada suma existencias; la venta usa el precio actual y descuenta stock en una transacción, sin permitir sobreventa.
- **ADMIN:** puede registrar/editar productos, administrar usuarios y roles, crear proveedores/marcas/categorías, y también registrar movimientos.

Desde **Proveedores y catálogos**, ADMIN puede editar o eliminar marcas, categorías y proveedores. No se permite eliminar una marca o categoría asignada a productos, ni un proveedor con entradas registradas.

Los movimientos se escriben desde las pantallas de operación para que encabezado, detalle y cambio de existencias queden coordinados. Las rutas API de movimientos son de consulta; las rutas API de escritura de productos y catálogos requieren un usuario `ADMIN` con sesión.

Las pruebas de integración cubren el CRUD de productos, categorías, marcas, proveedores,
usuarios y roles; el control de acceso por rol; y el flujo de recepción, venta, actualización
de existencias y consulta de los historiales de movimientos y sus detalles.

## Acceso inicial para pruebas

Al iniciar se crean, si faltan, los roles, datos de catálogos de prueba y estas cuentas:

- `admin@safari.com` / `admin123`
- `usuario@safari.com` / `user123`
- `vendedor@safari.com` / `vendedor123`

Las contraseñas nuevas se guardan con hash BCrypt. Las contraseñas existentes en texto plano se convierten a hash al iniciar sesión correctamente. Cambia las credenciales de demostración antes de usar información real.
