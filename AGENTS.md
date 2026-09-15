# NodeNote Worldbuilder

Read NodeNote_Worldbuilder_Codex_Prompt.md completely, IMPLEMENTATION_STATUS.md, and docs/FEATURE_MATRIX.md.
Offline native Kotlin/Compose/Room Android app. Never add runtime network dependencies.
Keep stable lore, placement, relationship, chronology, and story identities separate.
Board deletion cannot delete lore. Unknown dates are valid. Never invent dates or propagate knowledge.
No destructive database migrations. Validate archives before transactional insertion and retain original imports.
Use conventional Gradle/SDK builds. Never fabricate build, test, runtime, or signing evidence.
Windows: scripts/env.ps1 sets this workspace's optional local toolchain; gradlew.bat :core:test :app:testDebugUnitTest :app:lintDebug :app:assembleDebug.
No existing source or repository guidance was present at initialization.

Share the process Room instance. Route content mutations through Repository so incremental observation generations remain coherent; raw DAO writes belong only in migrations/test setup.
