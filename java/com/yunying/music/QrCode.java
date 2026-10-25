package com.yunying.music;

/**
 * 极简二维码编码器（Byte 模式 / 纠错等级 L / 版本 1-10 / 固定掩码 0）。
 * 纯 Java，无 Android 依赖，可用桌面 JVM 单测。
 */
public class QrCode {

    /* 版本 1-10，纠错等级 L 的分块表 */
    private static final int[] TOTAL_CODEWORDS = {26, 44, 70, 100, 134, 172, 196, 242, 292, 346};
    private static final int[] EC_PER_BLOCK    = {7, 10, 15, 20, 26, 18, 20, 24, 30, 18};
    private static final int[] BLOCKS_G1       = {1, 1, 1, 1, 1, 2, 2, 2, 2, 2};
    private static final int[] DATA_G1         = {19, 34, 55, 80, 108, 68, 78, 97, 116, 68};
    private static final int[] BLOCKS_G2       = {0, 0, 0, 0, 0, 0, 0, 0, 0, 2};
    private static final int[] DATA_G2         = {0, 0, 0, 0, 0, 0, 0, 0, 0, 69};

    private static final int MASK = 0;

    private final int version;
    private final int size;
    private final boolean[][] modules;
    private final boolean[][] isFunction;

    private QrCode(int version) {
        this.version = version;
        this.size = version * 4 + 17;
        this.modules = new boolean[size][size];
        this.isFunction = new boolean[size][size];
    }

    /** 编码 UTF-8 文本，返回 [y][x] 模块矩阵 */
    public static boolean[][] encode(String text) throws Exception {
        byte[] data = text.getBytes("UTF-8");
        int ver = -1;
        for (int v = 1; v <= 10; v++) {
            int headerBits = v <= 9 ? 12 : 20;
            int capacityBits = (DATA_G1[v - 1] * BLOCKS_G1[v - 1]
                    + DATA_G2[v - 1] * BLOCKS_G2[v - 1]) * 8;
            if (headerBits + data.length * 8 <= capacityBits) {
                ver = v;
                break;
            }
        }
        if (ver < 0) throw new Exception("二维码内容过长：" + data.length + " 字节");
        QrCode q = new QrCode(ver);
        byte[] codewords = q.buildCodewords(data);
        q.drawFunctionPatterns();
        q.drawCodewords(codewords);
        q.applyMask();
        q.drawFormatBits();
        return q.modules;
    }

    /* ------------------------------ 数据编码 ------------------------------ */

    private byte[] buildCodewords(byte[] data) {
        int dataCapacity = DATA_G1[version - 1] * BLOCKS_G1[version - 1]
                + DATA_G2[version - 1] * BLOCKS_G2[version - 1];
        int bits = dataCapacity * 8;
        BitBuf bb = new BitBuf(bits);
        bb.append(0b0100, 4);                       // Byte 模式
        bb.append(data.length, version <= 9 ? 8 : 16);
        for (byte b : data) bb.append(b & 0xFF, 8);
        bb.append(0, Math.min(4, bits - bb.len));   // 终止符
        bb.append(0, (8 - bb.len % 8) % 8);         // 补齐到字节
        for (int pad = 0xEC; bb.len < bits; pad ^= 0xEC ^ 0x11) bb.append(pad, 8);

        byte[] dat = bb.toBytes();
        int ecLen = EC_PER_BLOCK[version - 1];
        int numBlocks = BLOCKS_G1[version - 1] + BLOCKS_G2[version - 1];
        byte[][] dataBlocks = new byte[numBlocks][];
        byte[][] ecBlocks = new byte[numBlocks][];
        int k = 0;
        for (int i = 0; i < numBlocks; i++) {
            int len = i < BLOCKS_G1[version - 1] ? DATA_G1[version - 1] : DATA_G2[version - 1];
            dataBlocks[i] = new byte[len];
            System.arraycopy(dat, k, dataBlocks[i], 0, len);
            k += len;
            ecBlocks[i] = reedSolomon(dataBlocks[i], ecLen);
        }
        byte[] out = new byte[TOTAL_CODEWORDS[version - 1]];
        int p = 0;
        int maxData = Math.max(DATA_G1[version - 1], DATA_G2[version - 1]);
        for (int i = 0; i < maxData; i++) {
            for (int b = 0; b < numBlocks; b++) {
                if (i < dataBlocks[b].length) out[p++] = dataBlocks[b][i];
            }
        }
        for (int i = 0; i < ecLen; i++) {
            for (int b = 0; b < numBlocks; b++) out[p++] = ecBlocks[b][i];
        }
        return out;
    }

    /* --------------------------- Reed-Solomon --------------------------- */

    private static final int[] EXP = new int[512];
    private static final int[] LOG = new int[256];

    static {
        int x = 1;
        for (int i = 0; i < 255; i++) {
            EXP[i] = x;
            LOG[x] = i;
            x <<= 1;
            if (x >= 256) x ^= 0x11D;
        }
        for (int i = 255; i < 512; i++) EXP[i] = EXP[i - 255];
    }

    private static int mul(int a, int b) {
        if (a == 0 || b == 0) return 0;
        return EXP[LOG[a] + LOG[b]];
    }

    private static byte[] reedSolomon(byte[] data, int ecLen) {
        int[] gen = new int[ecLen];
        gen[ecLen - 1] = 1;
        int root = 1;
        for (int i = 0; i < ecLen; i++) {
            for (int j = 0; j < ecLen; j++) {
                gen[j] = mul(gen[j], root);
                if (j + 1 < ecLen) gen[j] ^= gen[j + 1];
            }
            root = mul(root, 2);
        }
        int[] rem = new int[ecLen];
        for (byte b : data) {
            int factor = (b & 0xFF) ^ rem[0];
            System.arraycopy(rem, 1, rem, 0, ecLen - 1);
            rem[ecLen - 1] = 0;
            for (int i = 0; i < ecLen; i++) rem[i] ^= mul(gen[i], factor);
        }
        byte[] out = new byte[ecLen];
        for (int i = 0; i < ecLen; i++) out[i] = (byte) rem[i];
        return out;
    }

