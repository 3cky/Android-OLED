/*
 * Copyright (c) 2016, Florian Frankenberger
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 * * Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 * * Neither the name of the copyright holder nor the names of its contributors
 *   may be used to endorse or promote products derived from this software without
 *   specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package com.github.ykc3.android.oled;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;

import com.github.ykc3.android.usbi2c.UsbI2cAdapter;
import com.github.ykc3.android.usbi2c.UsbI2cDevice;
import java.io.IOException;
import java.util.Arrays;

/**
 * An android driver for the 128x64 pixel OLED display (i2c bus).
 * The supported kind of display uses the SSD1306 driver chip and
 * is connected to the USB OTG I2C adapter.
 * <p>
 * Sample usage:
 * </p>
 * <pre>
 * OLEDDisplay display = new OLEDDisplay(i2cAdapter);
 * display.drawStringCentered("Hello World!", 25, true);
 * display.update();
 * Thread.sleep(10000); //sleep some time, because the display
 *                      //is automatically cleared the moment
 *                      //the application terminates
 * </pre>
 * <p>
 * This class is basically a rough port of Adafruit's BSD licensed
 * SSD1306 library (https://github.com/adafruit/Adafruit_SSD1306)
 * </p>
 *
 * @author Florian Frankenberger
 * @author Victor Antonovich
 */
public class OLEDDisplay {
    private static final int DEFAULT_DISPLAY_ADDRESS = 0x3C;

    private static final int DISPLAY_WIDTH = 128;
    private static final int DISPLAY_HEIGHT = 64;
    private static final int MAX_INDEX = (DISPLAY_HEIGHT / 8) * DISPLAY_WIDTH;

    private static final byte SSD1306_SETCONTRAST = (byte) 0x81;
    private static final byte SSD1306_DISPLAYALLON_RESUME = (byte) 0xA4;
    private static final byte SSD1306_DISPLAYALLON = (byte) 0xA5;
    private static final byte SSD1306_NORMALDISPLAY = (byte) 0xA6;
    private static final byte SSD1306_INVERTDISPLAY = (byte) 0xA7;
    private static final byte SSD1306_DISPLAYOFF = (byte) 0xAE;
    private static final byte SSD1306_DISPLAYON = (byte) 0xAF;

    private static final byte SSD1306_SETDISPLAYOFFSET = (byte) 0xD3;
    private static final byte SSD1306_SETCOMPINS = (byte) 0xDA;

    private static final byte SSD1306_SETVCOMDETECT = (byte) 0xDB;

    private static final byte SSD1306_SETDISPLAYCLOCKDIV = (byte) 0xD5;
    private static final byte SSD1306_SETPRECHARGE = (byte) 0xD9;

    private static final byte SSD1306_SETMULTIPLEX = (byte) 0xA8;

    private static final byte SSD1306_SETLOWCOLUMN = (byte) 0x00;
    private static final byte SSD1306_SETHIGHCOLUMN = (byte) 0x10;

    private static final byte SSD1306_SETSTARTLINE = (byte) 0x40;

    private static final byte SSD1306_MEMORYMODE = (byte) 0x20;
    private static final byte SSD1306_COLUMNADDR = (byte) 0x21;
    private static final byte SSD1306_PAGEADDR = (byte) 0x22;

    private static final byte SSD1306_COMSCANINC = (byte) 0xC0;
    private static final byte SSD1306_COMSCANDEC = (byte) 0xC8;

    private static final byte SSD1306_SEGREMAP = (byte) 0xA0;

    private static final byte SSD1306_CHARGEPUMP = (byte) 0x8D;

    private static final byte SSD1306_EXTERNALVCC = (byte) 0x1;
    private static final byte SSD1306_SWITCHCAPVCC = (byte) 0x2;


    private final UsbI2cDevice device;

    private final byte[] imageBuffer = new byte[(DISPLAY_WIDTH * DISPLAY_HEIGHT) / 8];

