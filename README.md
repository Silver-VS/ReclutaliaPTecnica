# Desarrollo del Servicio de Gestión de Empleados
**Author:** Silver VS

## 1. Objetivo
Implementar un backend en Java que proporcione:
- Persistencia de entidades `Employee` mediante procedimientos almacenados en MySQL.
- API REST CRUD sin frameworks externos.
- Cálculo del top 10 de salarios a partir de archivos JSON procesados en paralelo.
- Despliegue en AWS (RDS, Secrets Manager, Lambda y API Gateway).

---

## 2. Alcance
- **Almacenamiento**: operaciones Create, Read, Update y Delete sobre `Employee`.
- **Procesamiento complementario**: lectura concurrente de tres archivos JSON de salarios y selección de los 10 valores más altos.
- **Infraestructura**: configuración de AWS para garantizar seguridad y escalabilidad.
- **Artefactos**: scripts SQL, colección Postman y evidencias de despliegue.

---

## 3. Tecnologías, Herramientas y Entornos de Desarrollo
- **Control de versiones y CI/CD**: GitHub
- **IDE**: IntelliJ IDEA
- **Base de datos**: MySQL 8.x
- **Lenguaje y build**: Java 21, Maven
- **Concurrencia**: `ExecutorService` de Java
- **AWS**: RDS MySQL, Secrets Manager, Lambda, API Gateway
- **Serialización JSON**: Jackson
- **Pruebas**: Postman y cURL
- **Gestión de dependencias**: GitHub Actions para compilación y pruebas automatizadas
- **Control de calidad**: Análisis estático con IntelliJ IDEA Inspections

---

## 4. Estructura del Repositorio
```text
project-root/
├── src/
│ ├── main/java/… # Código fuente Java
│ └── main/resources/… # Configuraciones y credenciales
├── sql/
│ ├── ddl/ # Scripts DDL
│ └── sp/ # Stored procedures CRUD
├── json/ # Archivos de salarios
├── postman/ # Colección Postman
├── pom.mxl / # Configuración de Maven
└── README.md # Visión general
```

---

## 5. Fases de Desarrollo

### 5.1. Modelado y Creación de la Base de Datos
1. Definición de la tabla `employees` con los campos:
    - `id` INT AUTO_INCREMENT PK
    - `first_name`, `last_name`, `email`, `salary`
2. Elaboración de scripts DDL (`sql/ddl/`).
3. Desarrollo de procedimientos almacenados (`sql/sp/`):
    - `sp_create_employee`
    - `sp_get_employee_by_id`
    - `sp_update_employee`
    - `sp_delete_employee`
    - `sp_list_employees`

### 5.2. Configuración de Conexión en Java
1. Inclusión de dependencias JDBC y AWS SDK en `pom.xml`.
2. Recuperación de credenciales desde AWS Secrets Manager.
3. Creación de clase utilitaria para gestión de `DataSource`.

### 5.3. Capa de Persistencia
1. Definición de la interfaz `EmployeeDAO` con métodos CRUD.
2. Implementación con `CallableStatement` para invocar los procedimientos.
3. Gestión de errores y adherencia a principios SOLID.

### 5.4. API REST Local
1. Utilización de `com.sun.net.httpserver.HttpServer`.
2. Mapeo de rutas y handlers para:
    - `POST /employees`
    - `GET /employees`
    - `GET /employees/{id}`
    - `PUT /employees/{id}`
    - `DELETE /employees/{id}`
3. Serialización/deserialización JSON.
4. Validación con Postman o cURL.

### 5.5. Endpoint de Salarios
1. Lectura concurrente de `json/salary1.json`, `salary2.json`, `salary3.json`.
2. Unión y ordenamiento de los datos de salario.
3. Devolución de los 10 valores más altos en formato JSON.

### 5.6. Despliegue en AWS
1. Creación de instancia RDS y configuración de la base de datos.
2. Carga de scripts DDL y SP en RDS.
3. Almacenamiento de credenciales en Secrets Manager.
4. Empaquetado del proyecto como JAR (uber-jar).
5. Configuración de AWS Lambda con acceso a RDS y Secrets Manager.
6. Definición de API REST en API Gateway (proxy a Lambda).
7. Pruebas finales y captura de evidencias.

### 5.7. Documentación y Entrega
- Repositorio con carpetas `sql/`, `json/` y `postman/`.
- Instrucciones de despliegue y capturas de consola en `docs/`.
- Archivo `README.md` con resumen de la arquitectura y pasos de ejecución.

---

## 6. Paso Inmediato
Definir y validar el script DDL de la tabla `employees` antes de proceder a la implementación de los stored procedures.

---

*26 de junio de 2025*
