# Security Policy

## Current security posture

The foundation release binds to `127.0.0.1` by default. It is intended for local testing only. Authentication and hardened external access are not yet implemented.

Do not expose port `8787` directly to the public internet.

## Reporting a vulnerability

Please report security issues privately to `business@aiwithenoch.com`. Do not open a public issue containing exploit details, personal data, credentials, or private model files.

Include:

- Affected version or commit
- Reproduction steps
- Expected impact
- Suggested mitigation when available

## Secrets

Never commit API keys, passwords, cookies, private model URLs, signing keys, or user databases.
