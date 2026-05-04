Feature: HU-1.1.1 Registro de Cliente
  Como usuario del sistema bancario
  Quiero registrarme como cliente del banco
  Para poder acceder a los servicios financieros

  Scenario: Registro exitoso de cliente mayor de edad
    Given un usuario con los siguientes datos:
      | nombre        | Ana Maria Gomez Rodriguez |
      | documento     | 123456789              |
      | tipo_doc      | CC                       |
      | email         | ana@bank.com            |
      | fecha_nac     | 1990-05-15               |
      | telefono      | 3001234567               |
      | direccion     | Calle 10 # 20-30         |
      | password      | SecurePass123!           |
    When el usuario se registra en el sistema
    Then el sistema crea el registro con estado "PENDING"
    And la contraseña es encriptada
    And el usuario recibe un rol "ROLE_USER"

  Scenario Outline: Validación de edad mínima para registro
    Given un usuario con fecha de nacimiento "<fecha_nacimiento>"
    When intenta registrarse en el sistema
    Then el sistema rechaza el registro con mensaje "Error,el cliente debe ser mayor de 18 años para registrarse"

    Examples:
      | fecha_nacimiento |
      | 2010-05-15       |
      | 2015-10-20       |
      | 2008-03-01       |

  Scenario: Validación de duplicado de email
    Given existe un cliente registrado con email "existing@bank.com"
    When un usuario nuevo intenta registrarse con el mismo email
    Then el sistema rechaza el registro con mensaje "Error, el correo electrónico ya está registrado"

  Scenario: Validación de duplicado de documento
    Given existe un cliente registrado con documento "123456789"
    When un usuario nuevo intenta registrarse con el mismo documento
    Then el sistema rechaza el registro con mensaje "Error, el número de documento ya está registrado"
