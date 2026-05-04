#!/bin/bash
# Script para ejecutar tests de Cucumber de DigitalBank-Backend

echo "🥒 Ejecutando Tests de Cucumber para DigitalBank-Backend"
echo "=========================================="

# Navegar al directorio del proyecto
cd "$(dirname "$0")"

# Ejecutar tests de Cucumber
echo "📊 Ejecutando todos los tests de Cucumber..."
./mvnw clean test -DskipTests=false 2>&1 | tee cucumber-output.log

# Verificar el código de salida
if [ $? -eq 0 ]; then
    echo ""
    echo "✅ Tests de Cucumber completados exitosamente"
    echo "📋 Reporte HTML disponible en: target/cucumber-report.html"
    echo "📋 Reporte JSON disponible en: target/cucumber-report.json"
else
    echo ""
    echo "❌ Hubo errores en la ejecución de los tests"
    echo "📋 Revisa el log: cucumber-output.log"
fi

# Abrir el reporte automáticamente
if [ -f "target/cucumber-report.html" ]; then
    echo "🌐 Abriendo reporte HTML..."
    if command -v xdg-open > /dev/null; then
        xdg-open target/cucumber-report.html
    elif command -v open > /dev/null; then
        open target/cucumber-report.html
    else
        echo "📁 Abre manualmente: target/cucumber-report.html"
    fi
fi

echo ""
echo "=========================================="
echo "🎯 Proceso finalizado"
