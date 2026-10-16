# SAFARI

Aplicación Java Spring Boot para gestionar productos, categorías, marcas, usuarios, proveedores, entradas de mercancía y salidas usando MySQL/MariaDB de XAMPP.

## Requisitos

- Java 17+
- Maven incluido en la carpeta `apache-maven-3.9.9`
- XAMPP con MySQL activo
- Base de datos `safari`

## Modelo relacional para exponer

SAFARI contiene 11 tablas. El esquema incluye relaciones uno a muchos y muchos a muchos; no tiene una relación uno a uno porque no hace falta para las funciones actuales.

```mermaid
erDiagram
    USUARIOS ||--o{ USUARIO_ROLES : tiene
    ROLES ||--o{ USUARIO_ROLES : asignado
    USUARIOS ||--o{ ENTRADAS : registra
    PROVEEDORES ||--o{ ENTRADAS : suministra
    ENTRADAS ||--o{ DETALLES_ENTRADAS : contiene
    PRODUCTOS ||--o{ DETALLES_ENTRADAS : incluido_en
    USUARIOS ||--o{ SALIDAS : registra
    SALIDAS ||--o{ DETALLES_SALIDAS : contiene
    PRODUCTOS ||--o{ DETALLES_SALIDAS : vendido_en
    MARCAS o|--o{ PRODUCTOS : clasifica
    CATEGORIAS o|--o{ PRODUCTOS : clasifica
```

- **Muchos a muchos (N:M):** un usuario puede tener varios roles y un rol puede asignarse a varios usuarios. `usuario_roles` es la tabla puente. Los permisos de todos los roles asignados se combinan.
- **Uno a muchos (1:N):** hay relaciones de proveedores a entradas, usuarios a entradas/salidas, marcas/categorías a productos y movimientos a sus detalles.
- **Muchos a muchos (N:M):** una entrada puede contener varios productos y un producto aparecer en muchas entradas. `detalles_entradas` es la tabla intermedia; guarda también cantidad y precio de compra. `detalles_salidas` cumple la misma función entre salidas y productos, y guarda cantidad y precio de venta.
- **Uno a uno (1:1):** no existe actualmente. No conviene agregar una tabla solo para afirmar que está; se añadiría si aparece un requisito real, por ejemplo datos confidenciales de perfil que deban almacenarse aparte del usuario.

En el modelo entidad-relación, cada fila puente vincula un elemento de cada lado. Así, cada relación N:M se implementa en la base como dos relaciones 1:N alrededor de su tabla intermedia.

## Configuración de base de datos

La aplicación usa la base de datos `safari`. En phpMyAdmin:

1. Activa MySQL en XAMPP.
2. Importa `safari.sql` desde la pestaña **Importar**. El script crea y selecciona la base de datos `safari`.
3. Si ya tienes una base `safari` con datos, **no la elimines ni vuelvas a importar `safari.sql`**. Detén SAFARI, exporta primero un respaldo y ejecuta una sola vez `migracion_roles_multiples.sql` en phpMyAdmin. El script conserva los roles actuales de cada usuario en `usuario_roles` antes de retirar `usuarios.id_rol`.
4. Verifica las credenciales en `src/main/resources/application.properties`. La configuración predeterminada de XAMPP es usuario `root` y contraseña vacía.

`crear_bd_safari.sql` solo crea la base de datos; para crear las tablas, importa `safari.sql`.

La aplicación valida las tablas al iniciar; no las crea ni las modifica automáticamente. La URL y las credenciales pueden cambiarse con `SAFARI_DB_URL`, `SAFARI_DB_USERNAME` y `SAFARI_DB_PASSWORD`; el puerto web puede cambiarse con `PORT`. Si no defines esas variables, se usan los valores locales de XAMPP indicados abajo.

```properties
SAFARI_DB_URL=jdbc:mysql://localhost:3306/safari?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
SAFARI_DB_USERNAME=root
SAFARI_DB_PASSWORD=
PORT=8096
```

## Ejecutar

Con MySQL activo y `safari.sql` importado, crea el JAR ejecutable desde la carpeta del proyecto:

```powershell
.\apache-maven-3.9.9\bin\mvn.cmd clean package
```

Spring Boot incluye **Apache Tomcat integrado** en el JAR: no tienes que instalar o iniciar un Tomcat externo ni ejecutar un `.bat`. Copia `target/safari-1.0.0.jar` a otra carpeta o equipo que tenga Java 17 y ejecuta:

```powershell
java -jar safari-1.0.0.jar
```

En Windows también puedes abrir PowerShell desde la carpeta del JAR y usar el mismo comando. Para detener el servidor, pulsa `Ctrl+C` en esa terminal. La base MySQL debe estar disponible desde el equipo donde se inicia la aplicación.

La app queda en:

- http://localhost:8096

Para usar otra base de datos o puerto, configura las variables `SAFARI_DB_URL`, `SAFARI_DB_USERNAME`, `SAFARI_DB_PASSWORD` y `PORT` en el entorno antes de ejecutar el JAR.

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

Las pantallas se renderizan con Thymeleaf desde Java y usan estilos locales de Tabler 1.6.1, adaptados al tema oscuro azul de SAFARI. No usan React ni requieren JavaScript, Node.js o conexión a internet al iniciar la aplicación. La licencia MIT de Tabler se incluye en `src/main/resources/static/css/TABLER-LICENSE.txt`.

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
