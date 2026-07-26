# Sankofa Agent Gateway

This is the smallest cloud component in Sankofa. The Android app remains local-first; the gateway exists only to keep the Composio project API key out of the APK and to enforce server-side tool policy.

## Security model

- The Composio API key is stored as a Worker secret.
- The app authenticates to the gateway with a separate bearer token.
- Only configured tool prefixes are accepted.
- Read-only tools may run immediately.
- Drafts and external writes require an explicit one-time approval from the app.
- Destructive tools are blocked by default at the gateway.
- Tool arguments are not written to the default audit log.

The shared gateway bearer token is suitable for a private alpha or single-user deployment. A public multi-user release must replace it with per-user authentication and short-lived signed tokens.

## Deploy

```bash
cd gateway
npm install
npm run check

npx wrangler secret put COMPOSIO_API_KEY
npx wrangler secret put SANKOFA_GATEWAY_TOKEN
npm run deploy
```

Set a long random gateway token, for example:

```bash
openssl rand -hex 32
```

Do not commit either secret. Do not place the Composio key in Android resources, `BuildConfig`, or the APK.

## Environment variables

| Name | Purpose |
|---|---|
| `COMPOSIO_API_KEY` | Secret project API key used only by the Worker |
| `SANKOFA_GATEWAY_TOKEN` | Secret bearer token accepted from the Android alpha app |
| `COMPOSIO_BASE_URL` | Defaults to `https://backend.composio.dev` |
| `ALLOWED_ORIGIN` | Browser origin allowed by CORS |
| `ALLOWED_TOOL_PREFIXES` | Comma-separated Composio tool prefixes |

## Routes

- `GET /health`
- `POST /v1/connect`
- `POST /v1/tools/execute/:toolSlug`

### Connect account

```json
{
  "userId": "local-user-123",
  "authConfigId": "ac_example"
}
```

The gateway creates a Composio Connect Link and returns its `redirect_url`.

### Execute tool

```json
{
  "userId": "local-user-123",
  "toolSlug": "GMAIL_GET_PROFILE",
  "version": "latest",
  "arguments": {},
  "approved": false
}
```

External-write tools return HTTP `409` until the app resubmits with `approved: true`. Destructive tools return HTTP `403` even when approved.

## Production work still required

- Per-user authentication instead of one shared bearer token
- Rate limiting per user and device
- Durable audit storage with privacy controls
- App attestation and abuse protection
- Pinned Composio toolkit versions instead of `latest`
- Formal allowlists for exact tool slugs, not only toolkit prefixes
