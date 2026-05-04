Feature: HU-1.1.2 Validación de Identidad
  Como administrador del sistema bancario
  Quiero validar la identidad de los clientes registrados
  Para garantizar la seguridad y cumplir con regulaciones

  @requires_admin
  Scenario: Listado de clientes pendientes de validación
    Given existen los siguientes clientes en estado "PENDING":
      | nombre       | documento  | email              |
      | Juan Perez   | 123456789  | juan@bank.com      |
      | Maria Lopez  | 987654321  | maria@bank.com     |
      | Carlos Diaz  | 456123789  | carlos@bank.com    |
    When el administrador consulta la lista de clientes pendientes
    Then el sistema retorna 3 clientes
    And todos los clientes tienen estado "PENDING"

  @requires_admin
  Scenario: Aprobación de cliente pendiente
    Given un cliente en estado "PENDING" con documento "123456789"
    When el administrador aprueba al cliente con documento "123456789"
    Then el estado del cliente cambia a "ACTIVE"
    And se elimina cualquier comentario de rechazo
    And se envía un correo de aprobación al cliente

  @requires_admin
  Scenario: Rechazo de cliente pendiente con comentario
    Given un cliente en estado "PENDING" con documento "987654321"
    When el administrador rechaza al cliente con documento "987654321" y comentario "Documento borroso"
    Then el estado del cliente cambia a "REJECTED"
    And se registra el comentario de rechazo "Documento borroso"
    And se envía un correo de rechazo al cliente

  @requires_admin
  Scenario: Intento de rechazo sin comentario obligatorio
    Given un cliente en estado "PENDING" con documento "456123789"
    When el administrador intenta rechazar al cliente con documento "456123789" sin comentario
    Then el sistema rechaza la operación con mensaje "El comentario es obligatorio para rechazar"
    And el estado del cliente permanece como "PENDING"
