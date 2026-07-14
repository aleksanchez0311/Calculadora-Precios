# Secrets (plantillas y guías)

Este directorio contiene plantillas y scripts para crear y subir los secretos necesarios para firmar el APK desde GitHub Actions. NO pongas aquí archivos reales de keystore.

Archivos incluidos:

- `keystore.base64.example` — marcador de posición para el contenido Base64 del keystore.
- `github-secrets-example.md` — ejemplos y comandos para añadir secretos en GitHub.
- `encode_keystore.sh` — script Bash para codificar `release.keystore` a Base64.
- `encode_keystore.ps1` — script PowerShell equivalente.

Usos recomendados:
1. Genera tu `release.keystore` localmente (no lo subas al repo).
2. Codifícalo en Base64 y copia el contenido al secreto `KEYSTORE_BASE64` en GitHub.
3. Añade `KEYSTORE_PASSWORD`, `KEY_ALIAS` y `KEY_PASSWORD` como secretos en GitHub.

Ejemplo de codificación (Linux/macOS):

```bash
base64 release.keystore | tr -d '\n' > keystore.base64
# luego copia el contenido de keystore.base64 al secreto KEYSTORE_BASE64
```

Ejemplo (PowerShell):

```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("release.keystore")) > keystore.base64
```

No olvides mantener tus secretos seguros y usar la UI de GitHub o `gh` para cargarlos.
