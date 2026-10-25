import com.yunying.music.QrCode;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;

/** 桌面端自测：把二维码渲染成 PNG，供 cv2 反向解码校验。 */
public class QrTest {

    static void save(String text, String file, int scale) throws Exception {
        boolean[][] m = QrCode.encode(text);
        int quiet = 4;
        int dim = (m.length + quiet * 2) * scale;
        BufferedImage img = new BufferedImage(dim, dim, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < dim; y++) {
            for (int x = 0; x < dim; x++) {
                int mx = x / scale - quiet;
                int my = y / scale - quiet;
                boolean dark = mx >= 0 && my >= 0 && mx < m.length && my < m.length && m[my][mx];
                img.setRGB(x, y, dark ? 0x000000 : 0xFFFFFF);
            }
        }
        ImageIO.write(img, "png", new File(file));
        System.out.println("ok " + file + " modules=" + m.length);
    }

    public static void main(String[] a) throws Exception {
        new File("/tmp/qrtest").mkdirs();
        save("https://passport.aliyundrive.com/qrcodeCheck.htm?lgToken=10e93ea0bdeafda8b94f0b3574af985a0_0000000&_from=havana",
                "/tmp/qrtest/q1.png", 6);
        save("HELLO WORLD 123", "/tmp/qrtest/q2.png", 8);
        save("云影音乐 · NAS 扫码登录测试", "/tmp/qrtest/q3.png", 8);
        StringBuilder sb = new StringBuilder("https://example.com/?");
        while (sb.length() < 128) sb.append("abcdefgh");
        save(sb.toString(), "/tmp/qrtest/q4.png", 5);
        save("a", "/tmp/qrtest/q5.png", 10);
    }
}
