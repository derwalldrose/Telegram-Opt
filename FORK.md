# Telegram-Opt

Fork of official `DrKLO/Telegram` 12.10.6 (7112). Not Nnngram.

Package: `xyz.derwalld.telegramopt` (installs beside official Telegram).
Name: Telegram Opt. Test-signed with the upstream sample keystore (`android` / `androidkey`).

## Taken from Nnngram (client-side only)

- Faster download: 512 KB chunks, 12 parallel requests (official is 128/4 or 512/8).
- Faster upload chunks: min 512 KB instead of 128 KB. Does **not** unlock Premium 4 GB.
- Hide channel/bot sponsored messages (no `getSponsoredMessages`).
- Hide search sponsored peers.
- Hide proxy promo/sponsor dialog (`help.getPromoData`).
- Login: own `api_id`/`api_hash` from my.telegram.org (not official test id 4, not Nnngram); empty SafetyNet so login codes go to an already-open official session. QR login on the phone page (scan with official Telegram → Devices → Link Desktop Device).
- Launcher: two-tone Telegram-blue geometric **T** on white (Nnngram N-style letter mark), not the paper plane.

## Intentionally not ported

Nnngram channels, update bots, custom title, LLM translate, WS proxy, settings dump, branding.

WEB Proxy is already in official source.

Server `FLOOD_WAIT`, 2 GB vs 4 GB, and Premium still apply.
