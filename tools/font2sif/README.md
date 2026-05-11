# font2sif2

Converts TrueType (.ttf) and OpenType (.otf) fonts to SIF2 (System Independent Font v2) format for the Maverick AI glasses embedded display.

## Why?

The glasses embedded OS does not support vector fonts. Fonts must be pre-rendered to a pixel-based format. This tool handles that conversion.

## Requirements

1. [Python 3.8+](https://www.python.org/)
2. Python modules:

```bash
pip3 install numpy freetype-py Pillow
```

## Usage

```bash
python3 font2sif2.py --path Roboto/static/Roboto-Medium.ttf --size 16
```

### Character Ranges

By default the tool includes standard ASCII. To add specific Unicode ranges (e.g. extended Latin):

```bash
python3 font2sif2.py --path Roboto/static/Roboto-Medium.ttf --ranges 0x0020:0x007E 0x00A0:0x00FF --size 16
```

### Adding Special Characters

To include specific symbols (e.g. degree sign °):

```bash
python3 font2sif2.py --path Roboto-Medium.ttf --ranges 0x0020:0x007E 0x00B0:0x00B0 --size 24
```

In Kotlin, reference the character with its Unicode escape:

```kotlin
val celsius = "\u00B0"
```

### Options

| Option | Description |
|--------|-------------|
| `--path` | Path to the font file (.ttf or .otf) |
| `--size` | Font size in pixels (e.g. value from Figma) |
| `--ranges` | Unicode ranges to include (space-separated `start:end` pairs) |
| `--exclude` | Unicode code points to exclude |
| `--bpp` | Bits per pixel: 1, 2, or 4 (default: 2) |

## Output Files

Per execution, the script generates:

| File | Description |
|------|-------------|
| `[Font].[HxW].[X]bpp.sifz` | Compressed SIF2 file — **add to app resources** |
| `[Font].[HxW].[X]bpp.json` | Metadata JSON — **add to app resources** |
| `[Font].[HxW].[X]bpp.sif` | Uncompressed SIF2 (for back-compatibility, can be ignored) |
| `[Font].[HxW].[X]bpp.png` | Visual preview image for validation |

## Example

```bash
python3 font2sif2.py --path ./Saira-Regular.ttf --ranges 0x0020:0x007E 0x00E0:0x00FF --size 43
```

Output:

- `Saira-Regular.ttf.43x43.2bpp.sifz` (add to app resources)
- `Saira-Regular.ttf.43x43.2bpp.json` (add to app resources)
- `Saira-Regular.ttf.43x43.2bpp.sif`
- `Saira-Regular.ttf.43x43.2bpp.png`
