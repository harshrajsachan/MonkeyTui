# ⌨️ MonkeyTUI

A terminal-based typing practice application inspired by **Monkeytype**. Built in Java using the Lanterna library, MonkeyTUI provides a distraction-free environment to improve typing speed and accuracy directly from the terminal.

![Java](https://img.shields.io/badge/Java-21-orange)
![Maven](https://img.shields.io/badge/Maven-Build-blue)
![Lanterna](https://img.shields.io/badge/UI-Lanterna-green)
![License](https://img.shields.io/badge/License-MIT-yellow)

---

## ✨ Features

- 🚀 Terminal-first typing experience
- 📈 Real-time WPM calculation
- 🎯 Live accuracy tracking
- 📝 Multiple typing difficulty levels
- 💻 Programming language snippets
  - C++
  - Java
- 🎨 Clean Text User Interface (TUI)
- ⚡ Lightweight and fast

---

## Preview

*(Add screenshots or GIFs here)*

```
WPM: 82
Accuracy: 98%

The quick brown fox jumps over the lazy dog...
```

---

## Project Structure

```
src
├── main
│   ├── java
│   │   ├── game
│   │   ├── tui
│   │   ├── lang
│   │   └── Main.java
│   └── resources
│       └── snippets
│           ├── easy.txt
│           ├── medium.txt
│           ├── hard.txt
│           ├── cpp.txt
│           └── java.txt
```

---

## Requirements

- Java 21 (or newer)
- Maven 3.9+

---

## Installation

Clone the repository:

```bash
git clone git@github.com:harshrajsachan/MonkeyTui.git
cd MonkeyTui
```

Build the project:

```bash
mvn clean package
```

---

## Running

Run using Maven:

```bash
mvn exec:java
```

Or execute the generated JAR:

```bash
java -jar target/MonkeyTui.jar
```

---

## Controls

| Key | Action |
|------|--------|
| Start Typing | Begin the test |
| ESC | Exit |
| ENTER | Restart (if implemented) |

---

## Metrics

MonkeyTUI tracks:

- Words Per Minute (WPM)
- Typing Accuracy
- Characters Typed
- Errors

---

## Technologies Used

- Java
- Maven
- Lanterna
- ANSI Terminal

---

## Future Improvements

- [ ] Themes
- [ ] Custom text generation
- [ ] Time-based mode
- [ ] Quote mode
- [ ] Leaderboard
- [ ] Typing history
- [ ] Configuration file
- [ ] Vim keybindings

---

## Inspiration

MonkeyTUI is inspired by the excellent typing website **Monkeytype**, reimagined as a terminal application for developers who prefer staying inside the terminal.

---

## License

This project is licensed under the MIT License.

---

## Author

**Harsh Raj Sachan**

GitHub: https://github.com/harshrajsachan
