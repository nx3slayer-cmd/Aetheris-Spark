#!/usr/bin/env python3
import os
from PIL import Image, ImageDraw, ImageFilter, ImageFont

OUTPUT_DIR = "icon_options"
os.makedirs(OUTPUT_DIR, exist_ok=True)
SIZE = 1024
SS_SIZE = 2048

def find_system_font(size):
    font_paths = [
        "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf",
        "/usr/share/fonts/truetype/liberation/LiberationSans-Bold.ttf",
        "/usr/share/fonts/truetype/freefont/FreeSansBold.ttf",
        "/usr/share/fonts/truetype/ubuntu/Ubuntu-B.ttf"
    ]
    for path in font_paths:
        if os.path.exists(path):
            try:
                return ImageFont.truetype(path, size)
            except Exception:
                pass
    return ImageFont.load_default()

def draw_prismatic_base(
    bg_plate_fill,
    bg_plate_outline,
    glow_colors,
    facet_palette,
    badge_bg,
    badge_outline,
    badge_text_col,
    badge_text="KALLISTO • PRO"
):
    img = Image.new("RGBA", (SS_SIZE, SS_SIZE), (0, 0, 0, 255))
    draw = ImageDraw.Draw(img)

    # 1. Outer Bento Squircle Plate
    margin = 120
    draw.rounded_rectangle(
        [margin, margin, SS_SIZE - margin, SS_SIZE - margin],
        radius=360,
        fill=bg_plate_fill,
        outline=bg_plate_outline,
        width=14
    )

    # 2. Multi-layered Ambient Glow
    glow = Image.new("RGBA", (SS_SIZE, SS_SIZE), (0, 0, 0, 0))
    draw_glow = ImageDraw.Draw(glow)
    
    for r, col, alpha in glow_colors:
        draw_glow.ellipse(
            [SS_SIZE//2 - r, SS_SIZE//2 - 50 - r, SS_SIZE//2 + r, SS_SIZE//2 - 50 + r],
            fill=(col[0], col[1], col[2], alpha)
        )
    glow = glow.filter(ImageFilter.GaussianBlur(36))
    img.alpha_composite(glow)

    # 3. Faceted 4-Point Prismatic Diamond Star
    cx, cy = SS_SIZE//2, SS_SIZE//2 - 50
    span_v = 440
    span_h = 330
    inner = 65

    # Top, Right, Bottom, Left Facets
    facets = [
        ([(cx, cy - span_v), (cx + inner, cy), (cx, cy)], facet_palette["top_right"]),
        ([(cx, cy - span_v), (cx - inner, cy), (cx, cy)], facet_palette["top_left"]),
        ([(cx + span_h, cy), (cx, cy + inner), (cx, cy)], facet_palette["right_bottom"]),
        ([(cx + span_h, cy), (cx, cy - inner), (cx, cy)], facet_palette["right_top"]),
        ([(cx, cy + span_v), (cx + inner, cy), (cx, cy)], facet_palette["bottom_right"]),
        ([(cx, cy + span_v), (cx - inner, cy), (cx, cy)], facet_palette["bottom_left"]),
        ([(cx - span_h, cy), (cx, cy + inner), (cx, cy)], facet_palette["left_bottom"]),
        ([(cx - span_h, cy), (cx, cy - inner), (cx, cy)], facet_palette["left_top"]),
    ]
    for pts, col in facets:
        draw.polygon(pts, fill=col)

    # 4. Premium Monogram Pill Badge
    font = find_system_font(52)
    pill_w, pill_h = 600, 96
    px0 = SS_SIZE//2 - pill_w//2
    py0 = SS_SIZE - 320
    
    draw.rounded_rectangle(
        [px0, py0, px0 + pill_w, py0 + pill_h],
        radius=48,
        fill=badge_bg,
        outline=badge_outline,
        width=7
    )
    
    bbox = font.getbbox(badge_text)
    tw = bbox[2] - bbox[0]
    draw.text((SS_SIZE//2 - tw//2, py0 + 20), badge_text, fill=badge_text_col, font=font)

    return img.resize((SIZE, SIZE), Image.Resampling.LANCZOS)

# ==============================================================================
# 0. OFFICIAL FREE VERSION (Your Selected Icon)
# ==============================================================================
def make_free_icon():
    return draw_prismatic_base(
        bg_plate_fill=(13, 13, 18, 255),
        bg_plate_outline=(42, 42, 50, 255),
        glow_colors=[
            (400, (139, 92, 246), 75),
            (220, (56, 189, 248), 90)
        ],
        facet_palette={
            "top_right": (255, 255, 255, 255),
            "top_left": (220, 230, 250, 255),
            "right_bottom": (139, 92, 246, 255),
            "right_top": (196, 181, 253, 255),
            "bottom_right": (56, 189, 248, 255),
            "bottom_left": (14, 165, 233, 255),
            "left_bottom": (168, 85, 247, 255),
            "left_top": (216, 180, 254, 255),
        },
        badge_bg=(24, 24, 32, 255),
        badge_outline=(50, 50, 65, 255),
        badge_text_col=(255, 255, 255, 255),
        badge_text="KALLISTO • AI"
    )

# ==============================================================================
# PRO 1: 24K Obsidian Gold (Flagship Luxury)
# ==============================================================================
def make_pro_gold():
    return draw_prismatic_base(
        bg_plate_fill=(10, 9, 7, 255),
        bg_plate_outline=(70, 52, 18, 255),
        glow_colors=[
            (420, (245, 158, 11), 85),
            (240, (254, 240, 138), 100)
        ],
        facet_palette={
            "top_right": (255, 252, 235, 255), # Radiant White Gold
            "top_left": (254, 240, 138, 255),  # Pure Light Gold
            "right_bottom": (217, 119, 6, 255), # Deep Rich Amber
            "right_top": (251, 191, 36, 255),  # Vibrant Warm Gold
            "bottom_right": (245, 158, 11, 255),# Intense Honey Gold
            "bottom_left": (180, 83, 9, 255),  # Burnished Bronze Gold
            "left_bottom": (217, 119, 6, 255), # Deep Rich Amber
            "left_top": (253, 224, 71, 255),  # Champagne Gold
        },
        badge_bg=(22, 18, 10, 255),
        badge_outline=(180, 120, 20, 255),
        badge_text_col=(255, 220, 100, 255),
        badge_text="KALLISTO • PRO"
    )

# ==============================================================================
# PRO 2: Holographic Platinum & Ice Diamond
# ==============================================================================
def make_pro_platinum():
    return draw_prismatic_base(
        bg_plate_fill=(12, 14, 18, 255),
        bg_plate_outline=(70, 80, 100, 255),
        glow_colors=[
            (420, (148, 163, 184), 80),
            (240, (186, 230, 253), 110)
        ],
        facet_palette={
            "top_right": (255, 255, 255, 255),
            "top_left": (226, 232, 240, 255),
            "right_bottom": (148, 163, 184, 255),
            "right_top": (203, 213, 225, 255),
            "bottom_right": (186, 230, 253, 255),
            "bottom_left": (125, 211, 252, 255),
            "left_bottom": (148, 163, 184, 255),
            "left_top": (241, 245, 249, 255),
        },
        badge_bg=(20, 24, 32, 255),
        badge_outline=(100, 116, 139, 255),
        badge_text_col=(255, 255, 255, 255),
        badge_text="KALLISTO • PRO"
    )

# ==============================================================================
# PRO 3: Royal Amethyst & Champagne Gold
# ==============================================================================
def make_pro_amethyst_gold():
    return draw_prismatic_base(
        bg_plate_fill=(14, 10, 20, 255),
        bg_plate_outline=(75, 35, 100, 255),
        glow_colors=[
            (420, (168, 85, 247), 90),
            (240, (251, 191, 36), 85)
        ],
        facet_palette={
            "top_right": (255, 250, 220, 255), # Champagne Gold Peak
            "top_left": (251, 191, 36, 255),  # Warm Gold
            "right_bottom": (126, 34, 206, 255),# Royal Purple
            "right_top": (192, 132, 252, 255), # Lilac Quartz
            "bottom_right": (217, 119, 6, 255), # Gold Ember
            "bottom_left": (147, 51, 234, 255), # Vivid Amethyst
            "left_bottom": (107, 33, 168, 255),# Deep Violet
            "left_top": (233, 213, 255, 255),  # Soft Iris
        },
        badge_bg=(26, 16, 36, 255),
        badge_outline=(147, 51, 234, 255),
        badge_text_col=(254, 240, 138, 255),
        badge_text="KALLISTO • PRO"
    )

# ==============================================================================
# PRO 4: Quantum Emerald & Cyber Onyx
# ==============================================================================
def make_pro_emerald():
    return draw_prismatic_base(
        bg_plate_fill=(8, 16, 13, 255),
        bg_plate_outline=(20, 65, 45, 255),
        glow_colors=[
            (420, (16, 185, 129), 90),
            (240, (52, 211, 153), 105)
        ],
        facet_palette={
            "top_right": (240, 253, 244, 255),
            "top_left": (167, 243, 208, 255),
            "right_bottom": (5, 150, 105, 255),
            "right_top": (52, 211, 153, 255),
            "bottom_right": (13, 148, 136, 255),
            "bottom_left": (15, 118, 110, 255),
            "left_bottom": (4, 120, 87, 255),
            "left_top": (110, 231, 183, 255),
        },
        badge_bg=(12, 28, 22, 255),
        badge_outline=(16, 185, 129, 255),
        badge_text_col=(167, 243, 208, 255),
        badge_text="KALLISTO • PRO"
    )

if __name__ == "__main__":
    print("[*] Generating the official Kallisto Core Free & PRO icon suite...")
    
    make_free_icon().save(os.path.join(OUTPUT_DIR, "kallisto_icon_free.png"))
    print("  -> Saved: icon_options/kallisto_icon_free.png (Official Free Version)")

    make_pro_gold().save(os.path.join(OUTPUT_DIR, "kallisto_icon_pro_gold.png"))
    print("  -> Saved: icon_options/kallisto_icon_pro_gold.png (PRO 1: 24K Obsidian Gold)")

    make_pro_platinum().save(os.path.join(OUTPUT_DIR, "kallisto_icon_pro_platinum.png"))
    print("  -> Saved: icon_options/kallisto_icon_pro_platinum.png (PRO 2: Holographic Platinum)")

    make_pro_amethyst_gold().save(os.path.join(OUTPUT_DIR, "kallisto_icon_pro_amethyst_gold.png"))
    print("  -> Saved: icon_options/kallisto_icon_pro_amethyst_gold.png (PRO 3: Royal Amethyst & Gold)")

    make_pro_emerald().save(os.path.join(OUTPUT_DIR, "kallisto_icon_pro_emerald.png"))
    print("  -> Saved: icon_options/kallisto_icon_pro_emerald.png (PRO 4: Quantum Emerald)")

    print("\n[+] All Free & PRO icon variations generated in 'icon_options/' folder!")
