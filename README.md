# MorseTap 

MorseTap is a modern Android application designed for learning and using Morse code through an interactive touch interface. Users can tap and hold on the screen to enter dots and dashes, view the translated text in real time, hear audio feedback, and access a built-in Morse code reference sheet.

The app combines a retro-inspired terminal interface with visual signal feedback to provide an intuitive and enjoyable way to practice Morse code.

## Features

* Interactive tap pad for entering Morse code.
* Automatic translation of dots and dashes into text.
* Adjustable transmission speed (WPM).
* Real-time waveform visualizer.
* Built-in Morse code dictionary.
* Audio tone feedback while tapping.
* Text-to-Speech playback of decoded messages.
* Smooth animations and Material 3 design.

## Project Structure

```text
app/src/main/java/com/example/morsetap/

├── MainActivity.kt
├── MorseTranslator.kt
├── SoundManager.kt
├── MainScreenViewModel.kt
├── MainScreen.kt
└── MorseDictionarySheet.kt
```

### MainActivity.kt

The entry point of the application.

* Initializes Text-to-Speech.
* Sets up the app theme.
* Launches the main screen.

### MorseTranslator.kt

Handles Morse code translation.

* Converts Morse code into characters.
* Provides data for the reference dictionary.

### SoundManager.kt

Responsible for audio feedback.

* Generates the Morse tone.
* Starts and stops sound playback during tapping.

### MainScreenViewModel.kt

Manages application state and decoding logic.

* Tracks user input.
* Detects character gaps.
* Maintains signal history and settings.

### MainScreen.kt

Contains the main interface.

* WPM slider.
* Translation terminal.
* Signal visualizer.
* Interactive tap pad.
* Control buttons.

### MorseDictionarySheet.kt

Displays the built-in Morse code reference.

* Shows letters and symbols with their Morse representations.
* Uses animated transitions for opening and closing.

```
```
