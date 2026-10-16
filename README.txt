PROFESOR README

Integrantes:
- Luis Gonzalez
- Kevin Gonzalez
- Yorgelis Pacheco
- Andres Rondon
- Itzamar Diaz
- Oliver Valbuena

Seccion: 12
Trayecto: 2-2

SAFARI

Sistema de inventario construido con Java 17 y Spring Boot. Administra productos,
usuarios, roles, proveedores, marcas, categorías, entradas y ventas.

Inicio rápido:
1. Inicia MySQL en XAMPP.
2. Importa safari.sql en phpMyAdmin para crear el esquema y las tablas.
3. Construye el JAR con .\apache-maven-3.9.9\bin\mvn.cmd clean package
4. Desde la carpeta target, ejecuta java -jar safari-1.0.0.jar
5. Abre http://localhost:8096/

El JAR incluye Apache Tomcat integrado. No requiere un .bat ni instalar un Tomcat
externo; el equipo donde se ejecute sí necesita Java 17 y acceso a MySQL.

El modelo tiene 11 tablas. Incluye usuario_roles como tabla puente N:M para
usuarios y roles, y tablas de detalle para entradas-productos y salidas-productos.
No tiene una relación 1:1 porque el sistema no necesita una actualmente.
Para conservar los datos de una base safari existente, detén SAFARI, respalda la
BD y ejecuta una sola vez migracion_roles_multiples.sql antes de iniciar esta versión.

Revisa README.md para la configuración completa, las cuentas de demostración,
los permisos por rol y los endpoints de la API.
