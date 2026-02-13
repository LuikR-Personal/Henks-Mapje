package org.minimap.Services;

public final class SampleCodec {

    private static final int SAMPLE_COUNT = 16;

    private SampleCodec() {
    }

    public static String encode(int[] samples) {
        if (samples == null || samples.length != SAMPLE_COUNT) {
            return null;
        }
        StringBuilder sb = new StringBuilder(samples.length * 8);
        for (int i = 0; i < samples.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(samples[i]);
        }
        return sb.toString();
    }

    public static int[] decode(String raw) {
        if (raw == null || raw.isEmpty()) {
            return null;
        }
        String[] parts = raw.split(",");
        if (parts.length != SAMPLE_COUNT) {
            return null;
        }
        int[] samples = new int[SAMPLE_COUNT];
        for (int i = 0; i < parts.length; i++) {
            try {
                samples[i] = Integer.parseInt(parts[i]);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return samples;
    }
}
