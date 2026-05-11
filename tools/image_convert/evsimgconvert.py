#!/usr/bin/env python3
'''
 * Copyright 2025 Everysight LTD
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
'''


import sys
if sys.version_info < (3, 0):
    print("Sorry, requires Python 3.x or newer")
    exit(-1)


__version_info__ = ('2', '0', '0')
__version__ = '.'.join(__version_info__)

try:
    from PIL import __version__ as PIL_VERSION
    from PIL import Image
    from PIL.PngImagePlugin import PngImageFile, PngInfo
except ImportError:
    print("Please install missing modules: 'pip3 install Pillow'")
    exit(-1)


pil_old = True
try:
    pil_min_version = [8,1,0]
    pil_cur_version = [int(x) for x in PIL_VERSION.split('.')]
    pil_old = pil_min_version[0] > pil_cur_version[0] or (pil_min_version[0] == pil_cur_version[0] and pil_min_version[1] > pil_cur_version[1])
except:
    pass
if pil_old:
    print("You have an unsupported Pillow version {}".format(PIL_VERSION))
    print("You need to have Pillow version '8.1.0' or greater, please upgrade with 'pip3 install Pillow -U'")
    exit(-1)

#############################################################################################
import os
import argparse
import subprocess as sp
import tempfile
import shutil
import logging

MIN_OPTIPNG_PCT = 0.05
MAX_DATA_SIZE = (48*1024 - 16)

#############################################################################################

class ImageResult:
    fn: str
    size: int
    def __init__(self, fn: str):
        self.fn = fn
        self.size = os.stat(fn).st_size


class Message:
    def __init__(self, fmt, args):
        self.fmt = fmt
        self.args = args

    def __str__(self):
        return self.fmt.format(*self.args)


class StyleAdapter(logging.LoggerAdapter):
    def __init__(self, logger, extra=None):
        super(StyleAdapter, self).__init__(logger, extra or {})

    def log(self, level, msg, *args, **kwargs):
        if self.isEnabledFor(level):
            msg, kwargs = self.process(msg, kwargs)
            self.logger._log(level, Message(msg, args), (), **kwargs)


logger = StyleAdapter(logging.getLogger("evsimgconvert"))

#############################################################################################
# https://en.wikipedia.org/wiki/Ordered_dithering
dither565_r = [
    1, 7, 3, 5, 0, 8, 2, 6,
    7, 1, 5, 3, 8, 0, 6, 2,
    3, 5, 0, 8, 2, 6, 1, 7,
    5, 3, 8, 0, 6, 2, 7, 1,
    0, 8, 2, 6, 1, 7, 3, 5,
    8, 0, 6, 2, 7, 1, 5, 3,
    2, 6, 1, 7, 3, 5, 0, 8,
    6, 2, 7, 1, 5, 3, 8, 0
]

dither565_g = [
    1, 3, 2, 2, 3, 1, 2, 2,
    2, 2, 0, 4, 2, 2, 4, 0,
    3, 1, 2, 2, 1, 3, 2, 2,
    2, 2, 4, 0, 2, 2, 0, 4,
    1, 3, 2, 2, 3, 1, 2, 2,
    2, 2, 0, 4, 2, 2, 4, 0,
    3, 1, 2, 2, 1, 3, 2, 2,
    2, 2, 4, 0, 2, 2, 0, 4
]

dither565_b = [
    5, 3, 8, 0, 6, 2, 7, 1,
    3, 5, 0, 8, 2, 6, 1, 7,
    8, 0, 6, 2, 7, 1, 5, 3,
    0, 8, 2, 6, 1, 7, 3, 5,
    6, 2, 7, 1, 5, 3, 8, 0,
    2, 6, 1, 7, 3, 5, 0, 8,
    7, 1, 5, 3, 8, 0, 6, 2,
    1, 7, 3, 5, 0, 8, 2, 6
]

