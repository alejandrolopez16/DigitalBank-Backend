# Informe de Acceptance Tests - Cucumber BDD
## DigitalBank Backend - Abril 29, 2026

---

## 1. Resumen Ejecutivo

| Métrica | Valor |
|---|---|
| **Total Escenarios** | 27 |
| **Pasan** | 12 (44%) |
| **Fallan** | 15 (56%) |
| **Errores** | 0 |
| **Omitidos** | 0 |
| **Tiempo de Ejecución** | ~110 segundos |
| **Comando** | `./mvnw clean test -Dtest=CucumberRunnerIT` |
| **Reporte** | `target/cucumber-report.html` |

---

## 2. Arquitectura de Testing

| Componente | Tecnología | Detalle |
|---|---|---|
| **Framework** | Cucumber 7.20.1 | Gherkin + Step Definitions Java |
| **Motor** | JUnit Platform 5.11.x | `@Suite` con `CucumberEngine` |
| **Spring** | Spring Boot 4.0.3 (Java 21) | `@SpringBootTest(RANDOM_PORT)` |
| **API** | GraphQL (Spring GraphQL) | Endpoint `/graphql` |
| **Base de Datos** | PostgreSQL (Neon Cloud) | Datos reales, sin mocks ni H2 |
| **HTTP Client** | `RestTemplate` | Peticiones HTTP reales al servidor embebido |
| **Auth** | JWT (BCrypt) | Tokens reales generados por el servicio |
| **Reporte** | Plugin `html:target/cucumber-report.html` | Generado automáticamente por Cucumber |

### Flujo de Ejecución

```
Feature File (Gherkin)
  ↓
Step Definitions (Java + Spring @Autowired)
  ↓
GraphQL Client (RestTemplate → HTTP real al servidor)
  ↓
Spring Boot App completa (Controllers → Services → Repositories)
  ↓
PostgreSQL real (Neon)
  ↓
Respuesta GraphQL → Assertions (JUnit Jupiter)
```

---

## 3. Feature Files Escaneados

| Archivo | Feature | Escenarios |
|---|---|---|
| `HU_1_1_1_customer_registration.feature` | Registro de Cliente | 6 (3 examples + 3 singles) |
| `HU_1_1_2_customer_validation.feature` | Validación de Identidad (Admin) | 4 |
| `HU_1_1_3_customer_authentication.feature` | Autenticación y Control de Acceso | 4 |
| `HU_1_2_x_account_management.feature` | Gestión de Cuentas Financieras | 7 |
| `HU_2_x_transfers.feature` | Transferencias y Políticas de Seguridad | 6 |

### Step Definitions Implementadas

| Clase | Paquete | Rol |
|---|---|---|
| `CustomerRegistrationSteps` | `steps.customer` | Registro y validación de clientes |
| `CustomerValidationSteps` | `steps.customer` | Aprobación/rechazo admin |
| `CustomerAuthenticationSteps` | `steps.customer` | Login, bloqueo, desbloqueo |
| `AccountManagementSteps` | `steps.account` | CRUD cuentas financieras |
| `TransferSteps` | `steps.transaction` | Transferencias y políticas |
| `GraphQLSteps` | `steps.common` | Cliente HTTP compartido para GraphQL |

---

## 4. Resultados Detallados por Scenario

### ✅ PASAN (12)

| # | Scenario | Feature |
|---|---|---|
| 1 | Registro exitoso de cliente mayor de edad | HU-1.1.1 |
| 2 | Validación de edad mínima - Example 1.1 (2010-05-15) | HU-1.1.1 |
| 3 | Validación de edad mínima - Example 1.2 (2015-10-20) | HU-1.1.1 |
| 4 | Validación de duplicado de email | HU-1.1.1 |
| 5 | Login exitoso con credenciales correctas | HU-1.1.3 |
| 6 | Bloqueo de cuenta después de 3 intentos fallidos | HU-1.1.3 |
| 7 | Rechazo de login mientras la cuenta está bloqueada | HU-1.1.3 |
| 8 | Desbloqueo automático de cuenta después del tiempo límite | HU-1.1.3 |
| 9 | Creación exitosa de cuenta financiera | HU-1.2.x |
| 10 | Rechazo de creación de cuenta para otro usuario | HU-1.2.x |
| 11 | Transferencia exitosa entre cuentas activas | HU-2.x |
| 12 | Requerimiento de validación OTP para montos altos | HU-2.x |

### ❌ FALLAN (15)

