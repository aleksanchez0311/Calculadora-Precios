# Ejemplo de secretos para GitHub Actions

Los secretos que usa el workflow para firma automática son:

- `KEYSTORE_BASE64` — contenido del keystore codificado en Base64 (una sola línea sin saltos)
- `KEYSTORE_PASSWORD` — contraseña del keystore
- `KEY_ALIAS` — alias de la clave dentro del keystore
- `KEY_PASSWORD` — contraseña de la clave (si es diferente)

Ejemplos con la CLI `gh` (GitHub CLI) para añadir secretos a un repositorio público/privado:

```bash
# Crear secreto KEYSTORE_BASE64 desde un archivo
gh secret set KEYSTORE_BASE64 --body "$(cat keystore.base64)"

# Crear los demás secretos
gh secret set KEYSTORE_PASSWORD --body "mi_keystore_password"
gh secret set KEY_ALIAS --body "mi_alias"
gh secret set KEY_PASSWORD --body "mi_key_password"
```

Si prefieres la interfaz web: Repo → Settings → Secrets & variables → Actions → New repository secret.

Seguridad: no almacenes el keystore sin cifrar en el repositorio ni lo envíes por email.