#############################################################################################
M332_color3bit = [
    0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
    0x00, 0x00, 0x00, 0x00, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01,
    0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01,
    0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x02, 0x02, 0x02, 0x02, 0x02,
    0x02, 0x02, 0x02, 0x02, 0x02, 0x02, 0x02, 0x02, 0x02, 0x02, 0x02, 0x02, 0x02, 0x02, 0x02,
    0x02, 0x02, 0x02, 0x02, 0x02, 0x02, 0x02, 0x02, 0x02, 0x02, 0x02, 0x02, 0x02, 0x02, 0x02,
    0x02, 0x02, 0x03, 0x03, 0x03, 0x03, 0x03, 0x03, 0x03, 0x03, 0x03, 0x03, 0x03, 0x03, 0x03,
    0x03, 0x03, 0x03, 0x03, 0x03, 0x03, 0x03, 0x03, 0x03, 0x03, 0x03, 0x03, 0x03, 0x03, 0x03,
    0x03, 0x03, 0x03, 0x03, 0x03, 0x03, 0x03, 0x03, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04,
    0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04,
    0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x05,
    0x05, 0x05, 0x05, 0x05, 0x05, 0x05, 0x05, 0x05, 0x05, 0x05, 0x05, 0x05, 0x05, 0x05, 0x05,
    0x05, 0x05, 0x05, 0x05, 0x05, 0x05, 0x05, 0x05, 0x05, 0x05, 0x05, 0x05, 0x05, 0x05, 0x05,
    0x05, 0x05, 0x05, 0x05, 0x05, 0x05, 0x06, 0x06, 0x06, 0x06, 0x06, 0x06, 0x06, 0x06, 0x06,
    0x06, 0x06, 0x06, 0x06, 0x06, 0x06, 0x06, 0x06, 0x06, 0x06, 0x06, 0x06, 0x06, 0x06, 0x06,
    0x06, 0x06, 0x06, 0x06, 0x06, 0x06, 0x06, 0x06, 0x06, 0x06, 0x06, 0x06, 0x07, 0x07, 0x07,
    0x07, 0x07, 0x07, 0x07, 0x07, 0x07, 0x07, 0x07, 0x07, 0x07, 0x07, 0x07, 0x07, 0x07, 0x07, 0x07
]

# b = (b + 42) / 85;
M332_color2bit = [
    0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
    0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
    0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x01, 0x01,
    0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01,
    0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01,
    0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01,
    0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01,
    0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01,
    0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x02, 0x02, 0x02, 0x02, 0x02, 0x02, 0x02,
    0x02, 0x02, 0x02, 0x02, 0x02, 0x02, 0x02, 0x02, 0x02, 0x02, 0x02, 0x02, 0x02, 0x02, 0x02,
    0x02, 0x02, 0x02, 0x02, 0x02, 0x02, 0x02, 0x02, 0x02, 0x02, 0x02, 0x02, 0x02, 0x02, 0x02,
    0x02, 0x02, 0x02, 0x02, 0x02, 0x02, 0x02, 0x02, 0x02, 0x02, 0x02, 0x02, 0x02, 0x02, 0x02,
    0x02, 0x02, 0x02, 0x02, 0x02, 0x02, 0x02, 0x02, 0x02, 0x02, 0x02, 0x02, 0x02, 0x02, 0x02,
    0x02, 0x02, 0x02, 0x02, 0x02, 0x02, 0x02, 0x02, 0x02, 0x02, 0x02, 0x02, 0x02, 0x02, 0x02,
    0x02, 0x02, 0x02, 0x03, 0x03, 0x03, 0x03, 0x03, 0x03, 0x03, 0x03, 0x03, 0x03, 0x03, 0x03,
    0x03, 0x03, 0x03, 0x03, 0x03, 0x03, 0x03, 0x03, 0x03, 0x03, 0x03, 0x03, 0x03, 0x03, 0x03,
    0x03, 0x03, 0x03, 0x03, 0x03, 0x03, 0x03, 0x03, 0x03, 0x03, 0x03, 0x03, 0x03, 0x03, 0x03, 0x03
]

# b = x * 85;
M332_2bitColor = [0x00, 0x55, 0xaa, 0xff]

# g = (x & 7) * 255 / 7;
M332_3bitColor = [0x00, 0x24, 0x48, 0x6d, 0x91, 0xb6, 0xda, 0xff]

#############################################################################################
def rgb2index_RGB332(r, g, b):
    return M332_color2bit[b] | (M332_color3bit[g] << 2) | (M332_color3bit[r] << 5)

def index2color_RGB332(index):
    b = M332_2bitColor[(index & 3)]
    g = M332_3bitColor[(index >> 2) & 0x7]
    r = M332_3bitColor[(index >> 5) & 0x7]
    return (r, g, b, 255)

def rgb2index_RGB565(r, g, b):
    b = int((31 * b + 128) / 255)
    g = int((63 * g + 128) / 255)
    r = int((31 * r + 128) / 255)
    c = (b | (g << 5) | (r << 11))
    return ((c >> 8) & 0xFF) | ((c & 0xFF) << 8)

