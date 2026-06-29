import os
from PIL import Image, ImageEnhance, ImageFilter
import random
import numpy as np

def create_samples():
    base_image_path = "微信图片_20260617193902_20_18.jpg"
    out_dir = "验证证据/测试样本"
    
    if not os.path.exists(out_dir):
        os.makedirs(out_dir)
        
    try:
        img = Image.open(base_image_path)
    except Exception as e:
        print(f"Error opening {base_image_path}: {e}")
        return

    # 1. Clear (清晰)
    img.save(os.path.join(out_dir, "1_clear_original.jpg"))
    enhancer = ImageEnhance.Sharpness(img)
    enhancer.enhance(1.5).save(os.path.join(out_dir, "1_clear_sharpened.jpg"))
    
    # 2. Blur (模糊)
    img.filter(ImageFilter.GaussianBlur(radius=5)).save(os.path.join(out_dir, "2_blur_light.jpg"))
    img.filter(ImageFilter.GaussianBlur(radius=12)).save(os.path.join(out_dir, "2_blur_heavy.jpg"))
    
    # 3. Overexposed (过曝)
    enhancer = ImageEnhance.Brightness(img)
    enhancer.enhance(2.5).save(os.path.join(out_dir, "3_overexposed_light.jpg"))
    enhancer.enhance(4.0).save(os.path.join(out_dir, "3_overexposed_heavy.jpg"))
    
    # 4. Underexposed (欠曝)
    enhancer = ImageEnhance.Brightness(img)
    enhancer.enhance(0.4).save(os.path.join(out_dir, "4_underexposed_light.jpg"))
    enhancer.enhance(0.15).save(os.path.join(out_dir, "4_underexposed_heavy.jpg"))
    
    # 5. Noisy (噪点)
    img_arr = np.array(img)
    
    def add_noise(image_array, std):
        noise = np.random.normal(0, std, image_array.shape)
        noisy = np.clip(image_array + noise, 0, 255).astype(np.uint8)
        return Image.fromarray(noisy)
        
    add_noise(img_arr, 30).save(os.path.join(out_dir, "5_noisy_light.jpg"))
    add_noise(img_arr, 70).save(os.path.join(out_dir, "5_noisy_heavy.jpg"))
    
    # 6. Counter-example (反例 - 景深/虚化)
    # Simulate a macro shot where the center is clear and the rest is highly blurred
    w, h = img.size
    mask = Image.new('L', (w, h), 0)
    # Draw a white circle in the middle
    from PIL import ImageDraw
    draw = ImageDraw.Draw(mask)
    draw.ellipse((w*0.3, h*0.3, w*0.7, h*0.7), fill=255)
    # Blur the mask to make it a gradient
    mask = mask.filter(ImageFilter.GaussianBlur(radius=100))
    
    blurred_img = img.filter(ImageFilter.GaussianBlur(radius=20))
    bokeh_img = Image.composite(img, blurred_img, mask)
    bokeh_img.save(os.path.join(out_dir, "6_counter_example_bokeh.jpg"))
    
    print("Test samples generated successfully in 验证证据/测试样本/")

if __name__ == '__main__':
    create_samples()
