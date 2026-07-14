# Calculadora de Precios

Aplicación Android "Calculadora de Precios" (Jetpack Compose, MVVM, Room, DataStore).

## Objetivo
Aplicación para gestionar productos con precios en USD y calcular su equivalente en CUP usando una tasa de cambio persistente. Incluye CI con GitHub Actions para compilar el APK (Release) y opción de firma automática.

## Estructura importante
- `app/` — Módulo Android
- `.github/workflows/compilar_apk.yml` — Workflow para compilar y publicar el APK

## Requisitos locales
- JDK 17
- Android SDK (platform-tools, platforms;android-34, build-tools;34.0.0)
- Gradle (idealmente usar el Gradle wrapper incluido)

## Pasos recomendados para preparar el repositorio
1. Generar y commitear el Gradle Wrapper (recomendado):

```bash
# desde la raíz del proyecto
./gradlew wrapper --gradle-version 8.5.1
git add gradle/wrapper gradlew gradlew.bat
git commit -m "Add Gradle wrapper"
```

2. Confirmar que el archivo `app/build.gradle.kts` y `settings.gradle.kts` están presentes y correctos.

## Uso del workflow de GitHub Actions
El workflow `Compilar APK Release de Android` se lanza en:
- `push` a `main` o `master`
- `push` de tags que empiecen con `v` (por ejemplo `v1.0.0`)
- ejecución manual (`workflow_dispatch`)

Al ejecutarse compila `:app:assembleRelease` y sube el APK como artifact llamado `app-release-apk`.

### Firma automática (opcional)
Si deseas que el APK se firme automáticamente en CI, añade los siguientes secretos en el repositorio (Settings → Secrets & variables → Actions):

- `KEYSTORE_BASE64` — contenido del keystore codificado en Base64
- `KEYSTORE_PASSWORD` — contraseña del keystore
- `KEY_ALIAS` — alias de la clave
- `KEY_PASSWORD` — contraseña de la clave (si es diferente)

Cómo generar `KEYSTORE_BASE64` desde tu máquina local:

```bash
# ejemplo: convertir release.keystore a base64
base64 release.keystore | tr -d '\n' > keystore.base64
# copia el contenido de keystore.base64 al secreto KEYSTORE_BASE64
```

En el workflow, si `KEYSTORE_BASE64` existe, se decodifica a `release.keystore` y Gradle usará las propiedades proporcionadas para firmar.

## Ejecutar build localmente
Con Gradle wrapper (recomendado):

```bash
./gradlew :app:assembleRelease
# o para debug
./gradlew :app:assembleDebug
```

Si no tienes wrapper y usas `gradle` instalado globalmente:

```bash
gradle :app:assembleRelease
```

## Artefactos y descarga
Al finalizar el workflow, descarga el artifact `app-release-apk` desde la sección de ejecuciones del workflow (Actions → run → Artifacts).

## Notas de seguridad
- No añadas el keystore a Git; almacénalo solo como secreto en GitHub.
- `release.keystore` si se crea en CI no se comiteará (ver `.gitignore`).

## Próximos pasos sugeridos
- Commit del Gradle wrapper y push al repo
- Añadir los secretos en GitHub si quieres builds firmados

Si quieres, genero los comandos exactos para crear el wrapper en Windows o Linux y los pasos para configurar los secretos en GitHub GUI.
