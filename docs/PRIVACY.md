# Privacy and local files

No account, subscription, advertising, analytics, cloud service, AI SDK, runtime web fonts, or background network access. The manifest requests no INTERNET or broad storage permissions. Runtime dependencies and sample artwork are bundled/local.

Data lives in Room, DataStore preferences, and app-private managed originals. Android cloud backup and device-transfer extraction domains are excluded by manifest/XML rules. OEM behavior cannot be guaranteed by the application.

Document/photo selection is user-mediated through the system picker. A provider can itself be cloud-backed; NodeNote does not select that destination or upload independently. Accepted images are copied, so moving a source file does not break the gallery.

Markdown HTML is escaped. Unsafe URL schemes and automatic remote images are disabled. An ordinary link opens externally only when tapped. Lore text is never executed as code.

Complete backups, author Markdown exports, and internal recovery copies contain private notes and secrets and are not encrypted. Public account exports use a separate allowlist projection. It is an authoring tool, not a multi-user access-control or encryption boundary.

Only generated synthetic fixtures and original sample lore are used in tests. No other projects or private lore were read.