| # | Scenario | Feature | Error | Causa Raíz |
|---|---|---|---|---|
| 1 | Validación de edad mínima - Example 1.3 (2008-03-01) | HU-1.1.1 | expected: <400> but was: <200> | Backend valida 18 años con fecha exacta; 2008-03-01 tiene ~18 años cumplidos hoy (2026-04-29), no falla |
| 2 | Validación de duplicado de documento | HU-1.1.1 | expected: <400> but was: <200> | El servicio de registro no valida duplicados por documento, o el email diferente permite crearlo |
| 3 | Listado de clientes pendientes de validación | HU-1.1.2 | expected: <200> but was: <400> | Query `clientesPendientes` requiere rol ADMIN. Los tests no autentican como ADMIN |
| 4 | Aprobación de cliente pendiente | HU-1.1.2 | expected: <200> but was: <400> | Mutation `aprobarCliente` requiere `@PreAuthorize("hasRole('ADMIN')")`. No hay auth admin |
| 5 | Rechazo de cliente pendiente con comentario | HU-1.1.2 | expected: <200> but was: <400> | Mutation `rechazarCliente` requiere `@PreAuthorize("hasRole('ADMIN')")`. No hay auth admin |
| 6 | Intento de rechazo sin comentario obligatorio | HU-1.1.2 | expected: <true> but was: <false> | El backend no rechaza con comentario vacío, o el mensaje no contiene el texto esperado |
| 7 | Consulta exitosa de todas mis cuentas financieras | HU-1.2.x | expected: <200> but was: <400> | Query `misCuentasFinancieras` requiere autenticación JWT que no se establece correctamente |
| 8 | Consulta exitosa de cuenta financiera específica | HU-1.2.x | expected: <200> but was: <400> | Query `miCuentaFinanciera` requiere autenticación. El ID "cuenta-id-123" no es UUID válido |
| 9 | Rechazo de consulta de cuenta que no pertenece al cliente | HU-1.2.x | expected: <true> but was: <false> | El mensaje de error no contiene el texto esperado o la estructura de respuesta es diferente |
| 10 | Bloqueo exitoso de cuenta financiera | HU-1.2.x | expected: <200> but was: <400> | Mutation `bloquearCuenta` requiere auth JWT válida |
| 11 | Desbloqueo exitoso de cuenta financiera | HU-1.2.x | expected: <200> but was: <400> | Mutation `desbloquearCuenta` requiere auth JWT válida |
| 12 | Rechazo por fondos insuficientes | HU-2.x | expected: <400> but was: <200> | El servicio no retorna GraphQL errors para fondos insuficientes, retorna data normal con status "REJECTED" |
| 13 | Rechazo cuando cuenta origen está bloqueada | HU-2.x | expected: <200> but was: <400> | Auth token no se transmite correctamente al endpoint de transferencia |
| 14 | Validación de límite diario de transferencias | HU-2.x | expected: <400> but was: <200> | El servicio no valida el límite diario o retorna data normal con status de rechazo |
| 15 | Actualización exitosa de políticas de seguridad | HU-2.x | expected: <200> but was: <400> | Mutation `ActualizarPoliticaSeguridad` requiere `@PreAuthorize("hasRole('ADMIN')")` |

---

## 5. Categorías de Fallas

### Categoría A: Autenticación ADMIN faltante (4 escenarios)
**Feature:** HU-1.1.2 (Validación de Identidad) + HU-2.x (Políticas de Seguridad)

- Los endpoints `clientesPendientes`, `aprobarCliente`, `rechazarCliente`, `ActualizarPoliticaSeguridad` están protegidos con `@PreAuthorize("hasRole('ADMIN')")`.
- Los steps de test solo crean clientes con rol `ROLE_USER` y autentican como usuario normal.
- **Solución requerida:** Crear un usuario ADMIN, otorgar rol `ROLE_ADMIN`, y autenticar con ese usuario antes de ejecutar estos escenarios.

### Categoría B: Respuestas del backend no siguen formato de error GraphQL (4 escenarios)
**Feature:** HU-1.1.1 (Registro) + HU-2.x (Transferencias)

- Los escenarios de `duplicado de documento`, `fondos insuficientes`, `límite diario` y `edad 1.3` esperan que el backend retorne HTTP 400 con GraphQL errors.
- El backend retorna HTTP 200 con data válida pero con status de rechazo en el payload (ej: `status: "REJECTED"`).
- **Solución requerida:** Modificar las assertions para verificar el campo `status` en la data en lugar de esperar un error HTTP, o ajustar el backend para lanzar excepciones en estos casos.

