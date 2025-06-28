/*
 * DatabaseConfig.java
 * ---------------------------------------------------------------------------
 * Copyright © 2025 Silver VS
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 * OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL
 * THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 * ---------------------------------------------------------------------------
 */

package config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Configuración de {@link DataSource} para la base de datos de empleados.
 *
 * <p>
 * - Si {@code USE_AWS_SECRETS=true}, fuerza el uso de AWS Secrets Manager
 *   para recuperar las credenciales.
 * - En otro caso, intenta usar {@code DB_URL}, {@code DB_USER} y {@code DB_PASS}.
 *   Si la URL está ausente, vacía o apunta a {@code localhost}, también recurre
 *   a AWS Secrets Manager.
 * </p>
 *
 * <p>
 * Incluye un método {@code main()} para verificar localmente la recuperación
 * de credenciales y la conectividad a la base de datos.
 * </p>
 *
 * @author Silver
 * @version 1.4
 * @since 2025-06-27
 */
public final class DatabaseConfig {

    private static final Logger LOG = LoggerFactory.getLogger(DatabaseConfig.class);
    private static HikariDataSource dataSource;

    private DatabaseConfig() {
        // Prevent instantiation
    }

    /**
     * Inicializa (si es necesario) y devuelve un {@link DataSource} configurado
     * con HikariCP para la base de datos de empleados.
     *
     * @return {@link DataSource} configurado
     */
    public static DataSource getDataSource() {
        if (dataSource == null) {
            boolean forceAws = Boolean.parseBoolean(
                    System.getenv().getOrDefault("USE_AWS_SECRETS",
                            System.getProperty("USE_AWS_SECRETS", "false")
                    ).trim()
            );

            String urlEnv  = System.getenv("DB_URL");
            String userEnv = System.getenv("DB_USER");
            String passEnv = System.getenv("DB_PASS");

            // Aquí leemos props si no están en env
            String url  = (urlEnv  != null && !urlEnv.isBlank())
                    ? urlEnv
                    : System.getProperty("DB_URL");
            String user = (userEnv != null && !userEnv.isBlank())
                    ? userEnv
                    : System.getProperty("DB_USER");
            String pass = (passEnv != null && !passEnv.isBlank())
                    ? passEnv
                    : System.getProperty("DB_PASS");

            if (forceAws || url == null || url.isBlank()) {
                LOG.info(
                        forceAws
                                ? "USE_AWS_SECRETS=true → obteniendo credenciales de AWS Secrets Manager"
                                : "DB_URL ausente o local ('{}') → obteniendo credenciales de AWS Secrets Manager",
                        url
                );
                Map<String,String> secret = fetchSecretFromAWS();
                url  = secret.get("DB_URL");
                user = secret.get("DB_USER");
                pass = secret.get("DB_PASS");
            } else {
                LOG.info("Usando DB_URL de entorno/prop: {}", url);
            }

            HikariConfig cfg = new HikariConfig();
            cfg.setJdbcUrl(url);
            cfg.setUsername(user);
            cfg.setPassword(pass);
            cfg.setDriverClassName("com.mysql.cj.jdbc.Driver");
            cfg.setInitializationFailTimeout(-1); // no fail-fast
            cfg.setMinimumIdle(0);               // no conexiones iniciales
            cfg.setMaximumPoolSize(10);
            cfg.setPoolName("HikariEmployeesPool");

            dataSource = new HikariDataSource(cfg);
        }
        return dataSource;
    }

