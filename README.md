# WorldScribe 📜✨

**WorldScribe** is an interactive, live story-simulation engine powered by Google's flagship Gemini models. Built on a modular "Lego-brick" architecture, it allows players to initialize custom dimensions, interact with living story worlds, and watch the narrative automatically discover, track, and update dynamic world aspects (characters, places, legends, and legacies) in real-time.

---

## 🎨 Modular Design & Core Concepts

- **Interactive Narrative Sandbox**: Interact with a highly responsive storyteller (AI Dungeon style) that narrates consequences, handles dynamic elements, and proposes 3 thematic suggestions.
- **The Lego Brick Universe**: Unlike hardcoded storytelling, every dimension is modeled of decoupled world entities:
  - **Dynamic Characters**: Tracked as active (ALIVE) or dead (DEAD). If a villain or hero passes away, their state updates, and the storyteller retains their legacy summary as history.
  - **Discovered Locations**: Regional spots dynamically uncovered during story choices or handcrafted by the user.
  - **Legends & Lores**: Deep world secrets, magical items, or rules of physics that govern how the simulation remains coherent.
- **Aspect Scribe Pane**: Players can manually inject their own characters, places, or lore keys mid-game to instantly override or enrich the simulation rules.

---

## ⚙️ Key Engine Features

1. **Continue & Library Management**: Offers a polished classic menu tray (Continue, Load World, Conjure, Import, and Settings).
2. **Transferable World Codes (JSON Clipboard Share)**: Worlds can be exported into a single unified JSON text string that contains entire simulation runs, dynamic entity logs, and histories. Copy to share with friends, or paste in the Import box to reconstruct those exact realms instantly.
3. **Smart Timeline Delimitation**: Chat timeline highlights newly introduced elements directly underneath the paragraph with charming badges (e.g., `👤 Introduced: Dorian`, `📍 Discovered: Void Spires`).
4. **Room Persistent Storage**: Built with dynamic SQL tables to store simulation states, and messages safely on your local device.

---

## 🚀 Getting Started (API Keys Setup)

To unleash the interactive universe storyteller, configure your Gemini API credentials:

1. Copy your developer token from Google AI Studio.
2. In **Google AI Studio Build**, open the **Secrets panel** in the UI.
3. Reference `GEMINI_API_KEY` and insert your token.
4. Run the app to play!

---

## 🛠️ Project Tech Stack

- **Framework**: Jetpack Compose (Modern Material 3 Adaptive UI layout)
- **Architecture**: MVVM (Model-View-ViewModel Architecture)
- **Database**: Room Database with Kotlin Symbol Processing (KSP)
- **Remote AI Network**: REST Retrofit Client with custom Moshi json adapter configurations
- **Serialization**: Moshi Kotlin Reflection for high safety JSON marshalling
