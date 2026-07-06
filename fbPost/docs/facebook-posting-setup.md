# Facebook Posting Setup (fbem)

This project uses [fbem](https://github.com/crisng95/fbem) to post Reels and Photos to Facebook via the native web API — avoiding Graph API reach suppression. It exposes 6 MCP tools any agent can call.

## Architecture

```text
AI Agent → fbem-mcp (stdio) → fbem-bridge (HTTP :47102) → Chrome Extension (WS :9224) → facebook.com
```

- **fbem-mcp:** Stateless MCP server spawned per-agent. Installed at `~/fbem/.venv/bin/fbem-mcp`.
- **fbem-bridge:** Persistent FastAPI server. Must stay running during use.
- **Chrome Extension:** MV3 extension that injects into facebook.com, scrapes tokens, and replays native uploads.
- **Capture-then-replay:** Post manually once to seed templates; subsequent posts replay automatically with fresh media + tokens.

## Quick Start

### 1. Start the bridge (keep it running)

```sh
~/fbem/.venv/bin/fbem-bridge
# HTTP on :47102, WebSocket on :9224 — loopback only
```

Verify:
```sh
curl -s http://127.0.0.1:47102/api/health | python3 -m json.tool
# Look for: extension_connected: false (until extension loads)
```

### 2. Load the Chrome extension

1. Open `chrome://extensions`
2. Enable **Developer mode** (top right)
3. Click **Load unpacked** → select `~/fbem/extension/`
4. Keep a logged-in `www.facebook.com` tab open

Health check should now show `extension_connected: true`.

### 3. Seed templates (do once per post type)

With the bridge running and extension loaded:

1. **For Reels:** Manually post ONE Reel on facebook.com
2. **For Photos:** Manually post ONE photo (and/or album) on facebook.com
3. **For Pages:** Switch to a page once to capture the profile switch mutation

Verify capture status:
```sh
~/fbem/.venv/bin/python3 -c "
from fbem.mcp.bridge_api import health
import asyncio
h = asyncio.run(health())
print(f'reel_ready={h[\"has_template\"]} photo_ready={h[\"has_photo_template\"]}')
"
```
Or ask an agent: "check fbem capture_status"

### 4. The MCP is wired

`.mcp.json` in this project maps `fbem` to `~/fbem/.venv/bin/fbem-mcp`. Agents auto-discover the 6 tools.

## Available MCP Tools

| Tool | What it does |
|------|-------------|
| `post_reel` | Publish a Facebook Reel from a local .mp4 file |
| `post_photos` | Publish a photo (1 file) or album (N files) |
| `switch_profile` | Switch which page/profile posts go out as |
| `get_identity` | Read current posting identity (read-only) |
| `health` | Bridge + extension status, template readiness, tab TTL |
| `capture_status` | What's captured, what's ready, what needs (re)snapshot |

### Tool signatures

**post_reel**
```
Args: video_path (str) — absolute path to .mp4
      caption (str) — full caption including hashtags
      page_id (str, optional) — Facebook page ID to post as
      scheduled_publish_time (int, optional) — epoch SECONDS to schedule
Returns: { ok, videoId, permalinkUrl }
```

**post_photos**
```
Args: image_paths (list[str]) — absolute paths to .jpg/.png
      caption (str)
      page_id (str, optional)
      scheduled_publish_time (int, optional)
Returns: { ok, postId, photoIds, permalinkUrl }
```

## Configuration (optional)

All config defaults are sensible. Override via env vars if needed:

| Env | Default | Purpose |
|-----|---------|---------|
| `FBEM_HTTP_PORT` | `47102` | Bridge HTTP port |
| `FBEM_WS_PORT` | `9224` | Extension WebSocket port |
| `FBEM_BRIDGE_URL` | `http://127.0.0.1:47102` | Where MCP reaches the bridge |
| `FBEM_HOME` | `~/.fbem` | State dir |
| `FBEM_CAPTURES_DIR` | `~/.fbem/captures` | Template storage (contains live tokens) |
| `FBEM_MEDIA_DIR` | `~/.fbem/media` | Media staging dir |
| `FBEM_TAB_TTL_S` | `7200` | Tab auto-reload window (seconds) |

## Troubleshooting

### extension_not_connected
- Chrome extension not loaded? Check `chrome://extensions`
- facebook.com tab closed? Keep one open
- Dev mode disabled? Re-enable it
- Restart bridge + reload extension

### no_template_captured
- Never posted manually? Post one Reel/Photo by hand on facebook.com first
- Check capture_status to see what's missing

### Replay fails (502, story_create=null)
- Facebook may have rotated its payload schema
- Just re-capture manually: post by hand again on facebook.com
- No code change needed

### Bridge won't start (port conflict)
- Change ports: `FBEM_HTTP_PORT=47103 FBEM_WS_PORT=9225 fbem-bridge`
- Update `FBEM_BRIDGE_URL=http://127.0.0.1:47103` for the MCP side

## Security

- **Loopback-only.** Bridge and WS bind 127.0.0.1. Never expose to a network.
- **Captures contain live FB tokens.** `~/.fbem/captures/` is the bridge's state dir — never commit it.
- **No tokens in code.** Volatile tokens scraped live from the page at replay time.
- **MIT licensed.** [crisng95/fbem](https://github.com/crisng95/fbem)
