Feature: HU-1.1.3 Autenticación y Control de Acceso
  Como usuario registrado del sistema
  Quiero autenticarme de forma segura
  Para acceder a mis cuentas bancarias

  Scenario: Login exitoso con credenciales correctas
    Given un cliente activo con email "user@bank.com" y contraseña "SecurePass123!"
    When el usuario inicia sesión con email "user@bank.com" y contraseña "SecurePass123!"
    Then el sistema genera un token JWT válido
    And el sistema retorna el estado "ACTIVE"
    And el sistema retorna el rol del usuario
    And se resetean los intentos fallidos a 0
    And se elimina el tiempo de bloqueo si existía

  Scenario: Bloqueo de cuenta después de 3 intentos fallidos
    Given un cliente activo con email "user@bank.com" y contraseña "SecurePass123!"
    When el usuario intenta iniciar sesión con contraseña incorrecta 3 veces
    Then la cuenta se bloquea por 10 minutos
    And el contador de intentos fallidos es 3
    And se registra el tiempo de bloqueo
    And el cuarto intento de login falla con bloqueo

  Scenario: Rechazo de login mientras la cuenta está bloqueada
    Given un cliente con cuenta bloqueada hasta 10 minutos después de los intentos fallidos
    When el usuario intenta iniciar sesión con contraseña correcta
    Then el sistema rechaza el inicio de sesión
    And el mensaje indica "Cuenta bloqueada por intentos fallidos. Intente nuevamente en 10 minutos."

  Scenario: Desbloqueo automático de cuenta después del tiempo límite
    Given un cliente con cuenta bloqueada hace 15 minutos
    When el usuario intenta iniciar sesión con contraseña correcta
    Then el sistema permite el inicio de sesión
    And se resetean los intentos fallidos a 0
    And se elimina el tiempo de bloqueo
    And se genera un token JWT válido
