# Telegram-Opt

Fork of official `DrKLO/Telegram` 12.10.6 (7112). Not Nnngram.

Package: `xyz.derwalld.telegramopt` (installs beside official Telegram).
Name: Gram. Test-signed with the upstream sample keystore (`android` / `androidkey`).

## Taken from Nnngram (client-side only)

- Faster download: 512 KB chunks, 12 parallel requests (official is 128/4 or 512/8).
- Faster upload chunks: min 512 KB instead of 128 KB. Does **not** unlock Premium 4 GB.
- Hide channel/bot sponsored messages (no `getSponsoredMessages`).
- Hide search sponsored peers.
- Hide proxy promo/sponsor dialog (`help.getPromoData`).
- Login: own `api_id`/`api_hash` from my.telegram.org (not official test id 4, not Nnngram); empty SafetyNet so login codes go to an already-open official session. QR login on the phone page (scan with official Telegram → Devices → Link Desktop Device).
- Launcher: two-tone Telegram-blue **G** on white (Nnngram N-style letter mark; light body + dark crossbar), not the paper plane. App name is Gram.
- Default high-quality photo send (JPEG 99).
- Auto-switch proxies (`UseProxyRotation`) defaults on so WEB/SOCKS list rotates when Connecting to proxy.
- SOCKS/MTProto auto-off while a system VPN is up; restore only if Gram turned it off. WEB proxy stays on under VPN (loopback + Clash TUN; turning it off stuck Connecting/Updating). Watches NetworkCallback, all VPN networks, and applies before native init.
- Do not send typing; report offline; do not mark stories read.
- Hide stories bar, hide own phone in profile/settings, ignore blocked senders in notifications, reveal spoilers.
- Long-press: no-quote forward and repeat.

## Intentionally not ported

Nnngram channels, update bots, custom title, LLM translate, WS proxy, settings dump, branding.

WEB Proxy is already in official source.

Server `FLOOD_WAIT`, 2 GB vs 4 GB, and Premium still apply.
