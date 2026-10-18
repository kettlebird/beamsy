# On-device checklist

Run these on your Android TV with an iPhone on the same network. Each item has an action and what you should see. Tick them off and note anything that fails.

## 0. Install

1. On the TV: Settings > Device Preferences > About, then tap "Build" seven times to enable developer mode. Then Developer options > turn on "USB debugging" (or "Network debugging" / "Wireless debugging").
2. On your computer, with the TV on the same network (find its IP in the TV's network settings):
   ```
   adb connect <tv-ip>:5555
   adb install -r app/build/outputs/apk/release/app-release.apk
   ```
   Accept the "Allow debugging" prompt on the TV the first time. Without an upload key your release APKs are unsigned and adb will refuse them, so use the debug APKs instead (`app/build/outputs/apk/debug/app-debug.apk` and `unlock/build/outputs/apk/debug/unlock-debug.apk`). Build both in the same run on the same machine (see the README): the unlock check compares signing certificates, and debug keys from different machines or fresh containers will not match.
3. Install the unlock app later, when the checklist says so:
   ```
   adb install -r unlock/build/outputs/apk/release/unlock-release.apk
   ```
   (or `unlock/build/outputs/apk/debug/unlock-debug.apk` for the debug route)
   Expected: `Success` for each install. The main app only recognises the unlock app when both are signed with the same key.

## 1. Basics

1. Open the TV launcher. Expected: the app is listed as "Screen Mirroring TV". Open it; it shows the steps screen.
2. On the iPhone, open Control Center > Screen Mirroring. Expected: the TV is listed by its device name.
3. Tap the TV to mirror, then play something with sound. Expected: the iPhone screen shows on the TV and you hear the sound from the TV.
4. While one iPhone is mirroring, try to mirror from a second iPhone. Expected: the second iPhone cannot take over, and the first keeps mirroring.
5. Look at the Control Center illustration on the steps screen. Expected: it renders (not blank or broken).

## 2. Leaving and returning

1. While mirroring, press Home on the TV remote. Expected: the mirror ends within about 2 seconds, and the TV disappears from the iPhone's list.
2. Reopen the app. Expected: the TV is back in the iPhone's list within about 5 seconds, with no button press.

## 3. Resolution

1. Free tier (unlock app not installed): mirror a landscape video, then a portrait app (for example Settings or Safari). Expected: video is no larger than 1280x720 and has the right proportions in both (no stretching or squashing).
2. Install the unlock app (`adb install -r` as above), close the main app, reopen it and mirror again. Expected: mirroring now runs at 1080p, and the footer says "Full HD · unlocked".
3. With the main app in the background (press Home), uninstall the unlock app (`adb uninstall com.screenmirroringtv.unlock`). Reopen the main app and mirror. Expected: back to 720p.

## 4. Audio only

1. On the iPhone, play a song in the Music app and send the audio to the TV. Expected: the TV shows "Playing audio from <iPhone name>" (or "Playing audio from your iPhone" if the name is not available), and you hear the music.

## 5. Home screen behaviour

1. Leave the steps screen untouched for 60 seconds. Expected: it switches to the ambient screen.
2. Press any remote key. Expected: it returns to the steps screen.
3. On the ambient screen, press OK to wake it. Expected: the screen wakes and the Settings dialog does NOT open.
4. On the ambient screen, press Back. Expected: it returns to the steps screen and the app does not exit.
5. Leave the app on the ambient screen for longer than the TV's own screensaver timeout (check Settings, usually 5 to 15 minutes), then mirror from the iPhone. Expected: the TV is still listed, mirroring works, and the TV's screensaver never took over.
6. On a TV that has its own built-in AirPlay or Google Cast receiver, check that this TV still appears in the iPhone's list under its name. If the steps screen instead shows "This TV can't be found right now. Close and reopen the app.", note it (the app could not start its server).

## 6. Settings

1. Open Settings, choose Rename TV, use the TV's on-screen keyboard to type "Kitchen", and Save. Expected: the on-screen keyboard works in the field, and the iPhone lists "Kitchen" within about 5 seconds.
2. Open the dialog again and clear the name to blank. Expected: Save is disabled.
3. Rename to a 40-character name, then set the TV's font size to Large (or the largest) in accessibility settings. Expected: the steps screen and dialog still fit, with no clipped or overlapping text.
4. Mirror with the iPhone upright. Expected: the sides show a soft, dimmed blur of the picture, with no visible blocks, and motion stays smooth. Turn "Blurred sides for upright iPhone" Off in Settings and mirror again. Expected: the sides are plain black.

## 7. First run

1. If Android shows a local-network permission dialog, move to "Allow" with the D-pad and press OK. Expected: the button can be reached and selected with the remote alone. (Clear the app's data to see this dialog again.)

## 8. Network

1. Plug the TV into Ethernet (Wi-Fi off). Expected: the iPhone finds the TV and mirrors.
2. With the app open, switch the TV to a different Wi-Fi network, and join the iPhone to that network too. Expected: the TV appears in the iPhone's list within about 10 seconds. If not, the app needs to re-register for discovery on network change; report it.

## 9. Latency

1. Open a stopwatch that shows milliseconds on the iPhone and mirror it to the TV.
2. Take a photo with the iPhone and the TV in the same frame, three times.
3. For each photo, read both times and subtract (iPhone minus TV). Expected: the median is under 200 ms.
4. If it is not: before any code change, try upstream's `SCHEDULED_OUTPUT_BUFFER_RELEASE` and `AUDIO_ADAPTIVE_STEP` defaults, re-measure, and record the result in the README.

## 10. Soak

1. Mirror on the free tier for 30 minutes (a video with sound is a good test). Expected: no time limit, no prompt or pop-up asking for payment, and no disconnect.
