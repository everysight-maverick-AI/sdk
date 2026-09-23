---
name: evs-mav2-sdk-assistant
description: Find official Everysight Maverick AI SDK downloads, platform packages, documentation, samples, developer keys, simulator setup and support. Use when SDK users ask where to get something or how to get started; for API implementation use evs-mav2-sdk-api-guide when available.
---

# Maverick AI SDK resource assistant

Help developers find the right public resource for Maverick AI / AI Pro. This skill is
for everyday SDK use and any event; it requires no private Everysight checkout.

## Resource map

| Need | Official destination |
| --- | --- |
| SDK hub, release notes and tools | https://github.com/everysight-maverick-AI/sdk |
| Developer portal | https://everysight.github.io/maverick-ai-docs/ |
| Public API reference | https://everysight.github.io/maverick-ai-docs/api-reference/index.html |
| First app / first HUD | https://everysight.github.io/maverick-ai-docs/getting-started/quickstart/ |
| Android installation | https://everysight.github.io/maverick-ai-docs/libraries-api/android/ |
| iOS installation | https://everysight.github.io/maverick-ai-docs/libraries-api/ios/ |
| KMP installation | https://everysight.github.io/maverick-ai-docs/libraries-api/kmp/ |
| Native Android, native iOS and KMP samples | https://github.com/everysight-maverick-AI/sdk/tree/main/samples |
| Download repository ZIP, including samples | https://github.com/everysight-maverick-AI/sdk/archive/refs/heads/main.zip |
| Simulator downloads and connection setup | https://everysight.github.io/maverick-ai-docs/guides/simulator/#setup |
| Request a developer key | https://www.everysight.com/sdk-key |
| Key installation and authentication | https://everysight.github.io/maverick-ai-docs/getting-started/api-key/ |
| Troubleshooting | https://everysight.github.io/maverick-ai-docs/resources/troubleshooting/ |
| Developer support | https://everysight.github.io/maverick-ai-docs/resources/community/ |
| Developer program / product information | https://www.everysight.com/pages/developer / https://www.everysight.com/ |

## Answer the actual question

- Give the relevant direct links and the next practical step, not the whole directory.
- Infer Android, iOS or KMP from the project; ask only when the choice changes the answer.
- The SDK hub ZIP includes sample source, not a replacement for installing platform SDK
  packages. Android uses Maven / GitHub Packages; iOS uses Swift Package Manager. Read
  the platform guide for the current artifact, credentials and supported versions.
- For simulator downloads or a "latest SDK" request, check the current official page.
  Do not freeze version numbers, invent release URLs, or call a cached version latest.
- Distinguish the Everysight developer key (`sdk.key`) from GitHub package credentials.
  Link the setup instructions; do not ask developers to paste secrets into chat.
- For a generated starter, use `evs-mav2-sdk-quickstart` if installed; otherwise point
  to the public quickstart and sample projects.
- For "how do I connect / draw / use a service?", use `evs-mav2-sdk-api-guide` if installed.
  Without it, consult the public guide, API reference and matching platform sample.

## Optional documentation MCP

The Everysight SDK docs MCP may appear as `MavAISDK` or `mav2sdkdocs` in the client.
It provides `describe_maverick_ai_docs`, `list_maverick_ai_docs`,
`search_maverick_ai_docs` and `read_maverick_ai_doc`. Use `portal` for onboarding and
`samples` for working examples; the API skill explains the other sections.

Streamable HTTP endpoint:
https://mav2sdk-docs-mcp-905366563913.us-central1.run.app/mcp

The skill works through the public links above when MCP is not connected. Installing
this skill does not register an MCP server or install the SDK.
