<div align="center">
🦖🌿 Dino Garden Live Wallpaper

Your home screen is a peaceful garden. The moment your internet drops, it becomes a game.

Show Image
Show Image
Show Image
Show Image

</div>

📖 About

Dino Garden is a Kotlin-based Android Live Wallpaper that turns a dead moment — losing signal — into a small joy instead of a small annoyance.

By default, your screen shows a calm, lightweight, animated garden. But the instant ConnectivityManager reports the device has lost its default network, the wallpaper transforms in real time: the garden becomes an arcade course, a running Dino appears, bushes scroll in as obstacles, and a single tap makes the Dino jump. The moment your connection is restored, it gracefully settles back into the peaceful garden — no app switch, no loading screen, no input required.

Everything renders directly onto the wallpaper's SurfaceHolder canvas — there's no hidden Activity, no background game running when you're not looking at it, and no battery cost while the wallpaper isn't visible.


✨ Features

FeatureDetails🌐Real-time connectivity awarenessUses the modern ConnectivityManager.NetworkCallback API (registerDefaultNetworkCallback) — fully push-based, zero polling, and far gentler on battery than the deprecated BroadcastReceiver + CONNECTIVITY_ACTION approach.🎮A full arcade game inside your wallpaperGravity-driven jump arc, frame-based run-cycle sprite animation, scrolling obstacles, and AABB (axis-aligned bounding box) collision detection — all running directly on the Canvas.🔋Battery-first lifecycle handlingonVisibilityChanged(false) tears the render loop down completely — locking the screen or opening any app stops every timer this service owns. Nothing runs unless you can actually see it.🧵Dedicated render threadPhysics and drawing run on their own HandlerThread, off the main thread, with a single-writer threading model so game state never needs locks.🌱A genuinely lightweight idle stateThe "online" garden state does a single bitmap draw per frame with zero physics — it costs almost nothing to sit there looking pretty.📱Resolution-independent renderingSprite and obstacle sizes are derived from screen-ratio math at runtime, so the game looks correct on phones, tablets, and foldables alike.👆One-tap controlsA single tap is all it takes to jump — designed for a wallpaper, not a dedicated app, so the controls had to be that simple.


🗂️ Project Structure

textDinoGardenWallpaper/
├── app/
│   ├── build.gradle.kts
│   └── src/
│       └── main/
│           ├── AndroidManifest.xml
│           ├── java/
│           │   └── com/
│           │       └── example/
│           │           └── dinogarden/
│           │               └── DinoGardenWallpaperService.kt   # Service + Engine + game loop
│           └── res/
│               ├── drawable/
│               │   ├── bg_garden.png       # Peaceful idle-state background
│               │   ├── dino_run1.png       # Run-cycle frame 1
│               │   ├── dino_run2.png       # Run-cycle frame 2
│               │   └── obstacle_bush.png   # Scrolling obstacle sprite
│               └── xml/
│                   └── wallpaper_info.xml  # Live wallpaper metadata descriptor
├── gradle/
│   └── wrapper/
├── .github/
│   └── workflows/
│       └── android.yml       # CI: build + test on every push/PR to main
├── .gitignore
├── build.gradle.kts
├── settings.gradle.kts
├── LICENSE
└── README.md


🚀 Quick Start

Prerequisites


Android Studio (Hedgehog or newer recommended)
A device or emulator running Android 7.0 (API 24) or higher
JDK 17 (bundled with recent Android Studio installs)


Installation


Clone the repository


bash   git clone https://github.com/your-username/dino-garden-wallpaper.git
   cd dino-garden-wallpaper


Open in Android Studio
Choose File → Open, select the cloned DinoGardenWallpaper folder, and let Gradle sync finish.
Run it on a device or emulator
Connect a physical device (with USB debugging enabled) or start an emulator, then click Run ▶ in Android Studio.
Set it as your live wallpaper
On the installed device:

Long-press an empty area of the home screen
Tap Wallpapers → Live Wallpapers
Select Dino Garden Live Wallpaper → Apply



Try it out
Turn on airplane mode (or step out of Wi-Fi/cell range) and watch the garden turn into a game. Tap the screen to jump. Reconnect, and it settles back into the peaceful garden on its own.



🧭 Roadmap

These are ideas under consideration for future releases — contributions and discussion on any of them are very welcome:


🏆 Global high scores — an optional, opt-in online leaderboard for longest survival streaks while offline
🎨 Custom sprite packs — swap the Dino and garden art for community-made themes (space, ocean, retro pixel, etc.)
⚙️ In-app settings screen — adjustable difficulty (obstacle speed/gravity), toggleable sound effects, and a manual "preview game mode" switch
🌗 Day/night cycle — the garden's color palette shifts with the device's actual local time
☁️ Parallax background layers — drifting clouds and foreground grass for depth in the idle state
📳 Haptic feedback — a light tap vibration on jump and on obstacle near-misses
📊 Offline session stats — a small on-device log of "time spent offline" turned into a lighthearted weekly recap
⌚ Wear OS companion tile — a tiny version of the same online/offline indicator for the wrist



🤝 Contributing

Issues and pull requests are genuinely welcome. If you're picking up a roadmap item, consider opening an issue first so the approach can be discussed before you invest time in an implementation.


Fork the repository
Create a feature branch (git checkout -b feature/parallax-clouds)
Commit your changes
Push to your fork and open a Pull Request against main


Every PR runs through the CI workflow in .github/workflows/android.yml, which builds the project and runs the test suite automatically.


📄 License

This project is licensed under the MIT License — see LICENSE for the full text.

<div align="center">
Made with 🌿 and a healthy disrespect for dead Wi-Fi signal.

</div>
