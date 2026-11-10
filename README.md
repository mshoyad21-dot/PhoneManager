# Phone Manager

Java, MVVM (ViewModel + LiveData), Room, Material 3. Package: `com.jamildev.phonemanager`.
Min SDK 23, target SDK 35. UI in English and Bengali (`values/` and `values-bn/`).

## Open and run
1. Android Studio (Ladybug or newer): File > Open > this folder. Let Gradle sync (it creates the wrapper jar).
2. Run on a real phone (Android 6+). Emulators lack a front camera and biometrics.
3. If an older SecureVault build is installed, uninstall it first (different package, but old data is not needed).

## Screens
- **Home**: health score ring (0-100), Optimize button, Storage / Memory / App risks / App manager cards, quick tools.
- **Utilities**: grid of all tools, grouped (Cleanup & boost, Privacy, Security, Tools).

## What each tool really does
| Tool | Behaviour |
|---|---|
| Health score | storage 30% + memory 25% + battery 15% + app review 30%. Storage and memory score full marks while 30%+ is free. |
| Optimize / System boost | Asks Android to stop background processes of other apps (`killBackgroundProcesses`) and clears this app's cache. Android decides what stops; the result dialog shows what was actually freed. |
| Storage | Used / free / total, shortcut to largest apps, system storage settings, clears this app's cache. |
| App manager | Installed apps with size (needs Usage access); tap opens the system App info page (uninstall, clear cache there). |
| Risk check | Permission review of user-installed apps: SMS access, call-log access, accessibility access, device-admin, no install source. NOT a virus scanner. |
| Battery | Live level, status, health, temperature, voltage + shortcuts to system battery pages. |
| Network speed test | Downloads from `speed.cloudflare.com` for up to 10 s and shows average Mbps (uses data). |
| App Lock | PIN (created the first time you open it), fingerprint/face, random keypad, auto-lock delay, hidden apps, intruder selfie after 2 wrong PINs. Only App Lock screens ask for the PIN; the rest of the app does not. |

## Limits (Android rules)
- A normal app cannot clear other apps' cache or really "boost" the phone; Optimize is best-effort.
- "Hide apps" hides inside Phone Manager only; the system launcher still lists the app.
- App Lock needs Usage access + Display over other apps, and polls every 0.5 s, so a locked app can flash briefly.
- Intruder photos are stored in app-private storage, not yet encrypted.
- Uses a narrow `<queries>` launcher filter instead of `QUERY_ALL_PACKAGES` (Play Store restricts it).
- Not included: Private Safe, Kids mode, backup, ads/billing, spam call/SMS blocking.
