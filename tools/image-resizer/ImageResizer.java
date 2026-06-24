import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

/**
 * Standalone image-size reducer for project assets.
 *
 * This tool intentionally lives outside the Android module and uses only the JDK.
 */
public final class ImageResizer {
    private ImageResizer() {
    }

    public static void main(String[] args) throws IOException {
        Options options = Options.parse(args);
        if (options.help) {
            Options.printHelp();
            return;
        }

        BufferedImage input = ImageIO.read(options.input);
        if (input == null) {
            throw new IOException("Could not read image: " + options.input);
        }

        int[] size = scaledSize(input.getWidth(), input.getHeight(), options.maxWidth, options.maxHeight);
        BufferedImage output = resize(input, size[0], size[1]);

        File parent = options.output.getParentFile();
        if (parent != null) {
            parent.mkdirs();
        }

        String format = extension(options.output);
        if ("jpg".equals(format) || "jpeg".equals(format)) {
            writeJpeg(output, options.output, options.quality);
        } else {
            ImageIO.write(output, format, options.output);
        }

        long inBytes = options.input.length();
        long outBytes = options.output.length();
        System.out.println("Wrote " + options.output.getPath());
        System.out.println(input.getWidth() + "x" + input.getHeight() + " -> " + size[0] + "x" + size[1]);
        if (inBytes > 0 && outBytes > 0) {
            double saved = 100.0 - ((outBytes * 100.0) / inBytes);
            System.out.printf("%d bytes -> %d bytes (%.1f%% smaller)%n", inBytes, outBytes, saved);
        }
    }

    private static int[] scaledSize(int width, int height, int maxWidth, int maxHeight) {
        double scale = Math.min(maxWidth / (double) width, maxHeight / (double) height);
        scale = Math.min(scale, 1.0);
        return new int[] {
            Math.max(1, (int) Math.round(width * scale)),
            Math.max(1, (int) Math.round(height * scale))
        };
    }

    private static BufferedImage resize(BufferedImage input, int width, int height) {
        int type = input.getColorModel().hasAlpha() ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB;
        BufferedImage output = new BufferedImage(width, height, type);
        Graphics2D graphics = output.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.drawImage(input, 0, 0, width, height, null);
        graphics.dispose();
        return output;
    }

    private static void writeJpeg(BufferedImage image, File output, float quality) throws IOException {
        BufferedImage rgb = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = rgb.createGraphics();
        graphics.drawImage(image, 0, 0, null);
        graphics.dispose();

        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpg");
        if (!writers.hasNext()) {
            throw new IOException("No JPEG writer is available in this JDK.");
        }
        ImageWriter writer = writers.next();
        ImageWriteParam param = writer.getDefaultWriteParam();
        param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        param.setCompressionQuality(quality);
        try (ImageOutputStream stream = ImageIO.createImageOutputStream(output)) {
            writer.setOutput(stream);
            writer.write(null, new IIOImage(rgb, null, null), param);
        } finally {
            writer.dispose();
        }
    }

    private static String extension(File file) {
        String name = file.getName();
        int dot = name.lastIndexOf('.');
        if (dot < 0 || dot == name.length() - 1) {
            throw new IllegalArgumentException("Output file must end with .png, .jpg, or .jpeg");
        }
        String extension = name.substring(dot + 1).toLowerCase();
        if (!extension.equals("png") && !extension.equals("jpg") && !extension.equals("jpeg")) {
            throw new IllegalArgumentException("Unsupported output format: " + extension);
        }
        return extension;
    }

    private static final class Options {
        private final File input;
        private final File output;
        private final int maxWidth;
        private final int maxHeight;
        private final float quality;
        private final boolean help;

        private Options(File input, File output, int maxWidth, int maxHeight, float quality, boolean help) {
            this.input = input;
            this.output = output;
            this.maxWidth = maxWidth;
            this.maxHeight = maxHeight;
            this.quality = quality;
            this.help = help;
        }

        private static Options parse(String[] args) {
            File input = null;
            File output = null;
            int maxWidth = 1024;
            int maxHeight = 1024;
            float quality = 0.82f;

            for (int i = 0; i < args.length; i++) {
                String arg = args[i];
                if ("--help".equals(arg) || "-h".equals(arg)) {
                    return new Options(null, null, maxWidth, maxHeight, quality, true);
                } else if ("--input".equals(arg)) {
                    input = new File(requireValue(args, ++i, arg));
                } else if ("--output".equals(arg)) {
                    output = new File(requireValue(args, ++i, arg));
                } else if ("--max-width".equals(arg)) {
                    maxWidth = positiveInt(requireValue(args, ++i, arg), arg);
                } else if ("--max-height".equals(arg)) {
                    maxHeight = positiveInt(requireValue(args, ++i, arg), arg);
                } else if ("--quality".equals(arg)) {
                    quality = quality(requireValue(args, ++i, arg));
                } else {
                    throw new IllegalArgumentException("Unknown argument: " + arg);
                }
            }

            if (input == null || output == null) {
                throw new IllegalArgumentException("--input and --output are required. Use --help for examples.");
            }
            return new Options(input, output, maxWidth, maxHeight, quality, false);
        }

        private static void printHelp() {
            System.out.println("ImageResizer - standalone image-size reducer");
            System.out.println();
            System.out.println("Usage:");
            System.out.println("  java ImageResizer --input in.png --output out.jpg --max-width 900 --max-height 900 --quality 0.82");
            System.out.println();
            System.out.println("Options:");
            System.out.println("  --input       Source PNG/JPG/JPEG image");
            System.out.println("  --output      Destination .png, .jpg, or .jpeg");
            System.out.println("  --max-width   Maximum output width, default 1024");
            System.out.println("  --max-height  Maximum output height, default 1024");
            System.out.println("  --quality     JPEG quality from 0.0 to 1.0, default 0.82");
        }

        private static String requireValue(String[] args, int index, String option) {
            if (index >= args.length) {
                throw new IllegalArgumentException(option + " requires a value.");
            }
            return args[index];
        }

        private static int positiveInt(String value, String option) {
            int parsed = Integer.parseInt(value);
            if (parsed < 1) {
                throw new IllegalArgumentException(option + " must be at least 1.");
            }
            return parsed;
        }

        private static float quality(String value) {
            float parsed = Float.parseFloat(value);
            if (parsed <= 0.0f || parsed > 1.0f) {
                throw new IllegalArgumentException("--quality must be greater than 0.0 and at most 1.0.");
            }
            return parsed;
        }
    }
}
