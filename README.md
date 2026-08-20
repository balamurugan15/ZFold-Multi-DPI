# ZFold Multi DPI

An Android utility for Samsung Galaxy Z Fold devices that applies separate Screen Zoom / DPI presets for the cover and inner displays.

![ZFold Multi DPI app screen](docs/images/zfold-multi-dpi.jpg)

## What it does

- Stores a DPI preset for each stable fold posture.
- Detects the hinge state locally.
- Applies the selected DPI through WindowManager after a one-time Shizuku-assisted secure-settings grant.
- Runs automatic switching as a visible foreground service and can restore it after reboot when enabled.

## Setup

Install the latest APK from [Releases](https://github.com/balamurugan15/ZFold-Multi-DPI/releases).

### One time

Install [Shizuku (thedjchi)](https://github.com/thedjchi/Shizuku/releases).

1. Enable **Wireless debugging** in Android Developer options.
2. Open Shizuku.
   1. Start Shizuku via **Wireless debugging**.
   2. Open **Authorized Applications** and enable **ZFold Multi DPI**.
3. Open **ZFold Multi DPI**.
   1. Set DPI values for the inner and outer screens.
   2. Tap **Grant secure-settings access**. This is needed only once.
   3. Tap **Apply Preset Settings**.
4. Tap **Start automatic DPI switching**.

Shizuku is only required for the initial permission grant. Automatic switching uses the retained `WRITE_SECURE_SETTINGS` permission afterward.

## Notes

- The monitor uses a foreground-service notification while enabled, as required by Android.
- Set battery usage to **Unrestricted** for best reliability on Samsung devices.
- Density changes can make the UI difficult to use; begin with values close to Samsung's defaults.