    /**
     * Recupera las credenciales de AWS Secrets Manager.
     *
     * @return mapa que contiene "DB_URL", "DB_USER" y "DB_PASS"
     * @throws IllegalStateException si la recuperación o el parseo fallan
     */
    private static Map<String, String> fetchSecretFromAWS() {
        String secretName = System.getenv()
                .getOrDefault("AWS_SECRET_NAME", "employees/db/credentials");
        Region region = Region.of(
                System.getenv().getOrDefault("AWS_REGION", "mx-central-1")
        );

        Map<String,String> creds = new HashMap<>();

        // 1. Crea el cliente de Secrets Manager
        try (SecretsManagerClient client = SecretsManagerClient.builder()
                .region(region)
                .build()) {

            // 2. Prepara y ejecuta la petición
            GetSecretValueRequest getReq = GetSecretValueRequest.builder()
                    .secretId(secretName)
                    .build();
            GetSecretValueResponse getRes = client.getSecretValue(getReq);

            // 3. Obtén el String JSON crudo
            String secretJson = getRes.secretString();
            LOG.debug("Secret JSON obtenido: {}", secretJson);

            // 4. Parsealo y mapea a tus claves
            Map<String,String> raw = new ObjectMapper().readValue(
                    secretJson,
                    new TypeReference<Map<String,String>>() {}
            );

            creds.put("DB_URL",  raw.getOrDefault("DB_URL",
                    raw.getOrDefault("host", "") + ":" +
                            raw.getOrDefault("port", "3306") + "/" +
                            raw.getOrDefault("dbname", "employees")));
            creds.put("DB_USER", raw.getOrDefault("DB_USER",
                    raw.getOrDefault("username", raw.get("user"))));
            creds.put("DB_PASS", raw.getOrDefault("DB_PASS",
                    raw.getOrDefault("password", raw.get("pass"))));

            return creds;

        } catch (Exception e) {
            LOG.error("Error recuperando secret de AWS", e);
            throw new IllegalStateException("No se pudieron cargar credenciales DB", e);
        }
    }



    /**
     * Verifica la conectividad a la base de datos abriendo una conexión puntual
     * con {@link DriverManager}, sin tocar el pool de HikariCP.
     *
     * @throws SQLException si la conexión falla
     */
    public static void verifySingleConnection() throws SQLException {
        // Resolve credentials same as getDataSource()
        boolean forceAws = Boolean.parseBoolean(
                System.getenv().getOrDefault("USE_AWS_SECRETS",
                        System.getProperty("USE_AWS_SECRETS", "false")
                ).trim()
        );

        String urlEnv  = System.getenv("DB_URL");
        String userEnv = System.getenv("DB_USER");
        String passEnv = System.getenv("DB_PASS");

        // Aquí leemos props si no están en env
        String url  = (urlEnv  != null && !urlEnv.isBlank())
                ? urlEnv
                : System.getProperty("DB_URL");
        String user = (userEnv != null && !userEnv.isBlank())
                ? userEnv
                : System.getProperty("DB_USER");
        String pass = (passEnv != null && !passEnv.isBlank())
                ? passEnv
                : System.getProperty("DB_PASS");

        if (forceAws || url == null || url.isBlank()) {
            Map<String,String> secret = fetchSecretFromAWS();
            url  = secret.get("DB_URL");
            user = secret.get("DB_USER");
            pass = secret.get("DB_PASS");
        }

        LOG.info("✔ Verificando conexión puntual a {}", url);
        try (Connection con = DriverManager.getConnection(url, user, pass)) {
            LOG.info("✔ Prueba puntual exitosa: conectado a {}", con.getMetaData().getURL());
        }
    }

    /**
     * Utility main para validar la recuperación de credenciales y la conectividad.
     *
     * @param args no usados
     */
    public static void main(String[] args) {
        // 1) Verificar recuperación de secret
        try {
            LOG.info("Iniciando prueba de recuperación de credenciales...");
            Map<String, String> creds = fetchSecretFromAWS();
            LOG.info(
                    "Credenciales obtenidas → DB_URL='{}', DB_USER='{}'",
                    creds.get("DB_URL"), creds.get("DB_USER")
            );
        } catch (Exception e) {
            LOG.error("Error al recuperar credenciales de AWS", e);
        }

        // 2) Verificar conexión puntual
        try {
            verifySingleConnection();
        } catch (SQLException e) {
            LOG.error("Error en prueba puntual de conexión", e);
        }
    }
}
