#!/usr/bin/env python3

#########################################
import sys

if sys.version_info < (3, 5):
    print("Sorry, requires Python 3.5 or newer")
    exit(-1)

#########################################
import os
import argparse
# import numpy
import shutil
from PIL import Image
from functools import cmp_to_key

#########################################
SEP = "----------------------------"
IMAGE_WIDTH = int(399)
IMAGE_HEIGHT = int(400)

handled = set()
unhandled = set()


class bcolors:
    HEADER = '\033[95m'
    OKBLUE = '\033[94m'
    OKCYAN = '\033[96m'
    OKGREEN = '\033[92m'
    WARNING = '\033[93m'
    FAIL = '\033[91m'
    ENDC = '\033[0m'
    BOLD = '\033[1m'
    UNDERLINE = '\033[4m'


#########################################
def create_kt_file(path, name, namespace, kt_enum_block, assets_subfolder):
    if len(kt_enum_block) == 0:
        return
    
    class_name = (name[0].upper() + name[1:]).replace(' ', "_")
    enum_name = class_name + "Type"
    asset_name = name
    if(assets_subfolder is not None and assets_subfolder != ""):
        asset_name = assets_subfolder + "/" + name

    os.makedirs(path, exist_ok=True)
    kt_file_name = path + "/" + class_name + ".kt"
    print(f'{bcolors.OKGREEN}Kotlin file :\t{kt_file_name}{bcolors.ENDC}')
    file = open(kt_file_name, "w")
    file.write(f'package {namespace}\n\n')
    file.write(f'import com.everysight.mav2.sdk.resources.M2CacheScope\n')
    file.write(f'import com.everysight.mav2.sdk.resources.M2ImageFile\n')
    file.write(f'import com.everysight.mav2.sdk.resources.M2ImageResource\n')
    file.write(f'import com.everysight.mav2.sdk.resources.M2SpriteFill\n')
    file.write(f'import com.everysight.mav2.sdk.uikit.data.M2TextureFit\n')
    file.write(f'import com.everysight.mav2.sdk.uikit.drawables.M2SpriteImage\n\n')
    file.write(f'class {class_name}(sprite: {enum_name}) : M2SpriteImage(getSpriteFillFor(sprite)) {{\n\n')
    file.write(f'    private val TAG = "{class_name}"\n\n')
    file.write(f'    enum class {enum_name}(val x: Int, val y: Int, val w: Int, val h: Int) {{\n')
    for i in range(len(kt_enum_block)):
        if i == len(kt_enum_block) - 1:
            file.write(f'        {kt_enum_block[i]}\n')
        else:
            file.write(f'        {kt_enum_block[i]},\n')
    file.write('    }\n\n')

    file.write(f'    fun setFill(sprite: {enum_name}): M2SpriteImage {{\n')
    file.write(f'        setFill(getSpriteFillFor(sprite), M2TextureFit.ShapeToImage)\n')
    file.write(f'        return this\n')
    file.write(f'    }}\n\n')

    file.write(f'    companion object{{\n')
    file.write(f'        val nameWithExtension: String = "{asset_name}.png"\n')
    file.write(f'        val spriteImageResource = M2ImageFile(nameWithExtension, M2CacheScope.CachedManualDelete, tag = nameWithExtension)\n')
    file.write(f'        fun getSpriteFillFor(sprite: {enum_name}) = M2SpriteFill(spriteImageResource, sprite.w, sprite.h, sprite.x, sprite.y)\n')
    file.write(f'    }}\n')
    
    # end class
    file.write('}\n')
    file.close()


#########################################
def add_line_to_kt(kt_enum_block, file_name, x, next_y, img_w, img_h):
    name = os.path.splitext(file_name)[0].replace('-', '_')

    kt_enum_block.append(f'{name}(' + str(int(x)) + ',' + str(int(next_y)) + ',' + str(int(img_w)) + ',' + str(int(img_h)) + ')')