    /* ---------------------------- 功能图形 ---------------------------- */

    private void setFunc(int x, int y, boolean dark) {
        modules[y][x] = dark;
        isFunction[y][x] = true;
    }

    private void drawFunctionPatterns() {
        for (int i = 0; i < size; i++) {
            setFunc(6, i, i % 2 == 0);
            setFunc(i, 6, i % 2 == 0);
        }
        drawFinder(3, 3);
        drawFinder(size - 4, 3);
        drawFinder(3, size - 4);

        int[] align = alignmentPositions();
        int n = align.length;
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if ((i == 0 && j == 0) || (i == 0 && j == n - 1) || (i == n - 1 && j == 0)) continue;
                drawAlignment(align[i], align[j]);
            }
        }
        // 预留格式信息区
        for (int i = 0; i <= 8; i++) {
            if (!isFunction[8][i]) setFunc(i, 8, false);
            if (!isFunction[i][8]) setFunc(8, i, false);
        }
        for (int i = 0; i < 8; i++) {
            setFunc(size - 1 - i, 8, false);
            setFunc(8, size - 1 - i, false);
        }
        setFunc(8, size - 8, true);

        if (version >= 7) {
            int rem = version;
            for (int i = 0; i < 12; i++) rem = (rem << 1) ^ ((rem >>> 11) * 0x1F25);
            int bits = (version << 12) | rem;
            for (int i = 0; i < 18; i++) {
                boolean bit = ((bits >>> i) & 1) != 0;
                int a = size - 11 + i % 3;
                int b = i / 3;
                setFunc(a, b, bit);
                setFunc(b, a, bit);
            }
        }
    }

    private void drawFinder(int cx, int cy) {
        for (int dy = -4; dy <= 4; dy++) {
            for (int dx = -4; dx <= 4; dx++) {
                int x = cx + dx, y = cy + dy;
                if (x < 0 || y < 0 || x >= size || y >= size) continue;
                int dist = Math.max(Math.abs(dx), Math.abs(dy));
                setFunc(x, y, dist != 2 && dist != 4);
            }
        }
    }

    private void drawAlignment(int cx, int cy) {
        for (int dy = -2; dy <= 2; dy++) {
            for (int dx = -2; dx <= 2; dx++) {
                setFunc(cx + dx, cy + dy, Math.max(Math.abs(dx), Math.abs(dy)) != 1);
            }
        }
    }

    private int[] alignmentPositions() {
        if (version == 1) return new int[0];
        int num = version / 7 + 2;
        int step = (version * 4 + num * 2 + 1) / (num * 2 - 2) * 2;
        int[] r = new int[num];
        r[0] = 6;
        for (int i = num - 1, pos = version * 4 + 10; i >= 1; i--, pos -= step) r[i] = pos;
        return r;
    }

    /* ---------------------------- 数据落点 ---------------------------- */

    private void drawCodewords(byte[] data) {
        int i = 0;
        int totalBits = data.length * 8;
        for (int right = size - 1; right >= 1; right -= 2) {
            if (right == 6) right = 5;
            for (int vert = 0; vert < size; vert++) {
                for (int j = 0; j < 2; j++) {
                    int x = right - j;
                    boolean upward = ((right + 1) & 2) == 0;
                    int y = upward ? size - 1 - vert : vert;
                    if (!isFunction[y][x] && i < totalBits) {
                        boolean bit = ((data[i >>> 3] >>> (7 - (i & 7))) & 1) != 0;
                        modules[y][x] = bit;
                        i++;
                    }
                }
            }
        }
    }

    private void applyMask() {
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                if (isFunction[y][x]) continue;
                boolean invert = MASK == 0 && (x + y) % 2 == 0;
                if (invert) modules[y][x] = !modules[y][x];
            }
        }
    }

    private void drawFormatBits() {
        int data = (0b01 << 3) | MASK;   // 纠错等级 L = 01
        int rem = data;
        for (int i = 0; i < 10; i++) rem = (rem << 1) ^ ((rem >>> 9) * 0x537);
        int bits = ((data << 10) | rem) ^ 0x5412;

        for (int i = 0; i <= 5; i++) setFunc(8, i, ((bits >>> i) & 1) != 0);
        setFunc(8, 7, ((bits >>> 6) & 1) != 0);
        setFunc(8, 8, ((bits >>> 7) & 1) != 0);
        setFunc(7, 8, ((bits >>> 8) & 1) != 0);
        for (int i = 9; i < 15; i++) setFunc(14 - i, 8, ((bits >>> i) & 1) != 0);

        for (int i = 0; i < 8; i++) setFunc(size - 1 - i, 8, ((bits >>> i) & 1) != 0);
        for (int i = 8; i < 15; i++) setFunc(8, size - 15 + i, ((bits >>> i) & 1) != 0);
        setFunc(8, size - 8, true);
    }

    /* ------------------------------ 位缓冲 ------------------------------ */

    private static class BitBuf {
        private final byte[] buf;
        int len;

        BitBuf(int capacityBits) {
            buf = new byte[(capacityBits + 7) / 8];
        }

        void append(int value, int bits) {
            for (int i = bits - 1; i >= 0; i--) {
                if (((value >>> i) & 1) != 0) {
                    buf[len >>> 3] |= (byte) (1 << (7 - (len & 7)));
                }
                len++;
            }
        }

        byte[] toBytes() { return buf; }
    }
}
