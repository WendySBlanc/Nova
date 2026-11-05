# Nova MangaTranslator Server

This private companion translates complete image chapters on your Windows PC. Nova sends an ordered ZIP, follows a persistent job, validates the complete result, and keeps the original chapter unchanged.

The service binds only to `127.0.0.1`. Tailscale Serve gives your Android device a private HTTPS address without exposing port 8765 to the public Internet.

## Requirements

- Windows 10 or 11
- Python 3.11, 3.12, or 3.13
- [MangaTranslator](https://github.com/meangrinch/MangaTranslator) configured and working on the PC
- Tailscale signed in to the same tailnet on the PC and Android device

MangaTranslator's target language, OCR, cleaning, inpainting, rendering, and optional provider credentials stay in its own `config.json`. Do not enter provider API keys in Nova.

## Install

Clone MangaTranslator and complete its upstream installation first. Confirm that it can translate an image with the configuration you intend to use.

From this directory, create one Python environment containing both MangaTranslator's dependencies and the Nova server:

```powershell
py -3.12 -m venv .venv
& .\.venv\Scripts\python.exe -m pip install --upgrade pip
& .\.venv\Scripts\python.exe -m pip install -r C:\path\to\MangaTranslator\requirements.txt
& .\.venv\Scripts\python.exe -m pip install -e .
```

If MangaTranslator documents a different CUDA or PyTorch installation for your GPU, use its command instead of replacing it with a generic PyTorch build.

## Start

Generate a private token once and keep it in your password manager:

```powershell
$bytes = New-Object byte[] 32
[System.Security.Cryptography.RandomNumberGenerator]::Fill($bytes)
$env:NOVA_TRANSLATOR_TOKEN = [Convert]::ToHexString($bytes).ToLowerInvariant()
```

Start the service. `DataDirectory` contains persistent job metadata and translated result archives, so keep it between restarts.

```powershell
.\start-nova-server.ps1 `
  -MangaTranslatorRoot "C:\path\to\MangaTranslator" `
  -ConfigPath "C:\path\to\MangaTranslator\config.json" `
  -DataDirectory "D:\NovaMangaTranslatorData" `
  -Token $env:NOVA_TRANSLATOR_TOKEN
```

Add `-ForceCpu` only when GPU execution is unavailable. Leave this PowerShell window open while translating new chapters. Cached translations in Nova remain readable when the PC is offline.

## Private Tailscale access

In a second PowerShell window, expose the loopback service to your tailnet:

```powershell
tailscale serve --bg 8765
tailscale serve status
```

Tailscale prints a private HTTPS URL such as `https://your-pc.your-tailnet.ts.net`. On Android, open Nova's **Settings > Reader > Chapter translation**, save that URL and the same token, then choose **Test connection**.

To remove the private route later:

```powershell
tailscale serve reset
```

## Windows firewall

Do not create an inbound firewall rule for port 8765 and do not forward the port on your router. The server listens only on loopback; Tailscale's local service handles private access. If Windows asks whether Python should accept public-network connections, deny public access.

## Updating

After updating MangaTranslator, reactivate this environment, refresh its dependencies according to the upstream project, and restart the Nova service. Existing complete translations stay cached. A changed MangaTranslator configuration gets a new configuration revision, preventing a new result from silently reusing incompatible cache metadata.

## Troubleshooting

- **Test connection fails:** verify that the server window is still open, both devices are connected to Tailscale, the HTTPS URL is current, and both tokens match.
- **401 response:** replace the token in Nova with the exact server token.
- **Translation fails after an upstream update:** launch MangaTranslator directly and verify its `config.json` and model installation before restarting this service.
- **Job resumes after restart:** this is expected. Queued and running job records are persisted in `DataDirectory`.

The server never needs source cookies or extension credentials. Its access log is disabled, and Nova's bearer token must not be written to application logs.