    private static final int MAX_TRANSFER_SIZE = 16;
    private final byte[] writeBuffer = new byte[MAX_TRANSFER_SIZE];

    /**
     * Creates an OLED display object with default display address
     *
     * @param i2cAdapter the i2c bus adapter
     * @throws IOException
     */
    public OLEDDisplay(UsbI2cAdapter i2cAdapter) throws IOException {
        this(i2cAdapter, DEFAULT_DISPLAY_ADDRESS);
    }

    /**
     * Constructor with all parameters
     *
     * @param i2cAdapter the i2c bus adapter
     * @param displayAddress the i2c bus address of the display
     * @throws IOException
     */
    public OLEDDisplay(UsbI2cAdapter i2cAdapter, int displayAddress) throws IOException {
        device = i2cAdapter.getDevice(displayAddress);
        clear();
        init();
    }

    public synchronized void clear() {
        Arrays.fill(imageBuffer, (byte) 0x00);
    }

    public int getWidth() {
        return DISPLAY_WIDTH;
    }

    public int getHeight() {
        return DISPLAY_HEIGHT;
    }

    private void writeCommand(byte command) throws IOException {
        device.writeRegByte(0x00, command);
    }

    private void init() throws IOException {
        writeCommand(SSD1306_DISPLAYOFF);                    // 0xAE
        writeCommand(SSD1306_SETDISPLAYCLOCKDIV);            // 0xD5
        writeCommand((byte) 0x80);                           // the suggested ratio 0x80
        writeCommand(SSD1306_SETMULTIPLEX);                  // 0xA8
        writeCommand((byte) 0x3F);
        writeCommand(SSD1306_SETDISPLAYOFFSET);              // 0xD3
        writeCommand((byte) 0x0);                            // no offset
        writeCommand((byte) (SSD1306_SETSTARTLINE | 0x0));   // line #0
        writeCommand(SSD1306_CHARGEPUMP);                    // 0x8D
        writeCommand((byte) 0x14);
        writeCommand(SSD1306_MEMORYMODE);                    // 0x20
        writeCommand((byte) 0x00);                           // 0x0 act like ks0108
        writeCommand((byte) (SSD1306_SEGREMAP | 0x1));
        writeCommand(SSD1306_COMSCANDEC);
        writeCommand(SSD1306_SETCOMPINS);                    // 0xDA
        writeCommand((byte) 0x12);
        writeCommand(SSD1306_SETCONTRAST);                   // 0x81
        writeCommand((byte) 0xCF);
        writeCommand(SSD1306_SETPRECHARGE);                  // 0xd9
        writeCommand((byte) 0xF1);
        writeCommand(SSD1306_SETVCOMDETECT);                 // 0xDB
        writeCommand((byte) 0x40);
        writeCommand(SSD1306_DISPLAYALLON_RESUME);           // 0xA4
        writeCommand(SSD1306_NORMALDISPLAY);

        writeCommand(SSD1306_DISPLAYON);//--turn on oled panel
    }

    public synchronized void setPixel(int x, int y, boolean on) {
        final int pos = x + (y / 8) * DISPLAY_WIDTH;
        if (pos >= 0 && pos < MAX_INDEX) {
            if (on) {
                this.imageBuffer[pos] |= (1 << (y & 0x07));
            } else {
                this.imageBuffer[pos] &= ~(1 << (y & 0x07));
            }
        }
    }

    public synchronized void drawChar(char c, Font font, int x, int y, boolean on) {
        font.drawChar(this, c, x, y, on);
    }

    public synchronized void drawString(String string, Font font, int x, int y, boolean on) {
        int posX = x;
        int posY = y;
        for (char c : string.toCharArray()) {
            if (c == '\n') {
                posY += font.getOuterHeight();
                posX = x;
            } else {
                if (posX >= 0 && posX + font.getWidth() < this.getWidth()
                        && posY >= 0 && posY + font.getHeight() < this.getHeight()) {
                    drawChar(c, font, posX, posY, on);
                }
                posX += font.getOuterWidth();
            }
        }
    }

