package com.edgecloud.edge.research;

import ai.onnxruntime.*;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.FloatBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.List;
import static com.edgecloud.edge.research.Research.*;

/** Frozen image-v2 implementation, copied verbatim into Edge and Cloud. */
@Component
public final class ImageModel {
    public static final String VERSION = "squeezenet1.1-7-image-v2";
    public static final String PREPROCESS = "rgb-stretch224-bilinear-imagenet-v1";
    public static final int MAX_BYTES = 8 * 1024 * 1024;
    public static final long MAX_PIXELS = 24_000_000;
    private final OrtEnvironment environment = OrtEnvironment.getEnvironment();
    private OrtSession session;
    private List<String> labels = List.of();
    private String modelSha = "", labelSha = "", failure;
    private String inputName;
    public final String environmentId;

    public ImageModel() {
        String node = System.getenv("AECAI_NODE_ID");
        if (node == null || node.isBlank()) {
            try { node = java.net.InetAddress.getLocalHost().getHostName(); }
            catch (Exception e) { node = "unconfigured-node"; }
        }
        environmentId = node + ":" + System.getProperty("os.arch") + ":ort1.22.0:t1";
        try {
            var metadata = new Properties();
            try (var in = resource("metadata.properties")) { metadata.load(in); }
            byte[] model, labelBytes;
            try (var in = resource("squeezenet1.1-7.onnx")) { model = in.readAllBytes(); }
            try (var in = resource("imagenet_classes.txt")) { labelBytes = in.readAllBytes(); }
            modelSha = sha(model); labelSha = sha(labelBytes);
            require(modelSha.equals(metadata.getProperty("model.sha256")), "Model checksum mismatch");
            require(labelSha.equals(metadata.getProperty("labels.sha256")), "Label checksum mismatch");
            labels = new String(labelBytes, StandardCharsets.UTF_8).lines().toList();
            require(labels.size() == 1000, "Expected 1000 labels");
            try (var options = new OrtSession.SessionOptions()) {
                options.setIntraOpNumThreads(1); options.setInterOpNumThreads(1);
                session = environment.createSession(model, options);
            }
            require(session.getInputNames().size() == 1, "Expected one model input");
            inputName = session.getInputNames().iterator().next();
            infer(new float[3 * 224 * 224]); // readiness/warm-up, excluded from timings
        } catch (Exception e) {
            failure = e.getClass().getSimpleName() + ": " + e.getMessage();
            if (session != null) try { session.close(); } catch (Exception ignored) {}
            session = null;
        }
    }

