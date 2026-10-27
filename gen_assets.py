#!/usr/bin/env python3
"""生成 App 图标与 TV banner（纯 PIL 绘制，无外部素材）。"""
import os
from PIL import Image, ImageDraw, ImageFont

ROOT = os.path.dirname(os.path.abspath(__file__))
S = 4  # 超采样倍数

BRAND = (0, 193, 255)
DEEP = (0, 152, 212)
PURPLE = (91, 46, 122)


def lerp(a, b, t):
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3))


def bg_gradient(size, c1, c2, diagonal=True):
    """45 度对角渐变：逐像素按 (x+y) 归一化，无接缝。"""
    w, h = size
    img = Image.new("RGB", size, c1)
    px = img.load()
    denom = max(1, w + h - 2)
    for y in range(h):
        for x in range(w):
            px[x, y] = lerp(c1, c2, (x + y) / denom)
    return img


def rounded_mask(size, radius):
    m = Image.new("L", size, 0)
    d = ImageDraw.Draw(m)
    d.rounded_rectangle([0, 0, size[0] - 1, size[1] - 1], radius=radius, fill=255)
    return m


def draw_note(d, box, color=(255, 255, 255, 255)):
    """在给定矩形内绘制一个八分音符（双符头 + 符干 + 符尾横杠）。"""
    x0, y0, x1, y1 = box
    w = x1 - x0
    h = y1 - y0

    def pt(fx, fy):
        return (x0 + w * fx, y0 + h * fy)

    head_r_x = w * 0.135
    head_r_y = h * 0.105
    for cx, cy in ((0.33, 0.74), (0.66, 0.64)):
        cxp, cyp = pt(cx, cy)
        d.ellipse([cxp - head_r_x, cyp - head_r_y, cxp + head_r_x, cyp + head_r_y], fill=color)
    stem_w = w * 0.055
    for fx, top in ((0.33, 0.26), (0.66, 0.16)):
        cxp, cyp = pt(fx, 0.0)
        bottom = pt(fx, 0.74 if fx < 0.5 else 0.64)[1]
        t = pt(fx, top)[1]
        d.rectangle([cxp + head_r_x - stem_w, t, cxp + head_r_x, bottom], fill=color)
    # 符尾（斜横杠）
    p1 = pt(0.33, 0.26)
    p2 = pt(0.66, 0.16)
    bar_h = h * 0.10
    d.polygon([
        (p1[0] + head_r_x - stem_w, p1[1]),
        (p1[0] + head_r_x + stem_w * 1.2, p1[1] + bar_h * 0.55),
        (p2[0] + head_r_x + stem_w * 1.2, p2[1] + bar_h * 0.55),
        (p2[0] + head_r_x - stem_w, p2[1]),
    ], fill=color)


def make_icon(size):
    big = size * S
    img = bg_gradient((big, big), DEEP, PURPLE)
    d = ImageDraw.Draw(img)
    # 右侧柔光
    glow = Image.new("RGBA", (big, big), (0, 0, 0, 0))
    gd = ImageDraw.Draw(glow)
    gd.ellipse([big * 0.35, -big * 0.25, big * 1.35, big * 0.8],
               fill=(0, 193, 255, 70))
    img = Image.alpha_composite(img.convert("RGBA"), glow)
    d = ImageDraw.Draw(img)
    draw_note(d, (big * 0.20, big * 0.20, big * 0.80, big * 0.80))
    img = img.resize((size, size), Image.LANCZOS)
    img.putalpha(rounded_mask((size, size), int(size * 0.22)))
    return img


def find_font(size):
    cands = [
        ("/System/Library/Fonts/PingFang.ttc", 0),
        ("/System/Library/Fonts/PingFang.ttc", 2),
        ("/System/Library/Fonts/Hiragino Sans GB.ttc", 0),
        ("/System/Library/Fonts/Supplemental/Songti.ttc", 0),
        ("/System/Library/Fonts/Supplemental/Arial Bold.ttf", 0),
    ]
    for path, idx in cands:
        if not os.path.exists(path):
            continue
        try:
            return ImageFont.truetype(path, size, index=idx)
        except Exception:
            continue
    return ImageFont.load_default()


def make_banner(w=320, h=180):
    big_w, big_h = w * S, h * S
    img = bg_gradient((big_w, big_h), (10, 14, 22), (26, 20, 48))
    glow = Image.new("RGBA", (big_w, big_h), (0, 0, 0, 0))
    gd = ImageDraw.Draw(glow)
    gd.ellipse([-big_w * 0.3, big_h * 0.35, big_w * 0.75, big_h * 1.6],
               fill=(0, 152, 212, 110))
    img = Image.alpha_composite(img.convert("RGBA"), glow)
    d = ImageDraw.Draw(img)

    # 左侧音符徽标
    badge = int(big_h * 0.30)
    bx, by = int(big_w * 0.07), int(big_h * 0.18)
    bimg = Image.new("RGBA", (badge, badge), (0, 0, 0, 0))
    bd = ImageDraw.Draw(bimg)
    bd.rounded_rectangle([0, 0, badge - 1, badge - 1], radius=int(badge * 0.26),
                         fill=(0, 152, 212, 255))
    draw_note(bd, (badge * 0.12, badge * 0.12, badge * 0.88, badge * 0.88))
    img.alpha_composite(bimg, (bx, by))

    f1 = find_font(int(big_h * 0.22))
    f2 = find_font(int(big_h * 0.10))
    d.text((int(big_w * 0.07), int(big_h * 0.50)), "云影音乐", font=f1, fill=(255, 255, 255, 255))
    d.text((int(big_w * 0.075), int(big_h * 0.78)), "群晖 NAS", font=f2,
           fill=(0, 193, 255, 255))
    return img.resize((w, h), Image.LANCZOS)


def ensure(p):
    os.makedirs(p, exist_ok=True)
    return p


if __name__ == "__main__":
    for d, size in (("mdpi", 48), ("hdpi", 72), ("xhdpi", 96), ("xxhdpi", 144), ("xxxhdpi", 192)):
        folder = ensure(os.path.join(ROOT, "res", "mipmap-" + d))
        make_icon(size).save(os.path.join(folder, "ic_launcher.png"))
    banner_dir = ensure(os.path.join(ROOT, "res", "drawable-xhdpi"))
    make_banner().save(os.path.join(banner_dir, "tv_banner.png"))
    print("assets ok")