def index2color_RGB565(rgb):
    rgb = ((rgb >> 8) & 0xFF) | ((rgb & 0xFF) << 8)
    b = ((255 * (rgb & 0x1F)) + 16) / 31
    g = (255 * ((rgb >> 5) & 0x3F) + 32) / 63
    r = ((255 * ((rgb >> 11) & 0x1F)) + 16) / 31
    return (r, g, b, 255)

def rgb2index_RGBA4444(r, g, b, a):
    b = int((15 * b + 128) / 255)
    g = int((15 * g + 128) / 255)
    r = int((15 * r + 128) / 255)
    a = int((15 * a + 128) / 255)
    c = a | (b << 4) | (g << 8) | (r << 12)
    return ((c >> 8) & 0xFF) | ((c & 0xFF) << 8)

def index2color_RGBA4444(rgb):
    rgb = ((rgb >> 8) & 0xFF) | ((rgb & 0xFF) << 8)
    a = int((((rgb) & 0x0F)) * 255 / 15)
    b = int((((rgb >> 4) & 0x0F)) * 255 / 15)
    g = int((((rgb >> 8) & 0x0F)) * 255 / 15)
    r = int((((rgb >> 12) & 0x0F)) * 255 / 15)
    return (r, g, b, a)


#############################################################################################
def is_eeps_chunk_present(img: PngImageFile):
    for chunk in img.private_chunks:
        print("TAG={}".format(chunk[0]))
        if chunk[0] == b'edXX' or chunk[0] == b'edDX' or chunk[0] == b'edDA':
            return chunk[0]

    return None

#############################################################################################
def which(program):
    import os

    def is_exe(fpath):
        return os.path.isfile(fpath) and os.access(fpath, os.X_OK)

    fpath, fname = os.path.split(program)
    if fpath:
        if is_exe(program):
            return program
    else:
        for path in os.environ["PATH"].split(os.pathsep):
            exe_file = os.path.join(path, program)
            if is_exe(exe_file):
                return exe_file

    return None

#############################################################################################
def get_image_magick_convert_tool(tool):
    cmd = which(tool)
    if cmd is None:
        return None
    result = sp.run([cmd, "--version"], stdout=sp.PIPE)
    if result.returncode != 0:
        return None
    output = result.stdout.decode("utf-8")
    if "ImageMagick" in output:
        return cmd
    return None

#############################################################################################
def get_optipng_tool(tool):
    cmd = which(tool)
    if cmd is None:
        return None
    result = sp.run([cmd, "--version"], stdout=sp.PIPE)
    if result.returncode != 0:
        return None
    output = result.stdout.decode("utf-8")
    if "OptiPNG" in output:
        return cmd
    return None

#############################################################################################
def convert(fn, out_fn, keep_alpha, bpp, dither):
    img = Image.open(fn)
    if type(img) == PngImageFile and is_eeps_chunk_present(img) != None:
        logger.warning("This image already appears to be optimized.")
        return None

    has_alpha = 'A' in img.mode
    if not has_alpha and keep_alpha:
        keep_alpha = False

    if bpp == 8 and keep_alpha:
        bpp = 16

    if img.width * img.height * bpp / 8 > 512*1024:
        logger.warning("image too large {}x{}", img.width, img.height)
        return None

    logger.debug("Image mode: {}, keep_alpha:{}", img.mode, keep_alpha)
    if img.mode != "RGB" and img.mode != "RGBA":
        img = img.convert("RGBA")

    logger.debug("Image mode={} keep_alpha={} has_alpha={}", img.mode, keep_alpha, has_alpha)
    data = img.getdata()
    newData = [0]*(img.width*img.height)
    for i, el in enumerate(data):
        r = el[0]
        g = el[1]
        b = el[2]
        a = 255
        if has_alpha:
            a = el[3]
            if not keep_alpha:
                f = a/255.0
                r = int(r*f + 0.5)
                g = int(g*f + 0.5)
                b = int(b*f + 0.5)

        if dither:
            idx = (int(i % img.width) & 7) + (int(i / img.height) & 7) * 8
            if bpp == 8:
                r += dither565_r[idx] * 4
                g += dither565_g[idx] * 8
                b += dither565_b[idx] * 4
                a += dither565_b[idx] * 4
            else:
                if keep_alpha:
                    r += dither565_r[idx] * 2
                    g += dither565_g[idx] * 4
                    b += dither565_b[idx] * 2
                    a += dither565_b[idx] * 2
                    if a > 255: a = 255
                else:
                    r += dither565_r[idx]
                    g += dither565_g[idx]
                    b += dither565_b[idx]
                
            if r > 255: r = 255
            if g > 255: g = 255
            if b > 255: b = 255

        if bpp == 8:
            newData[i] = rgb2index_RGB332(r, g, b)
        else:
            if keep_alpha:
                newData[i] = rgb2index_RGBA4444(r, g, b, a)
            else:
                newData[i] = rgb2index_RGB565(r, g, b)

    info = PngInfo()
    if bpp == 8:
        newMode = 'L'   # RGB332
        info.add(b'edXX', b'')
    else:
        if keep_alpha:  
            newMode = 'I'  # RGBA4444
            info.add(b'edDA', b'')
        else:
            newMode = 'I'  # RGB565
            info.add(b'edDX', b'')

    newImg = Image.new(newMode, (img.width, img.height))
    newImg.putdata(newData)
    newImg.save(out_fn, pnginfo=info, optimize=True)
    return True

