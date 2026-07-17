# 📱 Calculadora de Precios

[![Build and Release APK](https://github.com/LimitlessCode/CalculadoraPrecios/actions/workflows/android.yml/badge.svg)](https://github.com/LimitlessCode/CalculadoraPrecios/actions)
[![Kotlin Version](https://img.shields.io/badge/Kotlin-1.9.x-blue.svg?style=flat&logo=kotlin)](https://kotlinlang.org/)
[![Android SDK](https://img.shields.io/badge/Android-24%2B-green.svg?style=flat&logo=android)](https://developer.android.com/about/dashboards)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

**Calculadora de Precios** es una solución robusta y moderna para la gestión de inventarios y el cálculo dinámico de precios. Diseñada específicamente para optimizar el flujo de trabajo de emprendedores y pequeños negocios que manejan múltiples divisas y catálogos visuales.

---

## ✨ Características Principales

### 📦 Gestión de Inventario (CRUD)
- Registro completo de productos incluyendo: Nombre, Marca, Modelo, Tipo, Precio en USD, Garantía, Colores e Información Adicional.
- Almacenamiento local seguro mediante **Room Database**.
- Soporte para imágenes de productos con persistencia de permisos.

### 💰 Cálculo Inteligente de Precios
- Conversión automática de **USD a CUP** (u otras divisas) basada en una tasa de cambio configurable.
- Calculadora rápida integrada para obtener el valor en USD a partir de un precio en moneda local.
- Manejo de precisión decimal para cálculos financieros exactos.

### 📤 Opciones Avanzadas de Compartir
- **Individual:** Envía la ficha técnica de un producto con su imagen y precio calculado.
- **Catálogo de Texto:** Genera un resumen textual de múltiples productos seleccionados.
- **Catálogo Visual (Lote):** Crea imágenes compuestas que incluyen la foto del producto superpuesta con su información clave.
- **Catálogo PDF:** Genera documentos PDF profesionales listos para enviar a clientes.
- **Integración con WhatsApp:** Envío directo mediante un número preconfigurado.

### 🛠️ Herramientas y Configuración
- **Filtros y Ordenamiento:** Localiza productos rápidamente por cualquier campo (equipo, marca, precio, etc.).
- **Backup y Restauración:** Exporta e importa toda tu base de datos en formato ZIP para nunca perder tu información.
- **Modo de Selección:** Realiza acciones en lote como ocultar productos o compartirlos masivamente.

---

## 🛠️ Stack Tecnológico

La aplicación sigue las mejores prácticas de desarrollo Android moderno:

- **Lenguaje:** [Kotlin](https://kotlinlang.org/)
- **Arquitectura:** MVVM (Model-View-ViewModel) con Clean Architecture.
- **UI:** Híbrido Material Design 3 (XML View Binding) y [Jetpack Compose](https://developer.android.com/jetpack/compose).
- **Base de Datos:** [Room](https://developer.android.com/training/data-storage/room) para persistencia local.
- **Inyección de Dependencias/Lifecycle:** ViewModel, LiveData y Flow.
- **Carga de Imágenes:** [Coil](https://coil-kt.github.io/coil/) para una gestión eficiente de recursos visuales.
- **Navegación:** Jetpack Navigation Component.
- **Almacenamiento de Preferencias:** DataStore Preferences.
- **Concurrencia:** Kotlin Coroutines.

---

## 🚀 Configuración y Desarrollo

### Requisitos Previos
- Android Studio Ladybug (2024.2.1) o superior.
- JDK 17.
- Dispositivo Android con API 24 (Nougat) o superior.

### Instalación
1. Clona el repositorio:
   ```bash
   git clone https://github.com/LimitlessCode/CalculadoraPrecios.git
   ```
2. Abre el proyecto en Android Studio.
3. Configura tu archivo `keystore.properties` (opcional para desarrollo, ver `keystore.properties.example`).
4. Sincroniza Gradle y ejecuta la aplicación.

---

## 🤖 CI/CD e Integración Continua

El proyecto utiliza **GitHub Actions** para automatizar el ciclo de vida del software:
- **Compilación Automática:** Cada `push` o `pull request` activa una verificación de compilación.
- **Generación de Releases:** Al crear un `tag` (ej. `v1.2.0`), el sistema genera automáticamente el APK firmado y lo adjunta a un nuevo Release en GitHub.

---

## 📂 Estructura del Proyecto

```text
app/src/main/java/cu/limitlesscode/calculadoradeprecios/
├── data/               # Modelos, DAO, Base de Datos y BackupManager
├── ui/                 # Fragments, Adapters y Temas
│   └── theme/          # Configuración de Material 3 y Compose Theme
├── MainActivity.kt     # Punto de entrada y gestión de navegación
├── MainViewModel.kt    # Lógica de negocio y estado de la UI
└── ProductFormatting.kt # Utilidades para formateo de texto y PDF
```

---

## 📄 Licencia

Este proyecto está bajo la Licencia MIT. Consulta el archivo [LICENSE](LICENSE) para más detalles.

---

Developed with ❤️ by **Limitless Code**.
