import os
from PIL import Image

image_dir = r"c:\Users\Savira\OneDrive\Desktop\speaknow-ai\speaknow\src\main\resources\static\images"
images_to_convert = ["asset1.png", "asset2.png", "challenge-bg.png", "freechat-bg.png", "voice-bg.png", "bg-main.png"]

for filename in images_to_convert:
    filepath = os.path.join(image_dir, filename)
    if os.path.exists(filepath):
        print(f"Processing {filename}...")
        img = Image.open(filepath)
        # Convert to RGB if needed (for PNG to WEBP/JPEG)
        if img.mode in ("RGBA", "P"):
            img = img.convert("RGB")
        
        # Resize if very large (e.g., width > 800)
        max_width = 800
        if img.width > max_width:
            ratio = max_width / img.width
            new_height = int(img.height * ratio)
            img = img.resize((max_width, new_height), Image.Resampling.LANCZOS)
            
        new_filename = filename.replace(".png", ".webp")
        new_filepath = os.path.join(image_dir, new_filename)
        
        # Save as WebP with 80% quality
        img.save(new_filepath, "WEBP", quality=80)
        print(f"Saved {new_filename}")
        
        # Optionally remove the old file to save space and avoid git lfs issues
        os.remove(filepath)
        print(f"Removed old {filename}")
