# Calculadora Precios

Aplicación de Android desarrollada en Kotlin que permite realizar cálculos de precios y márgenes de forma rápida y eficiente.

## Características
- Cálculo automático de precios.
- Gestión fácil de márgenes de ganancia.
- Interfaz intuitiva y moderna.

## Requisitos
- Android Studio Ladybug (o superior).
- JDK 17 o superior.
- SDK de Android (minSdk configurado en el proyecto).

## Configuración del Proyecto
1. Clona el repositorio.
2. Abre el proyecto en Android Studio.
3. Sincroniza Gradle.
4. Ejecuta la aplicación en un emulador o dispositivo físico.

## Integración Continua (CI/CD)
El proyecto está configurado para compilar y generar la APK automáticamente a través de **GitHub Actions**.
Cada vez que se sube un tag de versión (ej. `v1.0.0`) o se hace un push a la rama `main`, se genera un Release con el APK correspondiente.
