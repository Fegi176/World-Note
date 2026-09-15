# Committed verification summaries

These summarize actual local checks without committing device dumps, personal serials, SDK paths or generated APKs.

| Evidence | Scope |
|---|---|
| [baseline-0.2.0.json](baseline-0.2.0.json) | Counts/method names extracted from retained JVM and API 34 XML |
| [board-media-0.2.1.txt](board-media-0.2.1.txt) | Actual four-method Android 14 instrumentation output |
| [board-picker-0.2.1.txt](board-picker-0.2.1.txt) | Native board Image picker launch/cancel smoke |

Read [TEST_REPORT.md](../TEST_REPORT.md) for APK hashes, run distinctions, initial failures, known gaps and physical installation/launch evidence.

Original reports remain under ignored `artifacts/`. Curated screenshots use synthetic sample content. GitHub Actions produces independent reports when run; these summaries do not claim remote CI passed.