#############################################################################################
def convert_to_jpeg(fn, out_fn):
    img = Image.open(fn)
    if img.mode in ('RGBA', 'LA') or (img.mode == 'P' and 'transparency' in img.info):
        # Convert to RGBA if it's not already
        img = img.convert('RGBA')
        
        # Create a new image with black background
        background = Image.new('RGB', img.size, (0, 0, 0))
        
        # Paste the image on the background using alpha channel as mask
        background.paste(img, mask=img.split()[3])  # 3 is the alpha channel
        img = background
    else:
        # If there's no alpha channel, just convert to RGB
        img = img.convert('RGB')
    
    quality = 97
    while quality > 55:
        img.save(out_fn, 'JPEG', optimize=True, quality = quality, progressive=False, subsampling='4:2:0')
        size = os.stat(out_fn).st_size
        logger.info(f"convert_to_jpeg: quality: {quality} => size:{size}")
        if (size < MAX_DATA_SIZE):
            return True
        quality -= 3
    
    os.unlink(out_fn)
    return False


#############################################################################################
def view(fn):
    img = Image.open(fn)
    decodeEvsType = type(img) == PngImageFile and is_eeps_chunk_present(img)
    if decodeEvsType == None:
        logger.error('Error: Mode={} TAG={}', img.mode, decodeEvsType)
        return

    if img.mode == 'P':
        img = img.convert('L')

    data = img.getdata()
    newData = [0]*(img.width*img.height*4)
    if decodeEvsType == b'edXX':
        for i, pix in enumerate(data):
            newData[i*4:i*4+4] = index2color_RGB332(pix)
    elif decodeEvsType == b'edDX':
        for i, pix in enumerate(data):
            newData[i*4:i*4+4] = index2color_RGB565(pix)
    elif decodeEvsType == b'edDA':
        for i, pix in enumerate(data):
            newData[i*4:i*4+4] = index2color_RGBA4444(pix)

    try:
        import matplotlib.pyplot as plt
        import numpy as np
        newData = np.array(newData, int)
        plt.axes().set_facecolor("black")
        plt.imshow(newData.reshape((img.height, img.width, 4)))
        plt.show()
    except ImportError:
        logger.error("Missing modules: matplotlib, numpy, please install with: 'pip3 install numpy matplotlib'")


#############################################################################################
def convert_to_png(convert, src, dst):
    result = sp.run([convert, src, dst], stdout=sp.PIPE)
    if result.returncode != 0:
        logger.warning("Unable to convert image:")
        logger.warning(result.stdout.decode("utf-8"))

    return result.returncode == 0

#############################################################################################
def optipng(optipng, image, out_image):
    with tempfile.TemporaryDirectory() as tmp:
        outfn = os.path.join(tmp, "out.png")
        result = sp.run([optipng, "-quiet", "-o7", "-f3", image, "-out", outfn], stdout=sp.PIPE)
        if result.returncode != 0:
            logger.warning("Unable to optimize image:")
            logger.warning(result.stdout.decode("utf-8"))
        in_size = os.stat(image).st_size
        out_size = os.stat(outfn).st_size
        pct = ((in_size - out_size) / in_size)
        if pct > MIN_OPTIPNG_PCT:
            shutil.copyfile(outfn, out_image)
        elif image != out_image:
            shutil.copyfile(image, out_image)
            
    return result.returncode == 0