### Categoría C: Autenticación JWT no se transmite correctamente (4 escenarios)
**Feature:** HU-1.2.x (Gestión de Cuentas) + HU-2.x (Transferencias)

- Los endpoints `misCuentasFinancieras`, `miCuentaFinanciera`, `bloquearCuenta`, `desbloquearCuenta` y `ejecutarTransferencia` requieren un token JWT válido.
- El token obtenido del login puede no estar siendo incluido correctamente en los headers HTTP de las peticiones siguientes.
- **Solución requerida:** Verificar que `setAuthenticationHeader` se aplica correctamente y que el header `Authorization: Bearer {token}` llega al controller.

### Categoría D: Mensaje de error no coincide (2 escenarios)
**Feature:** HU-1.1.2 + HU-1.2.x

- El texto esperado en los assertions (`expected: <true> but was: <false>`) no coincide con el mensaje real que retorna el backend.
- **Solución requerida:** Ajustar el texto esperado en el feature file o en la assertion para que coincida con el mensaje real del servicio.

---

## 6. Problemas Resueltos en esta Sesión

| # | Problema | Solución |
|---|---|---|
| 1 | `documentType` null en Customer (constraint violation) | Agregado `.documentType("CC")` en todos los builders de Customer |
| 2 | Contraseñas sin BCrypt (login fallaba) | Inyectado `PasswordEncoder` y usado `passwordEncoder.encode()` en steps que crean usuarios |
| 3 | GraphQLSteps usaba HttpClient hardcodeado puerto 8080 | Cambiado a `RestTemplate` + `Environment.getProperty("local.server.port")` |
| 4 | Login mutation usaba `password` en vez de `passwordHash` | Corregido a `passwordHash` en `buildAuthenticationToken` |
| 5 | ClassCast en respuestas de error | Reescrito `executeQuery` con manejo correcto de tipos genéricos |
| 6 | Steps undefined (13+ pasos faltantes) | Creado `CustomerValidationSteps.java` + agregados steps faltantes en `CustomerAuthenticationSteps` y `TransferSteps` |
| 7 | Step duplicado `elSistemaRechazaLaOperacionConMensaje` | Compartido a través de `GraphQLSteps.getLastErrorResponse()` |
| 8 | BigDecimal.ZERO vs 0.0 | Cambiado a `BigDecimal.compareTo()` para comparación correcta |
| 9 | IndexOutOfBounds en transferencia | Agregado check `financialAccountRepository.count() < 2` con fallback |
| 10 | `@Value("${local.server.port}")` no resolvía | Reemplazado por `Environment.getProperty()` |
| 11 | Script no abría reporte automáticamente | Modificado `run-cucumber-tests.sh` para abrir sin preguntar |

---

## 7. Cómo Ejecutar

```bash
# Ejecución completa (abre reporte automáticamente)
./run-cucumber-tests.sh

# Ejecución directa con Maven
./mvnw clean test -Dtest=CucumberRunnerIT
```

---

## 8. Limitaciones Conocidas

1. **No hay isolation entre escenarios:** Cada escenario escribe en la misma DB de Neon. Datos residuales de un escenario pueden afectar al siguiente.
2. **No hay cleanup:** No existe `@Transactional` con rollback ni hooks `@After` que limpien datos.
3. **Sin mocks:** Todo es integración real (DB + servicios + HTTP).
4. **Orden de ejecución:** Cucumber no garantiza orden. Los escenarios que dependen de datos creados por otros pueden fallar si se ejecutan en otro orden.
5. **Fechas dinámicas:** La validación de edad usa `LocalDate.now()`, lo que hace que el Example 1.3 (2008-03-01) sea borderline dependiendo de la fecha exacta de ejecución.

---

## 9. Próximos Pasos Recomendados

1. **Implementar auth ADMIN:** Crear steps que configuren un usuario con rol `ROLE_ADMIN` y autentiquen antes de los escenarios `@requires_admin`.
2. **Agregar hooks de limpieza:** `@After` en los steps para limpiar datos de la DB entre escenarios.
3. **Corregir assertions del backend:** Los tests que esperan HTTP 400 deben verificar el campo `status` en la data cuando el backend retorna 200 con status de rechazo.
4. **Validar header Authorization:** Debuggear por qué el JWT no llega al controller en los escenarios de cuentas financieras y transferencias.
5. **Separar tests unitarios de acceptance:** Mantener los tests de servicio con mocks (`*ServiceTest.java`) separados de estos tests de aceptación.
