---
name: evs-mav2-sdk-api-guide
description: Help app developers use the public Everysight Maverick AI SDK API with documentation and working examples. Use for connecting glasses, drawing text or button-like controls, handling input, resources, display, sensors, camera, audio or AR. Prefer the SDK documentation MCP; fall back to public docs and samples. Not an SDK-internals development workflow.
---

# Maverick AI SDK: API guide and MCP

A lightweight guide for apps consuming Maverick AI / AI Pro, on native Android,
native iOS and KMP. Works at any event and in ordinary development, without a
private Everysight checkout. Retrieve API details as needed instead of copying a
versioned API manual into the skill.

## Answer with the developer's platform and version

Infer the platform and SDK version from the app; ask only if they affect the answer.
Read the relevant guide, matching platform sample and public API declarations before
writing SDK code. Use the version the app actually depends on. If only newer docs
are available, state that limitation rather than silently changing dependencies.

Provide a focused explanation and the smallest useful Kotlin or Swift example,
including necessary imports, setup and lifecycle placement. Cite the specific public
guide or sample; for MCP-only evidence cite its section and relative file path.
Distinguish verified API calls from proposed application logic and untested examples.

## Find the authoritative material

Prefer the connected documentation MCP, often named `MavAISDK` or `mav2sdkdocs`.
Discover its tools by name; the client-specific tool prefix can differ.

| Tool | Use |
| --- | --- |
| `describe_maverick_ai_docs({})` | Discover documentation sections |
| `search_maverick_ai_docs({query, section, max_results})` | Find a concept or exact symbol; start with 5-10 results |
| `read_maverick_ai_doc({relative_path, section, start_line, max_lines})` | Read the relevant result and its surrounding code |
| `list_maverick_ai_docs({section})` | Locate a file when search is insufficient |

Use `portal` for guides, `api-reference` for classes/methods/parameters,
and `public-sdk-dev-docs` for broader public context. For code, also consult `samples-android-native`,
`samples-ios-native` or `samples-kmp-compose`; `samples` searches all three.

Example searches: `glassesService.connect` in `samples-android-native`, `M2Text(` in
`samples`, and the discovered class or method in `api-reference`. Read the returned
files before composing an answer; a search hit alone is not the implementation.

## Practical API map

Guide paths below are relative to https://everysight.github.io/maverick-ai-docs/.
The equivalent MCP `portal` path replaces the trailing `/` with `.md`.

| Task | Starting model and source |
| --- | --- |
| Connect glasses | Initialize `Evs`, supply the developer key and platform permissions, configure the target glasses, register connection callbacks, then connect. Use `Evs.glassesService` and wait for readiness before presenting the HUD. See `getting-started/start-development/`, `getting-started/api-key/` and `guides/communication/`. |
| Draw text | Build an `M2Screen`, add `M2Text` in `onCreate()`, set font/position/color, and present it through `Evs.screenService.addScreen(...)`. See `ui-kit/ui-kit-overview/`, `ui-kit/fonts/` and the platform sample's HUD screen. |
| Draw a button | First distinguish a phone UI button from a control rendered on glasses. For a glasses control, compose `M2Panel`, rectangle and `M2Text` drawables; implement selection/activation using the supported input events. See `ui-kit/custom-controls/`, `ui-kit/ui-lifecycle/` and `guides/glasses/`. Verify whether the requested SDK version supplies a higher-level control; never invent an `M2Button` API. |
| Handle input / lifecycle | Read `M2Screen.onTouch`, the exact touch type and the relevant glasses events. Use `onCreate()` / `onRelease()` and register/unregister listeners with their owner. Do not assume glasses provide touchscreen coordinates. See `ui-kit/ui-lifecycle/` and `guides/glasses/`. |
| Images, fonts and files | Keep phone UI resources distinct from SDK files sent to glasses. Follow `guides/resources/`, `ui-kit/images/` and `ui-kit/fonts/` for platform packaging and resolver requirements. |
| Other public services | Route to `guides/display/`, `guides/inertial-sensors/`, `guides/vision/`, `guides/microphone/`, `guides/eye-tracker/`, `guides/ota/` or `los-kit/overview/`, then verify the named service in the API reference and samples. |

The drawing tree is retained: add objects and update their properties; do not invent
a phone-side loop that clears and recreates the entire HUD. Account for screens being
released and recreated. Keep Swift examples Swift; check exported names against the
iOS sample instead of mechanically translating Kotlin or trusting a mislabeled tab.
When guides and samples disagree, verify the version-specific declaration and call
out the discrepancy rather than combining incompatible APIs.

## MCP connection and fallback

Transport: **Streamable HTTP**. Server name: `mav2sdkdocs` (a client-local alias).
URL: https://mav2sdk-docs-mcp-905366563913.us-central1.run.app/mcp

Add that URL through the AI client's MCP settings when the user wants MCP connected.
This skill itself does not register a server, bundle credentials, or contain a docs
snapshot. If MCP is absent or a call fails, continue with these public sources:

- Guides: https://everysight.github.io/maverick-ai-docs/
- API: https://everysight.github.io/maverick-ai-docs/api-reference/index.html
- Samples: https://github.com/everysight-maverick-AI/sdk/tree/main/samples
- Platform setup: https://everysight.github.io/maverick-ai-docs/libraries-api/overview/

Open the actual pages/files and follow their API links. Do not construct unverified
Dokka deep links. If neither docs nor samples can be reached, explain what remains
unverified; do not invent signatures. For download/resource discovery use
`evs-mav2-sdk-assistant` if available; for generating a new project use
`evs-mav2-sdk-quickstart` if available. Neither is required to use this API skill.