    public synchronized void drawStringCentered(String string, Font font, int y, boolean on) {
        final int strSizeX = string.length() * font.getOuterWidth();
        final int x = (this.getWidth() - strSizeX) / 2;
        drawString(string, font, x, y, on);
    }

    public synchronized void clearRect(int x, int y, int width, int height, boolean on) {
        for (int posX = x; posX < x + width; ++posX) {
            for (int posY = y; posY < y + height; ++posY) {
                setPixel(posX, posY, on);
            }
        }
    }

    /**
     * draws the given image over the current image buffer. The image
     * is automatically converted to a binary image (if it not already
     * is).
     * <p>
     * Note that the current buffer is not cleared before, so if you
     * want the image to completely overwrite the current display
     * content you need to call clear() before.
     * </p>
     *
     * @param image
     * @param x
     * @param y
     */
    public synchronized void drawImage(Bitmap image, int x, int y) {
        Bitmap image1bpp = Bitmap.createBitmap(image.getWidth(), image.getHeight(),
                Bitmap.Config.ARGB_8888);
        Canvas canvas1bpp = new Canvas(image1bpp);
        ColorMatrix cm1bpp = new ColorMatrix();
        cm1bpp.setSaturation(0);
        Paint paint1bpp = new Paint();
        paint1bpp.setColorFilter(new ColorMatrixColorFilter(cm1bpp));
        canvas1bpp.drawBitmap(image, 0, 0, paint1bpp);

        for (int posY = 0; posY < image1bpp.getHeight(); posY++) {
            for (int posX = 0; posX < image1bpp.getWidth(); posX++) {
                int pixelVal = image1bpp.getPixel(posX, posY);
                setPixel(x + posX, y + posY, (pixelVal & 0x80) > 0);
            }
        }
    }

    /**
     * Do full update of display with the current buffer content.
     * @throws IOException in case of I2C bus I/O error
     */
    public void update() throws IOException {
        update(0, 0, getWidth(), getHeight());
    }

    /**
     * Do partial update of display area with the current buffer content.
     * @param x buffer x coordinate of the update area
     * @param y buffer y coordinate of the update area
     * @param w width of the update area
     * @param h height of the update area
     * @throws IOException in case of I2C bus I/O error
     */
    public synchronized void update(int x, int y, int w, int h) throws IOException {
        if (x < 0 || x > getWidth() || y < 0 || y > getHeight() || w <= 0 || h <= 0
                || (x + w) > getWidth() || ((y + h) > getHeight())) {
            throw new IllegalArgumentException("Invalid update area: x=" +
                    x + ", y=" + y + ", w=" + w + ", h=" + h);
        }

        writeCommand(SSD1306_COLUMNADDR);
        int startColumn = x;
        int endColumn = x + w - 1;
        writeCommand((byte) startColumn);   // Column start address
        writeCommand((byte) endColumn); // Column end address

        int startPage = y / 8;
        int endPage = (y + h - 1) / 8;
        writeCommand(SSD1306_PAGEADDR);
        writeCommand((byte) startPage); // Page start address
        writeCommand((byte) endPage); // Page end address

        int bufferIndex = 0;
        for (int page = startPage; page <= endPage; page++) {
            for (int column = startColumn; column <= endColumn ; column++) {
                writeBuffer[bufferIndex++] = imageBuffer[page * DISPLAY_WIDTH + column];
                if (bufferIndex > MAX_TRANSFER_SIZE - 1) {
                    device.writeRegBuffer(0x40, writeBuffer, bufferIndex);
                    bufferIndex = 0;
                }
            }
        }
        if (bufferIndex > 0) {
            device.writeRegBuffer(0x40, writeBuffer, bufferIndex);
        }
    }
}
