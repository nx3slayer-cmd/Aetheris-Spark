#!/usr/bin/env python3
import os
import math
from PIL import Image, ImageDraw, ImageFilter, ImageFont

OUTPUT_DIR = "icon_options"
os.makedirs(OUTPUT_DIR, exist_ok=True)
SIZE = 1024
SS_SIZE = 2048  # Super-sampled 2x for ultra-sharp edges

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

# ==============================================================================
# OPTION 1: Celestial Orbital Wave (Midnight Dark + Electric Cyan & Indigo)
# ==============================================================================
def render_opt1():
    img = Image.new("RGBA", (SS_SIZE, SS_SIZE), (9, 10, 15, 255))
    glow = Image.new("RGBA", (SS_SIZE, SS_SIZE), (0, 0, 0, 0))
    draw_glow = ImageDraw.Draw(glow)
    draw = ImageDraw.Draw(img)

    # Ambient radial nebula glow
    for r in range(700, 100, -25):
        alpha = int((1.0 - (r / 700.0)) * 45)
        draw_glow.ellipse(
            [SS_SIZE//2 - r, SS_SIZE//2 - r, SS_SIZE//2 + r, SS_SIZE//2 + r],
            fill=(99, 102, 241, alpha)
        )
    
    # Outer Orbital Ring with glow
    ring_bbox = [400, 400, SS_SIZE - 400, SS_SIZE - 400]
    draw_glow.ellipse(ring_bbox, outline=(56, 189, 248, 120), width=32)
    glow = glow.filter(ImageFilter.GaussianBlur(18))
    img.alpha_composite(glow)

    # Solid Crisp Orbital Ring
    draw.ellipse(ring_bbox, outline=(99, 102, 241, 255), width=20)

    # Centered Kokoro Acoustic Wave Bars
    bar_heights = [180, 320, 520, 720, 520, 320, 180]
    spacing = 80
    start_x = SS_SIZE//2 - (len(bar_heights) // 2) * spacing
    for i, h in enumerate(bar_heights):
        x = start_x + (i * spacing)
        y0 = SS_SIZE//2 - h//2
        y1 = SS_SIZE//2 + h//2
        draw.rounded_rectangle([x - 14, y0, x + 14, y1], radius=14, fill=(56, 189, 248, 255))

    # Satellite Pearl Node (Kallisto Moon)
    moon_x, moon_y = SS_SIZE - 450, 480
    draw.ellipse([moon_x - 36, moon_y - 36, moon_x + 36, moon_y + 36], fill=(255, 255, 255, 255))
    
    # Clean Badge Label
    font = find_system_font(54)
    text = "KALLISTO CORE"
    bbox = font.getbbox(text)
    tw = bbox[2] - bbox[0]
    draw.text((SS_SIZE//2 - tw//2, SS_SIZE - 280), text, fill=(241, 245, 249, 220), font=font)

    return img.resize((SIZE, SIZE), Image.Resampling.LANCZOS)

# ==============================================================================
# OPTION 2: The Sovereign Monolith (OLED True Black + Prismatic 4-Point Spark)
# ==============================================================================
def render_opt2():
    img = Image.new("RGBA", (SS_SIZE, SS_SIZE), (0, 0, 0, 255))
    draw = ImageDraw.Draw(img)

    # Rounded Bento Squircle Outer Plate
    margin = 120
    draw.rounded_rectangle(
        [margin, margin, SS_SIZE - margin, SS_SIZE - margin],
        radius=360,
        fill=(13, 13, 18, 255),
        outline=(42, 42, 50, 255),
        width=12
    )

    # Ambient Prismatic Glow
    glow = Image.new("RGBA", (SS_SIZE, SS_SIZE), (0, 0, 0, 0))
    draw_glow = ImageDraw.Draw(glow)
    draw_glow.ellipse([SS_SIZE//2 - 380, SS_SIZE//2 - 380, SS_SIZE//2 + 380, SS_SIZE//2 + 380], fill=(139, 92, 246, 75))
    draw_glow.ellipse([SS_SIZE//2 - 200, SS_SIZE//2 - 200, SS_SIZE//2 + 200, SS_SIZE//2 + 200], fill=(56, 189, 248, 90))
    glow = glow.filter(ImageFilter.GaussianBlur(32))
    img.alpha_composite(glow)

    # Draw Geometric 4-Point Prismatic Diamond Spark
    cx, cy = SS_SIZE//2, SS_SIZE//2 - 50
    span_v = 420
    span_h = 320
    inner = 60

    # Facet Polygons with Prismatic Gradient Shading
    facets = [
        ([(cx, cy - span_v), (cx + inner, cy), (cx, cy)], (255, 255, 255, 255)),
        ([(cx, cy - span_v), (cx - inner, cy), (cx, cy)], (220, 230, 250, 255)),
        ([(cx + span_h, cy), (cx, cy + inner), (cx, cy)], (139, 92, 246, 255)),
        ([(cx + span_h, cy), (cx, cy - inner), (cx, cy)], (196, 181, 253, 255)),
        ([(cx, cy + span_v), (cx + inner, cy), (cx, cy)], (56, 189, 248, 255)),
        ([(cx, cy + span_v), (cx - inner, cy), (cx, cy)], (14, 165, 233, 255)),
        ([(cx - span_h, cy), (cx, cy + inner), (cx, cy)], (168, 85, 247, 255)),
        ([(cx - span_h, cy), (cx, cy - inner), (cx, cy)], (216, 180, 254, 255)),
    ]
    for pts, col in facets:
        draw.polygon(pts, fill=col)

    # Subtle Monogram Typography Pill
    font = find_system_font(52)
    pill_w, pill_h = 560, 90
    px0 = SS_SIZE//2 - pill_w//2
    py0 = SS_SIZE - 320
    draw.rounded_rectangle([px0, py0, px0 + pill_w, py0 + pill_h], radius=45, fill=(24, 24, 32, 255), outline=(50, 50, 65, 255), width=6)
    
    text = "KALLISTO • AI"
    bbox = font.getbbox(text)
    tw = bbox[2] - bbox[0]
    draw.text((SS_SIZE//2 - tw//2, py0 + 18), text, fill=(255, 255, 255, 255), font=font)

    return img.resize((SIZE, SIZE), Image.Resampling.LANCZOS)

# ==============================================================================
# OPTION 3: Nord Forest Infinity Ribbon (Calm Sage & Slate Arctic Blue)
# ==============================================================================
def render_opt3():
    img = Image.new("RGBA", (SS_SIZE, SS_SIZE), (28, 33, 32, 255))
    draw = ImageDraw.Draw(img)

    # Subtle geometric grid backdrop
    for i in range(200, SS_SIZE, 160):
        draw.line([(i, 0), (i, SS_SIZE)], fill=(36, 44, 42, 255), width=4)
        draw.line([(0, i), (SS_SIZE, i)], fill=(36, 44, 42, 255), width=4)

    # Dual Interlocking Circular Loop (Sage Green + Arctic Blue)
    r = 300
    c1 = (SS_SIZE//2 - 200, SS_SIZE//2 - 40)
    c2 = (SS_SIZE//2 + 200, SS_SIZE//2 - 40)

    # Left Loop: Memory / Brain (Sage Green #A3BE8C)
    draw.ellipse([c1[0]-r, c1[1]-r, c1[0]+r, c1[1]+r], outline=(163, 190, 140, 255), width=40)
    
    # Right Loop: Voice / Sound (Arctic Cyan #88C0D0)
    draw.ellipse([c2[0]-r, c2[1]-r, c2[0]+r, c2[1]+r], outline=(136, 192, 208, 255), width=40)

    # Intersecting Wave Pulses
    for angle_deg in range(0, 360, 24):
        rad = math.radians(angle_deg)
        x1 = int(c1[0] + (r + 40) * math.cos(rad))
        y1 = int(c1[1] + (r + 40) * math.sin(rad))
        x2 = int(c1[0] + (r + 90) * math.cos(rad))
        y2 = int(c1[1] + (r + 90) * math.sin(rad))
        draw.line([(x1, y1), (x2, y2)], fill=(163, 190, 140, 160), width=8)

    # Central Core Connection Node
    draw.ellipse([SS_SIZE//2 - 32, (SS_SIZE//2 - 40) - 32, SS_SIZE//2 + 32, (SS_SIZE//2 - 40) + 32], fill=(236, 239, 244, 255))

    # Bottom Nordic Typography
    font = find_system_font(56)
    text = "K A L L I S T O"
    bbox = font.getbbox(text)
    tw = bbox[2] - bbox[0]
    draw.text((SS_SIZE//2 - tw//2, SS_SIZE - 280), text, fill=(236, 239, 244, 240), font=font)

    return img.resize((SIZE, SIZE), Image.Resampling.LANCZOS)

# ==============================================================================
# OPTION 4: Architectural "K" Monogram (Equalizer Stem + Vector Chevrons)
# ==============================================================================
def render_opt4():
    img = Image.new("RGBA", (SS_SIZE, SS_SIZE), (12, 14, 20, 255))
    draw = ImageDraw.Draw(img)

    # Bento background container
    draw.rounded_rectangle([100, 100, SS_SIZE - 100, SS_SIZE - 100], radius=280, fill=(18, 21, 31, 255), outline=(38, 43, 64, 255), width=10)

    # Left Stem of "K" — Vertical Acoustic Equalizer Bars
    stem_x = SS_SIZE//2 - 280
    y_starts = [450, 600, 750, 900, 1050, 1200, 1350]
    bar_widths = [160, 260, 360, 420, 360, 260, 160]
    
    for i, (y, bw) in enumerate(zip(y_starts, bar_widths)):
        draw.rounded_rectangle([stem_x, y, stem_x + bw, y + 90], radius=45, fill=(99, 102, 241, 255))

    # Right Diagonal Arms of "K" — Futuristic Cyber Wings
    # Upper Arm (Cyan)
    draw.polygon([
        (SS_SIZE//2 + 80, SS_SIZE//2 - 40),
        (SS_SIZE//2 + 360, 450),
        (SS_SIZE//2 + 460, 520),
        (SS_SIZE//2 + 180, SS_SIZE//2 + 30)
    ], fill=(56, 189, 248, 255))

    # Lower Arm (Indigo / Violet)
    draw.polygon([
        (SS_SIZE//2 + 100, SS_SIZE//2 + 10),
        (SS_SIZE//2 + 460, 1350),
        (SS_SIZE//2 + 360, 1420),
        (SS_SIZE//2 + 0, SS_SIZE//2 + 120)
    ], fill=(139, 92, 246, 255))

    # Live Neural Core Beacon Node
    beacon_x, beacon_y = SS_SIZE//2 - 280, 320
    draw.ellipse([beacon_x - 30, beacon_y - 30, beacon_x + 30, beacon_y + 30], fill=(16, 185, 129, 255))

    # Micro Monogram Subtitle
    font = find_system_font(48)
    text = "LOCAL • AI"
    bbox = font.getbbox(text)
    tw = bbox[2] - bbox[0]
    draw.text((SS_SIZE//2 - tw//2, SS_SIZE - 250), text, fill=(148, 163, 184, 255), font=font)

    return img.resize((SIZE, SIZE), Image.Resampling.LANCZOS)

# ==============================================================================
# OPTION 5: Multimodal Neural Spark Core (Concentric Audio Radar & Diamond)
# ==============================================================================
def render_opt5():
    img = Image.new("RGBA", (SS_SIZE, SS_SIZE), (10, 10, 14, 255))
    draw = ImageDraw.Draw(img)

    # Concentric Neural Radar Rings with varying opacity
    center = (SS_SIZE//2, SS_SIZE//2 - 60)
    radii = [180, 320, 460, 600]
    alphas = [220, 150, 90, 45]

    for r, a in zip(radii, alphas):
        draw.ellipse([center[0]-r, center[1]-r, center[0]+r, center[1]+r], outline=(99, 102, 241, a), width=10)

    # Acoustic wave ticks radiating across horizontal axis
    for x in range(center[0] - 640, center[0] + 680, 60):
        dist = abs(x - center[0])
        h = int(max(20, 360 - (dist * 0.45)))
        draw.line([(x, center[1] - h//2), (x, center[1] + h//2)], fill=(56, 189, 248, 180), width=10)

    # Central Prismatic White Core
    core_r = 75
    draw.ellipse([center[0]-core_r, center[1]-core_r, center[0]+core_r, center[1]+core_r], fill=(255, 255, 255, 255))

    # Inner Glow Diamond
    d_size = 140
    diamond = [
        (center[0], center[1] - d_size),
        (center[0] + d_size, center[1]),
        (center[0], center[1] + d_size),
        (center[0] - d_size, center[1])
    ]
    draw.polygon(diamond, outline=(139, 92, 246, 255), fill=(99, 102, 241, 110))

    # Bottom Pill Typography
    font = find_system_font(52)
    text = "KALLISTO CORE"
    bbox = font.getbbox(text)
    tw = bbox[2] - bbox[0]
    draw.text((SS_SIZE//2 - tw//2, SS_SIZE - 280), text, fill=(241, 245, 249, 240), font=font)

    return img.resize((SIZE, SIZE), Image.Resampling.LANCZOS)

# ==============================================================================
# Execute All Generators
# ==============================================================================
if __name__ == "__main__":
    print("[*] Generating 5 unique high-res app icon options for Kallisto Core...")
    
    opt1 = render_opt1()
    opt1.save(os.path.join(OUTPUT_DIR, "kallisto_icon_opt1.png"))
    print("  -> Saved: icon_options/kallisto_icon_opt1.png (Celestial Orbital Wave)")

    opt2 = render_opt2()
    opt2.save(os.path.join(OUTPUT_DIR, "kallisto_icon_opt2.png"))
    print("  -> Saved: icon_options/kallisto_icon_opt2.png (The Sovereign Monolith)")

    opt3 = render_opt3()
    opt3.save(os.path.join(OUTPUT_DIR, "kallisto_icon_opt3.png"))
    print("  -> Saved: icon_options/kallisto_icon_opt3.png (Nord Forest Infinity Ribbon)")

    opt4 = render_opt4()
    opt4.save(os.path.join(OUTPUT_DIR, "kallisto_icon_opt4.png"))
    print("  -> Saved: icon_options/kallisto_icon_opt4.png (Architectural 'K' Monogram)")

    opt5 = render_opt5()
    opt5.save(os.path.join(OUTPUT_DIR, "kallisto_icon_opt5.png"))
    print("  -> Saved: icon_options/kallisto_icon_opt5.png (Multimodal Neural Spark Core)")

    print("\n[+] All 5 icons generated successfully in folder: 'icon_options/'")
