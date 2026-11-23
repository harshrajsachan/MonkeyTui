# MonkeyTUI Coding Standards

These rules are mandatory and are checked automatically.

## 1. Function Size

- Every function, constructor, and helper has at most **6 executable lines**.
- Blank lines do not count.
- Comment-only lines do not count.
- Lines containing only braces or semicolons do not count.
- Control-flow lines count as executable lines.
- Extract a named helper instead of compressing code into an unreadable one-liner.
- Do not fragment code without a meaningful responsibility.

Example:

```java
// Computes the next selection index within bounds.
private int nextIndex(int current, int size) {
    int last = size - 1;
    return Math.max(0, Math.min(last, current + 1));
}
```

## 2. Function Comments

- Every function has **exactly one** single-line `//` comment immediately above it.
- For annotated functions, the comment goes immediately above the annotation.
- The comment describes the function's responsibility.
- JavaDoc comments, block comments, file-level comments, and inline comments are prohibited.

Allowed:

```java
// Returns the standard history file path.
private static Path defaultPath() {
    return Path.of(System.getProperty("user.home"), ".local", "share", "monkeytui", "history.tsv");
}
```

Prohibited:

```java
/** Returns the history path. */
private static Path defaultPath() { ... }
```

## 3. Main.java

`Main.java` is only the application entry point.

- It delegates to `MonkeyTui.launch(args)`.
- It contains no event loop, rendering, persistence, configuration, or state management.
- It must not contain application logic or control flow beyond startup delegation.

## 4. Architecture Boundaries

- `com.monkeytui.typing` owns the typing engine, mode catalog, durations, and snippet loading.
- `com.monkeytui.history` owns run records and TSV persistence.
- `com.monkeytui.ui` owns screen models, layout, painting, panels, and the color theme.
- `com.monkeytui.app` owns application state, keyboard actions, terminal setup, and the event pump.
- `com.monkeytui.Main` owns startup delegation only.

Business logic must not render. Rendering must not persist or mutate test state. History serialization must not depend on terminal code.

## 5. Automated Enforcement

The checker is integrated into the Maven lifecycle and runs during `validate`:

```bash
mvn validate
```

It checks the main and test source trees for:

- functions exceeding six executable lines,
- missing or duplicate single-line function comments,
- JavaDoc and multiline comments,
- inline and file-level comments,
- `Main.java` logic beyond startup.

Run the checker directly:

```bash
python3 scripts/check_standards.py src/main/java src/test/java
```

## 6. Verification Commands

```bash
mvn validate          # coding standards
mvn test              # unit and rendering tests
mvn package           # build target/MonkeyTui.jar
mvn exec:java         # run from Maven
java -jar target/MonkeyTui.jar
```

Changes are complete only when the standards check, tests, package build, and application launch all succeed.
