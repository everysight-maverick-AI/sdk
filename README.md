# Maverick AI / AI Pro SDK


## Welcome, Developers!

This is the official Maverick AI / AI Pro SDK repository - your central hub for building apps for Maverick AI smart glasses.

Here you'll find everything you need to get started: release notes, sample projects, development tools, and issue tracking. For the complete SDK documentation and [API reference](https://everysight.github.io/maverick-ai-docs/api-reference/index.html), visit the [developer Portal](https://everysight.github.io/maverick-ai-docs/).


## What's in This Repo

| Folder | Description |
|--------|-------------|
| [`releasenotes/`](./releasenotes/) | SDK version history and changelogs |
| [`samples/`](./samples/) | Sample applications for Android and iOS |
| [`tools/`](./tools/) | Development tools - image converter, font converter and the Glasses Simulator |


## Getting Started

1. Get your Maverick AI / AI Pro glasses or download the [Glasses Simulator](./tools/simulator/)
2. Obtain your developer key at [everysight.com/sdk-key](https://www.everysight.com/sdk-key)
3. Follow the [Quickstart Guide](https://everysight.github.io/maverick-ai-docs/getting-started/quickstart/) in the Developer Portal
4. Explore the [samples](./samples/) for working examples

## SDK Libraries

| Platform | Method | Artifact |
|----------|--------|----------|
| Android | Maven (GitHub Packages) | `com.everysight.mav2:maverick-ai-sdk` |
| iOS | Swift Package Manager | `https://github.com/everysight-maverick-AI/mav-ai-ios-spm` |

## Hello Glasses

```kotlin
class HelloScreen : M2Screen(540f, 280f) {
    override fun onCreate() {
        val title = M2Text()
        title.setFont(M2FontResource.fontMedium)
            .setText("Hello Maverick AI!")
            .setAlign(Align.CenterHorizontal)
            .setX(width / 2f).setY(height / 2f)
            .setColor(M2Color.White)
        add(title)
    }
}
```

## Support

Questions, bugs, or feature requests? Visit our [Community](https://everysight.github.io/maverick-ai-docs/resources/community/) page

---

*Everysight Team*
