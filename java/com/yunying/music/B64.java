package com.yunying.music;

/** Base64 解码（纯 Java 实现，兼容换行、URL-safe、缺省 padding）。 */
public class B64 {

    private static final String ALPHABET =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";

    public static byte[] decode(String s) {
        if (s == null) return new byte[0];
        int len = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\r' || c == '\n' || c == ' ' || c == '\t' || c == '=') continue;
            len++;
        }
        byte[] out = new byte[len * 3 / 4 + 3];
        int buffer = 0, bits = 0, idx = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '-') c = '+';
            else if (c == '_') c = '/';
            int v = ALPHABET.indexOf(c);
            if (v < 0) continue;
            buffer = (buffer << 6) | v;
            bits += 6;
            if (bits >= 8) {
                bits -= 8;
                out[idx++] = (byte) ((buffer >> bits) & 0xFF);
            }
            buffer &= (1 << bits) - 1;
        }
        if (idx == out.length) return out;
        byte[] trimmed = new byte[idx];
        System.arraycopy(out, 0, trimmed, 0, idx);
        return trimmed;
    }
}
