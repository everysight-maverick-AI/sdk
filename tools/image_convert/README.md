# Maverick AI / AI Pro Image Converter Tool

`evsimgconvert.py` converts PNG and JPG images to the format and resolution supported by the Maverick AI glasses display.

The glasses display uses 16-bit color. Images can be converted to:

- **8-bit (RGB332)** - smaller file, faster upload, reduced color fidelity
- **16-bit (RGB565 / RGBA4444)** - full display color depth

## Requirements

1. [Python 3.8+](https://www.python.org/)
2. Python module: `Pillow`

```bash
pip3 install Pillow
```

## Usage

Convert an image to 8-bit format (default):

```bash
python3 evsimgconvert.py source_image.png -o output_image.png
```

Convert to 16-bit for higher color fidelity:

```bash
python3 evsimgconvert.py source_image.png -o output_image.png --bpp 16
```

Preserve alpha channel:

```bash
python3 evsimgconvert.py --alpha source_image.png -o output_image.png
```

### Sprite Sheet Example

To convert a sprite sheet for widget icons:

```bash
python3 evsimgconvert.py sprite_output/WidgetSpriteIcons.png -o widget_icons.png --alpha --bpp 16
```

## Batch Conversion

Use `conv_folder.py` to convert all images in a folder:

```bash
python3 conv_folder.py /path/to/input_folder /path/to/output_folder
```
