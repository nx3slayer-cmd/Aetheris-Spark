#!/usr/bin/env python3
import os
from PIL import Image

DENSITIES = {
    "mipmap-mdpi": 48,
    "mipmap-hdpi": 72,
    "mipmap-xhdpi": 96,
    "mipmap-xxhdpi": 144,
    "mipmap-xxxhdpi": 192
}

ICONS = {
    "ic_launcher_free": "icon_options/kallisto_icon_free.png",
    "ic_launcher_pro_platinum": "icon_options/kallisto_icon_pro_platinum.png",
    "ic_launcher_pro_gold": "icon_options/kallisto_icon_pro_gold.png",
    "ic_launcher_pro_amethyst": "icon_options/kallisto_icon_pro_amethyst_gold.png",
    "ic_launcher_pro_emerald": "icon_options/kallisto_icon_pro_emerald.png"
}

RES_BASE = "app/src/main/res"

print("[*] Exporting multi-density mipmap icons for dynamic launcher switcher...")

for icon_name, source_path in ICONS.items():
    if not os.path.exists(source_path):
        print(f"  [!] Missing {source_path}, skipping...")
        continue
    
    src_img = Image.open(source_path).convert("RGBA")
    
    for folder_name, target_size in DENSITIES.items():
        out_dir = os.path.join(RES_BASE, folder_name)
        os.makedirs(out_dir, exist_ok=True)
        
        resized = src_img.resize((target_size, target_size), Image.Resampling.LANCZOS)
        out_file = os.path.join(out_dir, f"{icon_name}.png")
        resized.save(out_file, "PNG")
        
    print(f"  -> Generated mipmaps for {icon_name}")

print("\n[+] All dynamic app icon densities successfully installed into 'app/src/main/res/'!")