    private InputStream resource(String name) {
        var stream = getClass().getClassLoader().getResourceAsStream("image-model/" + name);
        if (stream == null) throw new IllegalStateException("Missing image-model/" + name);
        return stream;
    }
    public static String sha(byte[] bytes) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)); }
        catch (Exception e) { throw new IllegalStateException(e); }
    }
    public Capability capability() {
        Map<String,Object> hardware = new LinkedHashMap<>();
        hardware.put("os", System.getProperty("os.name"));
        hardware.put("architecture", System.getProperty("os.arch"));
        hardware.put("logicalProcessors", Runtime.getRuntime().availableProcessors());
        hardware.put("java", System.getProperty("java.version"));
        hardware.put("inferenceThreads", 1);
        if (failure != null) hardware.put("modelError", failure);
        return new Capability(session != null, VERSION, modelSha, labelSha, PREPROCESS, environmentId, hardware);
    }
    public byte[] validate(ImageInput request) {
        require(request != null && request.requestId() != null && !request.requestId().isBlank()
                && request.requestId().length() <= 100 && request.privacy() != null, "Request ID and privacy required");
        if (!VERSION.equals(request.modelVersion()))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "MODEL_VERSION_MISMATCH");
        require(request.imageBase64() != null && request.imageBase64().length() <= ((MAX_BYTES + 2L) / 3) * 4,
                "Image exceeds 8 MiB or is missing");
        byte[] bytes = Base64.getDecoder().decode(request.imageBase64());
        require(bytes.length > 0 && bytes.length <= MAX_BYTES, "Image size out of range");
        return bytes;
    }
    public record Decoded(BufferedImage image, String format) {}
    public Decoded decode(byte[] bytes) {
        try (var input = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            var readers = ImageIO.getImageReaders(input);
            require(readers.hasNext(), "Unsupported or undecodable image");
            var reader = readers.next();
            try {
                reader.setInput(input, true, true);
                int width = reader.getWidth(0), height = reader.getHeight(0);
                require(width > 0 && height > 0 && (long) width * height <= MAX_PIXELS, "Image exceeds 24 million pixels");
                String format = reader.getFormatName().toUpperCase(Locale.ROOT);
                require(Set.of("PNG", "JPEG", "JPG").contains(format), "Only JPEG and PNG are supported");
                BufferedImage image = reader.read(0);
                require(image != null, "Image decode failed");
                return new Decoded(image, format);
            } finally { reader.dispose(); }
        } catch (IOException e) { throw new IllegalArgumentException("Image decode failed", e); }
    }
    public BufferedImage resize(BufferedImage source) {
        var output = new BufferedImage(224, 224, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = output.createGraphics();
        try {
            graphics.setColor(Color.BLACK); graphics.fillRect(0, 0, 224, 224);
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            graphics.drawImage(source, 0, 0, 224, 224, null);
        } finally { graphics.dispose(); }
        return output;
    }
    public byte[] png(BufferedImage image) {
        try {
            var bytes = new ByteArrayOutputStream();
            require(ImageIO.write(image, "png", bytes), "PNG encoder unavailable");
            return bytes.toByteArray();
        } catch (IOException e) { throw new IllegalStateException(e); }
    }
    private float[] tensor(BufferedImage image) {
        float[] values = new float[3 * 224 * 224];
        float[] mean = {.485f, .456f, .406f}, std = {.229f, .224f, .225f};
        for (int y = 0; y < 224; y++) for (int x = 0; x < 224; x++) {
            int rgb = image.getRGB(x, y), offset = y * 224 + x;
            for (int c = 0; c < 3; c++) values[c * 224 * 224 + offset] =
                    (((rgb >> (16 - c * 8)) & 255) / 255f - mean[c]) / std[c];
        }
        return values;
    }
    private float[] infer(float[] input) {
        if (session == null) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "IMAGE_MODEL_NOT_READY");
        try (var value = OnnxTensor.createTensor(environment, FloatBuffer.wrap(input), new long[]{1, 3, 224, 224});
             var result = session.run(Map.of(inputName, value))) {
            var output = new ArrayList<Float>(1000);
            flatten(result.get(0).getValue(), output);
            require(output.size() == 1000, "Expected 1000 model outputs");
            float[] logits = new float[1000];
            for (int i = 0; i < 1000; i++) { logits[i] = output.get(i); require(Float.isFinite(logits[i]), "Non-finite model output"); }
            return logits;
        } catch (OrtException e) { throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "INFERENCE_FAILED", e); }
    }
    private static void flatten(Object value, List<Float> output) {
        if (value instanceof float[] a) for (float x : a) output.add(x);
        else if (value instanceof Object[] a) for (Object x : a) flatten(x, output);
        else throw new IllegalArgumentException("Expected float32 output");
    }
    public Prepared prepare(ImageInput request) {
        long start = System.nanoTime(); byte[] raw = validate(request);
        byte[] encoded = png(resize(decode(raw).image()));
        return new Prepared(request.requestId(), VERSION, PREPROCESS, Base64.getEncoder().encodeToString(encoded),
                raw.length, encoded.length, elapsed(start));
    }
    public Logits logits(ImageInput request, boolean prepared) {
        long start = System.nanoTime(); byte[] raw = validate(request);
        var decoded = decode(raw).image();
        if (prepared) require(decoded.getWidth() == 224 && decoded.getHeight() == 224, "Hybrid image must be 224x224");
        float[] values = tensor(prepared ? decoded : resize(decoded));
        double pre = elapsed(start); long inferenceStart = System.nanoTime();
        float[] output = infer(values); double inference = elapsed(inferenceStart);
        return new Logits(request.requestId(), VERSION, modelSha, labelSha, output, new Stages(pre, inference, 0));
    }
    public Prediction postprocess(Logits output) {
        long start = System.nanoTime();
        require(output != null && VERSION.equals(output.modelVersion()) && modelSha.equals(output.modelSha256())
                && labelSha.equals(output.labelsSha256()), "Incompatible model or labels");
        float[] logits = output.logits();
        require(logits != null && logits.length == 1000 && output.stages() != null, "Expected 1000 logits and timings");
        double max = Double.NEGATIVE_INFINITY;
        for (float value : logits) { require(Float.isFinite(value), "Invalid logits"); max = Math.max(max, value); }
        double[] probabilities = new double[1000]; double sum = 0;
        for (int i = 0; i < 1000; i++) { probabilities[i] = Math.exp(logits[i] - max); sum += probabilities[i]; }
        var order = new ArrayList<Integer>(); for (int i = 0; i < 1000; i++) order.add(i);
        order.sort(Comparator.<Integer>comparingDouble(i -> -probabilities[i]).thenComparingInt(i -> i));
        var top = new ArrayList<ClassScore>();
        for (int i : order.subList(0, 5)) top.add(new ClassScore(i, labels.get(i), probabilities[i] / sum));
        var best = top.get(0); var timing = output.stages();
        return new Prediction(output.requestId(), VERSION, modelSha, labelSha, best.classIndex(), best.label(),
                best.probability(), List.copyOf(top), new Stages(timing.preprocessingMs(), timing.inferenceMs(), elapsed(start)));
    }
    public Prediction predict(ImageInput request) { return postprocess(logits(request, false)); }
    public static double elapsed(long start) { return (System.nanoTime() - start) / 1e6; }
    @PreDestroy public void close() throws OrtException { if (session != null) session.close(); }
}