#########################################
def create_output_files(input_folder, output_folder, output_name, namespace, prefix,assets_subfolder):
    out_png = output_folder + "/" + output_name + ".png"
    print(f"Creating files for {bcolors.OKCYAN}{out_png}{bcolors.ENDC}")

    output_image = Image.new('RGBA', (IMAGE_WIDTH, IMAGE_HEIGHT),(255, 0, 0, 0))

    kt_enum_block = []
    png_images = []
    print(f"Searching files in {input_folder}")
    for f in os.listdir(input_folder):
        f_lower = f.lower()
        if f_lower.endswith("png"):
            if prefix == "" or prefix is None:
                pass_check = True
            elif prefix in f_lower:
                pass_check = True
            else:
                pass_check = False

            if pass_check:
                full_path = os.path.join(input_folder, f)
                try:
                    with Image.open(full_path) as img:
                        img_copy = img.copy()
                        img_copy.filename = full_path  # preserve for sorting
                        png_images.append(img_copy)
                except Exception as e:
                    print(f"{bcolors.FAIL}Failed to open {f}: {e}{bcolors.ENDC}")
            else:
                unhandled.add(f_lower)

    print("Processing files...")
    png_images = sorted(png_images, key=lambda img: img.filename)
    png_images = sorted(png_images, key=lambda img: img.size[1])
    count = 0
    added = 0
    next_x = 0
    next_y = 0
    max_height = 0
    max_x = 0
    max_y = 0

    for img in png_images:
        filename = img.filename.split('/')[-1]
        img_w, img_h = img.size
        current_x = next_x
        next_x = next_x + img_w
        # new line check
        if next_x > IMAGE_WIDTH:
            current_x = 0
            next_x = img_w
            next_y = next_y + max_height
            max_height = 0

        max_height = max(max_height, img_h)
        if next_y + max_height > IMAGE_HEIGHT:
            print(f'\t{bcolors.WARNING}Skipping {filename} (out of space) {bcolors.ENDC}')
            unhandled.add(filename)
            continue

        offset = current_x, next_y
        max_x = max(max_x, current_x + img_w)
        max_y = max(max_y, next_y + img_h)
        print(f'\t[{added}] Adding {filename}-{img.size} at {offset} \t until ({offset[0] + img.size[0]},{offset[1] + img.size[1]})')
        added = added + 1
        output_image.paste(img, offset)
        # img_for_debugger = numpy.asarray(img)
        # output_image_for_debugger = numpy.asarray(output_image)
        add_line_to_kt(kt_enum_block,filename, current_x, next_y, img_w, img_h)

        handled.add(filename)
        count = count + 1

    print(SEP)
    if len(png_images) == 0:
        print(f'{bcolors.FAIL}No images were found!!{bcolors.ENDC}')
    else:
        create_kt_file(output_folder, output_name, namespace, kt_enum_block,assets_subfolder)
        box = (0, 0, max_x, max_y)
        cropped = output_image.crop(box)
        cropped.save(out_png)
        print(f'{bcolors.OKGREEN}Sprite Image :\t{out_png}{bcolors.ENDC}')
        print(f'{bcolors.OKGREEN}Sprite Image Size :\t{cropped.size}{bcolors.ENDC}')
        print(f"{bcolors.OKGREEN}DONE ({str(count)} files processed){bcolors.ENDC}")
        print(SEP)


#########################################
def main():
    parser = argparse.ArgumentParser(
        description='Batch folder convert - Process a folder of images and generate Kotlin sprite files.',
        formatter_class=argparse.ArgumentDefaultsHelpFormatter,
        epilog='Example usage:\n  python sprite_png.py -fin ./images -n mySprite\n'
    )

    folders_group = parser.add_argument_group('***** Folders Configuration *****')
    folders_group.add_argument('-fin',
                        '--input_folder',
                        type=str,
                        required=True,
                        help='Input folder of images to process (required)')
    folders_group.add_argument('-fout',
                        '--output_folder',
                        type=str,
                        default="./out",
                        help='Output folder for the generated files')
    folders_group.add_argument('-pf',
                        '--with_prefix',
                        type=str,
                        help='Filter only images starting with this prefix')
    
    # Generation options
    gen_group = parser.add_argument_group('***** Kotlin Generation Options *****')
    gen_group.add_argument('-n',
                        '--name',
                        type=str,
                        required=True,
                        help='Generated files base name (required)')
    gen_group.add_argument('-ns',
                        '--namespace',
                        type=str,
                        default="com.everysight",
                        help='Namespace for the generated Kotlin file')
    
    gen_group.add_argument('-sub',
                        '--assets_subfolder',
                        type=str,
                        help='Assets subfolder name for the generated sprite image (if any)')

    args = parser.parse_args()
    
    os.makedirs(os.path.dirname(args.output_folder), exist_ok=True)

    print(SEP)
    create_output_files(args.input_folder, args.output_folder, args.name, args.namespace, args.with_prefix,args.assets_subfolder)
    
    # move handled photos to a sub folder
    path_handled = args.output_folder + "/handled/"
    if not os.path.exists(os.path.dirname(path_handled)):
        os.makedirs(os.path.dirname(path_handled), exist_ok=True)
    for filename in handled:
        shutil.copy(os.path.join(args.input_folder, filename), os.path.join(path_handled, filename))

    # move skipped photos to a '.../out/fields_icons/'
    path_unhandled = args.output_folder + "/unhandled/"
    if not os.path.exists(os.path.dirname(path_unhandled)):
        os.makedirs(os.path.dirname(path_unhandled), exist_ok=True)
    for f in unhandled:
        shutil.copy(os.path.join(args.input_folder, f), os.path.join(path_unhandled, f))

    print(f"In order to convert the sprite file please use this command:")
    out_png = args.output_folder + "/" + args.name + ".png"
    # The converter ships next to this tool: tools/image_convert/evsimgconvert.py
    converter = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "image_convert", "evsimgconvert.py")
    print(f"{bcolors.OKBLUE}python3 {os.path.normpath(converter)} {out_png} -a -o {out_png.replace('.png','_conv.png')} {bcolors.ENDC}\n")

    print(f"{bcolors.OKCYAN}----------- DONE -----------{bcolors.ENDC}")


if __name__ == '__main__':
    main()
# Example:
# py sprite_png.py -fin /Users/eran/everysight/git/epsilon/phones/android/cycling/sprite_icons/handled -fout /Users/eran/Temp/1112 -n Eran -ns com.everysight.maverick2.tester.hud.controls -sub sprites