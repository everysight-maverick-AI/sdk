# Maverick AI SDK skills

AI-assistant skills for building with the Maverick AI SDK. Each folder is one skill; they work with
Claude Code and Codex.

| Skill | What it does |
|---|---|
| [`evs-mav2-sdk-quickstart`](evs-mav2-sdk-quickstart/SKILL.md) | Generates a runnable project wired to the latest published SDK, with your first HUD screen: Compose Multiplatform (Android + iOS from one codebase) by default, or native Android and/or iOS. Includes a Python generator (3.10+) you can also run without an AI client. |
| [`evs-mav2-sdk-assistant`](evs-mav2-sdk-assistant/SKILL.md) | Finds the SDK, platform packages, documentation, samples, developer keys and simulator setup. |
| [`evs-mav2-sdk-api-guide`](evs-mav2-sdk-api-guide/SKILL.md) | Helps with the public API - connecting glasses, drawing text and controls, input, resources, sensors, camera, audio, AR. Uses the documentation MCP when connected, otherwise the public docs and samples. |

## Install

Copy the skill folder into your client's skills directory, then start a new session:

macOS / Linux (bash, zsh):

```bash
mkdir -p ~/.claude/skills && cp -R evs-mav2-sdk-quickstart ~/.claude/skills/   # Claude Code
mkdir -p ~/.codex/skills  && cp -R evs-mav2-sdk-quickstart ~/.codex/skills/    # Codex
```

Windows (PowerShell):

```powershell
New-Item -ItemType Directory -Force "$HOME\.claude\skills" | Out-Null; Copy-Item -Recurse evs-mav2-sdk-quickstart "$HOME\.claude\skills\"   # Claude Code
New-Item -ItemType Directory -Force "$HOME\.codex\skills"  | Out-Null; Copy-Item -Recurse evs-mav2-sdk-quickstart "$HOME\.codex\skills\"    # Codex
```

Repeat for the other two skills.

Then ask, for example: *"Use $evs-mav2-sdk-quickstart to create HelloGlasses for Android and iOS in ~/dev."*

To run the quickstart generator without an AI client:

```bash
python3 evs-mav2-sdk-quickstart/scripts/new_mav2_project.py --name HelloGlasses --out ~/dev   # macOS / Linux
```

```powershell
py evs-mav2-sdk-quickstart\scripts\new_mav2_project.py --name HelloGlasses --out $HOME\dev    # Windows
```

On Windows you can build and run the Android app; building the iOS app needs a Mac with Xcode.

## Documentation MCP

The skills work best with the SDK documentation MCP connected. Add a **Streamable HTTP** server named
`mav2sdkdocs` in your AI client's MCP settings:

```
https://mav2sdk-docs-mcp-905366563913.us-central1.run.app/mcp
```

Installing a skill does not connect the MCP; the assistant and API guide fall back to the public
documentation when it is not connected.

See the [Vibe coding](https://everysight.github.io/maverick-ai-docs/getting-started/vibe-coding/) page for more.