#############################################################################################
if __name__ == "__main__":
    parser = argparse.ArgumentParser('Everysight Mav2 Image Converter / Viewer')
    parser.add_argument('-s', '--show', action='store_true', default=False, help='Show image')
    parser.add_argument('-c', '--convert', action='store_true', default=True, help='Convert image')
    parser.add_argument('image', type=str, help='Image file')
    parser.add_argument('-v', '--verbose',action='store_true', help='Be Verbose')
    parser.add_argument('-a', '--alpha', action='store_true', default=False, help='keep Alpha channel')
    parser.add_argument('-d', '--dither', action='store_true', default=False, help='Dither the image')
    parser.add_argument('--bpp', type=int, choices=[8, 16], default=8, help='Bit per pixel')
    parser.add_argument('-o', "--out", type=str, help="Output file name", default="", required=False)
    parser.add_argument("--imageMagick", type=str, help="Path to ImageMagick convert tool", default="convert", required=False)
    parser.add_argument("--optipng", type=str, help="Path to optipng tool", default="optipng", required=False)
    parser.add_argument('--version', action='version', version='%(prog)s {}'.format(__version__))
    args = parser.parse_args()

    log_level = logging.WARNING
    if args.verbose:
        log_level = logging.DEBUG

    FORMAT = '%(message)s'

    logging.basicConfig(format=FORMAT)
    logger.setLevel(log_level)

    img_convert_cmd = get_image_magick_convert_tool(args.imageMagick)
    optipng_cmd = get_optipng_tool(args.optipng)

    logger.debug("Using ImageMagick convert:{}", img_convert_cmd)

    if os.path.isfile(args.image) is False:
        logger.error("Not a file: {}", args.image)
        sys.exit(-1)

    isSVG = os.path.splitext(args.image)[-1].lower() == '.svg'
    format = Image.open(args.image).format
    logger.info(f"Input image format: [{format}]")
    if args.show:
        if isSVG:
            logger.warning("Unabel to preview SVG images")
            sys.exit(-1)
        view(args.image)
        sys.exit(0)

    if args.out == "":
        file_name = os.path.realpath(args.image)
        args.out = file_name[:file_name.index('.')] + "_opt.png"
    if not args.out.lower().endswith('.png'):
        logger.error("This tool converts images to PNG file format")
        logger.error("output filename must end in .png")
        sys.exit(-1)
        
    in_size = os.stat(args.image).st_size
    if isSVG:
        if img_convert_cmd is None:
            logger.error("Input image is an SVG file, and ImageMagic is not found in PATH")
            logger.error("If ImageMagick is installed, consider running with: --imageMagic PATH_TO_IMAGEMAGIC_CONVERT_BINARY")
            logger.error("i.e: --imageMagic /opt/imageMagic-6.19/bin/convert")
            logger.error("See: https://imagemagick.org/")
            sys.exit(1)
        if convert_to_png(img_convert_cmd, args.image, args.out) is False:
            sys.exit(1)
        args.image = args.out

    ret = convert(args.image, args.out, args.alpha, args.bpp, args.dither)
    if(ret is None):
        pass
    elif ret and optipng_cmd is not None:
        ret = optipng(optipng_cmd, args.out, args.out)
    else:
        logger.warning("No optipng tool found, output PNG is not optimal")
        logger.warning("If optipng is installed, consider running with: --optipng PATH_TO_OPTIPNG_BINARY")
        logger.warning("i.e: --optipng /opt/optipng/bin/optipng")
        logger.warning("See: http://optipng.sourceforge.net/")

    if ret is False or ret is None:
        sys.exit(1)

    results = []
    results.append(ImageResult(args.out))

    converted_jpeg = args.image + "_420.jpg"
    jpeg = convert_to_jpeg(args.image, converted_jpeg)
    if jpeg:
        results.append(ImageResult(converted_jpeg))

    results = sorted(results, key = lambda x: x.size)
    logger.info("All:")
    for r in results:
        logger.info(f"{r.fn} ({r.size})")
    
    best = results[0]
    for r in results:
        if r.fn != args.image and (best is not None and r.fn != best.fn):
            logger.debug(f"Unlink {r.fn}")
            os.unlink(r.fn)
    
    if best.size >= MAX_DATA_SIZE:
        logger.error("Image is too large! (limit is {})", MAX_DATA_SIZE)
        logger.error("Size after optimization {}bytes; try smaller image/lower bpp", best.size)
        sys.exit(1)

    ext = os.path.splitext(best.fn)[-1].lower()
    if ext != ".png":
        args.out = args.out.replace(".png",ext)
    shutil.move(best.fn, args.out)

    logger.warning("Output image: {}, Input={} Output={} Opt={:.2f}%", args.out, in_size, best.size, 100 - best.size/in_size*100)
    sys.exit(0)
