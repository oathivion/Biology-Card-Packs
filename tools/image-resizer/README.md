# Image Resizer Tool

Standalone utility for shrinking PNG/JPG images before adding them to the Android app.

This tool is intentionally outside the Android module and uses only the JDK.

## Compile

From this folder:

```powershell
javac ImageResizer.java
```

## Run

```powershell
java ImageResizer --input original.png --output resized.jpg --max-width 900 --max-height 900 --quality 0.82
```

## Options

- `--input`: source `.png`, `.jpg`, or `.jpeg`
- `--output`: destination `.png`, `.jpg`, or `.jpeg`
- `--max-width`: maximum output width, default `1024`
- `--max-height`: maximum output height, default `1024`
- `--quality`: JPEG quality from `0.0` to `1.0`, default `0.82`

The tool keeps the original aspect ratio and will not enlarge small images.
