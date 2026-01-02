# Security Rules

## Sensitive Files - NEVER Read or Commit
- `.env` and `.env.*` files
- `local.properties`
- `google-services.json`
- `*.keystore` and `*.jks` files
- `credentials.json`
- Any file in `secrets/` directory

## Code Security
- Never hardcode API keys or secrets
- Use BuildConfig for environment-specific values
- Validate all user inputs
- Use EncryptedSharedPreferences for sensitive data
- Always use HTTPS for network calls

## Database Security
- Use parameterized queries (Room handles this)
- Never log sensitive data
- Encrypt sensitive columns if needed

## Network Security
- Implement certificate pinning for critical endpoints
- Validate SSL certificates
- Don't trust user-provided URLs

## ProGuard/R8
- Enable for release builds
- Keep rules minimal and specific
- Test release builds thoroughly
