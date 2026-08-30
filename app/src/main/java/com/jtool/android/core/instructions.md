# Gemini System Instructions & Guidelines

## 1. Code Generation & Refactoring
* **No Comments:** Never add inline comments or documentation blocks to generated code.
*   **Strong Typing & Generics**: Strictly enforce strong typing across all data models. Abstract shared functionality using interfaces and generic implementations where applicable.
*   **String IDs**: All data models and Firestore documents must use `String` for their `id` property. Never use numeric types for identifiers.
*   **Enum Extraction, Naming & Location**: All `enum class` definitions must be extracted into their own dedicated files. Every enum class name must end with the `Enum` suffix (e.g., `SupportStatusEnum`). These files must be placed within the `core/models` directory under a relevant category's `enums` subfolder (e.g., `core/models/support/enums/SupportStatusEnum.kt`) or in `core/models/common/enums/` if shared across multiple domains. Never define enums inside other classes or feature logic files.
*   **Strict Type Definitions**: Never use `Any` (or `Object`) as a class placeholder. Always use an existing class or create a new dedicated class if a suitable model does not exist.
* **Enum-Based Parameters:** When building generic components or functions accessed via string parameters, map all string options to an enum (string-based or numeric). Never accept loose string parameters directly, and ensure existing code remains compatible without breakage.
* **Component Extraction Prompting:** Whenever an opportunity arises to modularize code, prompt the developer to extract the logic into a reusable file and add it to the Admin Panel "Kitchen Sink."
* **Database Field Stability:** Never modify, rename, or remove existing data model property names to maintain database schema stability and prevent connection errors.

## 2. Page Consistency & Architectural Changes
* **Structural Consistency:** New pages must conform strictly to existing project layouts, structural patterns, and implementation styles.
* **Change Approvals:** Any proposed deviations or new architectural implementations must be backed by a clear technical justification and explicitly prompted to the user for approval before applying.

## 3. Localization & Internationalization
* **Dynamic Resource Strings:** Never hardcode user-facing strings. Add all new display text to the Localization Service across all supported language files.
* **Dynamic Language Resolution:** Never hardcode lists of supported languages. Always query and resolve language options dynamically from the implemented language resource files at runtime.

## 4. State & Async Handling
* **Safe Loading States:** Guarantee every loading state execution path (success, failure, cancellation, or exception) resolves properly to prevent deadlocks or frozen UIs.
* **Error Notification:** If an unresolved loading state risk arises, flag the issue to the developer immediately to determine a resolution.

## 5. Change Logs & Developer Reporting
* **Technical Summaries:** End responses with a clear summary of modifications, including technical implementation details and an assessment of potential risks introduced by the changes.

---

# Global Setup for New Chats
To maintain consistency across all sessions, copy the following block into **Android Studio > Settings > Tools > Gemini > Custom Instructions**:

```text
Follow the architectural guidelines in 'app/src/main/java/com/jtool/android/core/instructions.md'.
- No comments/KDoc in code.
- Strict typing (no Any/Object).
- All Enums must have 'Enum' suffix and stay in 'core/models/.../enums/'.
- All IDs must be Strings.
- Data models must have default values for all properties (Firestore compatibility).
- Use Property::name.name for Firestore keys.
- Use Localization Service (Loc) for all UI strings.
```