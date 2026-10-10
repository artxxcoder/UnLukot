# UnLukot 🍃

**Scan crumpled paper. Smooth it out. Read and label every component, entirely on your phone.**

> *"Lukot"* is Filipino for crumpled. **UnLukot** un-crumples it.

Everything runs on-device.
APK for trying is under releases or here (https://github.com/artxxcoder/UnLukot/releases/tag/APK)
 - Download the APK and then open it! 
---

## Open in Android Studio

1. **File → Open** → select the `UnLukot` folder.
2. Let Gradle sync (needs internet **once**, to download build dependencies).
3. If Studio offers to upgrade the Android Gradle Plugin / Gradle, accept it.
4. Plug in a phone (Android 8.0+ / API 26+) or use an emulator with a camera, then press ▶ **Run**.

Tips: test on a real phone. Emulator cameras only show a fake scene, so use the **Import** button there to pick a photo of crumpled paper.

---

## What the app does

| Step | What happens | Where it runs |
|---|---|---|
| 1. Flatten | Estimates paper brightness cell by cell, divides out crease shadows, boosts contrast, scores "crumple level" | CPU, on-device |
| 2. Read | Bundled ML Kit Latin text model reads the original *and* flattened image; keeps the better read | On-device |
| 3. Predict | Predicts components (title, labelled fields, dates, times, amounts, phones, emails, links, ID numbers, table rows, paragraphs) and the **paper type** (receipt, invoice, ID/certificate, form, letter/memo, schedule, note) with a confidence % | On-device |

Results show boxes drawn over the flattened paper, tap any component to highlight it. Export as plain text or JSON (Share / Copy). Scans are saved in the app's private storage only.

---

## Settings & About

The **Settings** tab lets you pick a theme (System / Light / Dark), choose how crease flattening is applied (Auto / Always / Off), toggle haptics, highlight boxes and history saving, choose which result view opens first, and clear saved scans. The **About** panel explains what the app does, what it recognizes, privacy, and the libraries it is built with.

## Privacy

- **No INTERNET permission.** `AndroidManifest.xml` explicitly removes it (`tools:node="remove"`), so Android blocks all network use.
- **Bundled model.** `com.google.mlkit:text-recognition` ships the OCR model inside the APK, so there's no model download.
- Scans are saved only in the app's private storage.

---

## Project layout

```
app/src/main/java/com/unlukot/app/
├── MainActivity.kt
├── ScanViewModel.kt            # UI state + orchestration
├── ai/
│   ├── ImageEnhancer.kt        # crease flattening + crumple score + bitmap loading
│   ├── LocalOcr.kt             # bundled ML Kit OCR wrapper
│   ├── DocumentAnalyzer.kt     # component predictor + doc-type classifier
│   ├── UnLukotEngine.kt        # full pipeline + flatten modes
│   └── Models.kt               # data classes + JSON export
├── data/ScanStore.kt           # private on-device storage
├── data/SettingsStore.kt       # user preferences
└── ui/                         # green & white Compose UI (light + dark)
    ├── Theme.kt, Common.kt, App.kt
    ├── HomeScreen.kt, HistoryScreen.kt, ScanRow.kt
    ├── SettingsScreen.kt, SettingRows.kt, AboutPanel.kt
    ├── CameraScreen.kt         # CameraX capture
    ├── ProcessingScreen.kt     # animated scan
    └── ResultScreen.kt, ResultParts.kt  # overlay boxes, components / text / JSON
```

---

## Honest limitations

- The component predictor and paper-type classifier are **rule + weighted-lexicon models**, not a trained neural network. They're fast, explainable, and fully offline, but less flexible than a learned model.
- OCR uses the Latin script model (English, Filipino, etc.). Handwriting is not reliable.
- "Un-crumpling" corrects lighting and shadows, not geometric warping of badly folded pages.
- The crumple score is a quick heuristic estimate.
