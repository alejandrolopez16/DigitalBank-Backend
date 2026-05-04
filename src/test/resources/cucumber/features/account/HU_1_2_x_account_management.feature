Feature: HU-1.2.x Gestión de Cuentas Financieras
  Como cliente del banco
  Quiero gestionar mis cuentas financieras
  Para realizar operaciones bancarias

  @requires_authentication
  Scenario: Creación exitosa de cuenta financiera
    Given un cliente autenticado con documento "123456789"
    When el cliente crea una cuenta financiera para su documento
    Then la cuenta se crea con saldo inicial de 0
    And la cuenta pertenece al cliente autenticado
    And el estado de la cuenta es "ACTIVE"

  @requires_authentication
  Scenario: Rechazo de creación de cuenta para otro usuario
    Given un cliente autenticado con documento "123456789"
    When el cliente intenta crear una cuenta financiera para documento "999999999"
    Then el sistema rechaza la operación con mensaje "Solo puedes crear una cuenta financiera para tu propio usuario"

  @requires_authentication
  Scenario: Consulta exitosa de todas mis cuentas financieras
    Given el cliente autenticado tiene 2 cuentas financieras
    When el cliente consulta sus cuentas financieras
    Then el sistema retorna las 2 cuentas del cliente
    And todas las cuentas pertenecen al cliente autenticado

  @requires_authentication
  Scenario: Consulta exitosa de cuenta financiera específica
    Given el cliente autenticado tiene una cuenta con ID "cuenta-id-123"
    When el cliente consulta la cuenta con ID "cuenta-id-123"
    Then el sistema retorna la cuenta financiera
    And la cuenta consultada pertenece al cliente autenticado

  @requires_authentication
  Scenario: Rechazo de consulta de cuenta que no pertenece al cliente
    Given el cliente autenticado intenta consultar una cuenta de otro usuario
    When el cliente consulta la cuenta con ID "cuenta-otro-usuario"
    Then el sistema rechaza la consulta con mensaje "Cuenta financiera no encontrada para el usuario autenticado"

  @requires_authentication
  Scenario: Bloqueo exitoso de cuenta financiera
    Given el cliente autenticado tiene una cuenta con estado "ACTIVE"
    When el cliente bloquea su cuenta financiera
    Then el estado de la cuenta cambia a "BLOCKED"
    And la operación es exitosa

  @requires_authentication
  @Ignored
  # QA NOTE: Escenario marcado como Ignorado porque la mutación 'desbloquearCuenta' no existe en el backend
  # BUG_ID: QA-001 - Pendiente de implementación en el backend
  # IMPACTO: Funcionalidad de desbloqueo de cuentas no disponible para usuarios
  Scenario: Desbloqueo exitoso de cuenta financiera
    Given el cliente autenticado tiene una cuenta con estado "BLOCKED"
    When el cliente desbloquea su cuenta financiera
    Then el estado de la cuenta cambia a "ACTIVE"
    And la operación es exitosa
