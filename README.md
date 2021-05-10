Android OLED Display
===============

A Java library for Android to drive the popular monochrome 128x64 pixel OLED display (SSD1306)
device using [usb-i2c-android](https://github.com/3cky/usb-i2c-android) interfacing library.

Ported to Java from [Adafruit's SSD1306 library for Arduino](https://github.com/adafruit/Adafruit_SSD1306) 
by Florian Frankenberger in [Pi-OLED](https://github.com/entrusc/Pi-OLED) project.

how to use?
============
Add jitpack.io repository to your root build.gradle:
```gradle
allprojects {
    repositories {
        ...
        maven { url 'https://jitpack.io' }
    }
}
```
Add library to dependencies:
```gradle
dependencies {
    implementation 'com.github.3cky:android-oled:1.1'
}
```

The hardware should be connected to the supported USB-I2C adapter using an USB OTG cable.

Then you can use the library like this:
```
    ...
    UsbI2cAdapter i2cAdapter = usbI2cManager.getAdapter(usbDevice)
    OLEDDisplay display = new OLEDDisplay(i2cAdapter);
    display.drawStringCentered("Hello World!", Font.FONT_5X8, 25, true);
    display.update();
```

Note that you always need to call update() after you changed the content of the display
to actually get the content displayed on the hardware.

Also note that constructor used in example assumes you have connected the display 
with i2c address 0x3C. If this is not the case, you can use the constructor with 
an explicit display address parameter.

how to build?
=============

The entire project is build with Gradle. Just clone the master branch, open the 
directory in Android Studio and hit build. Or if you prefer the command line:

    ./gradlew install

should build and install everything correctly.