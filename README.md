# MTProto Proxy Mod for Fabric 1.21.4

This mod allows Minecraft to connect through an MTProto proxy (Telegram MTProto) with Obfuscated2 obfuscation and AES-256-IGE encryption.

## Features
- Intercepts Minecraft client network connections
- Tunnels all traffic through a configured MTProto proxy
- Implements AES-256-IGE (via Bouncy Castle)
- Obfuscated2 handshake for MTProto compatibility
- Configurable via JSON

## Installation
1. Install Fabric Loader for Minecraft 1.21.4.
2. Place the mod JAR into your `mods` folder.
3. Configure the proxy settings in `config/mtproto-proxy-mod.json`.

## Configuration
Edit `config/mtproto-proxy-mod.json`:
```json
{
  "enabled": true,
  "proxyHost": "your.mtproto.proxy.com",
  "proxyPort": 443,
  "secret": "hex:YOUR_SECRET_HEX"
}
```

- `secret`: can be plaintext or hex-encoded with `hex:` prefix.

## Building

```
./gradlew build
```

The mod JAR will be in `build/libs/`.

## Limitations

- This is a proof-of-concept. Full integration with Minecraft's packet system is not complete.
- Handshake implementation may not be fully compatible with all MTProto proxies.
- The mod currently lacks a GUI and advanced error recovery.

## Dependencies

- Fabric API
- Bouncy Castle (bundled)

## License

MIT