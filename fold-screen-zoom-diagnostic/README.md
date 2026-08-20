# Fold Screen Zoom Diagnostic

This is a **personal, sideloaded Android diagnostic app** for a Samsung Galaxy Z Fold 7 on One UI 8.5. It intentionally does not automate Screen Zoom yet. Its purpose is to answer the key engineering question safely: after a one-time Shizuku/ADB grant, can a normal app apply the relevant density behavior without Shizuku running?

## What it does

- Observes the public hinge-angle sensor and reports folded, flex, and unfolded posture.
- Stores independent cover and inner density presets locally.
- Uses Shizuku only for a one-time, scoped `WRITE_SECURE_SETTINGS` grant to **its own package**.
- Lets the user test writing `display_density_forced` after Shizuku is stopped.
- Runs a visible foreground posture monitor, which is observation-only in this diagnostic build.

## What it does not do yet

- It does not automatically change Screen Zoom.
- It does not use Accessibility Service automation.
- It does not silently open Settings or change any developer option.
- It does not upload data or use Firebase.

## Device test plan

1. Open the project in Android Studio, let Gradle sync, then build and install the debug APK on the Fold 7.
2. Start Shizuku with USB or wireless debugging.
3. In the app, tap **Grant secure-settings access through Shizuku** and approve Shizuku's dialog.
4. Confirm the app reports `WRITE_SECURE_SETTINGS: granted`.
5. Stop Shizuku completely.
6. Set a clearly noticeable but safe test preset (for example, 20–30 DPI away from the current density), then tap **Test direct write using current posture**.
7. Record whether One UI applies the size change immediately, only after a fold/unfold, only after reboot, or never.

## Interpreting the result

- **Immediate change:** investigate a direct WindowManager invocation next; direct secure-setting writes may be enough on this firmware.
- **Applies only after fold/unfold or reboot:** the setting is stored but One UI is not observing it live. A direct WindowManager call is needed.
- **No change / exception:** an active Shizuku bridge or a separate Settings-UI fallback remains necessary.

## Safety

Density overrides can make the UI difficult to operate. Keep the first test close to stock. If the UI becomes unusable, reset Screen Zoom or Smallest Width manually in Settings. This app deliberately caps entered test densities to 160–1000 DPI.
