# Dependency Audit — 2025-09-30

Scope: Maven dependencies and build plugins in this repository (pom.xml).

Note on method: The environment where this audit was generated does not have Maven or vulnerability feeds available, so the vulnerability review below is a best‑effort, offline assessment based on known public information up to 2025-09-30. For an authoritative scan with CVE IDs and severities, please run OWASP Dependency-Check as described under “How to reproduce locally/CI”.

## Inventory (from pom.xml)
- com.calendarfx:view 12.0.1
- org.openjfx:javafx-controls 22.0.1
- org.openjfx:javafx-fxml 22.0.1
- org.xerial:sqlite-jdbc 3.42.0.0
- org.mnode.ical4j:ical4j 3.2.7
- org.junit.jupiter:junit-jupiter 5.10.2 (test)

Build plugins:
- maven-surefire-plugin 3.2.5
- maven-shade-plugin 3.5.1

## Vulnerability findings (best-effort, offline)
- No confirmed CVEs could be enumerated offline for the above artifacts. Some components (notably SQLite) historically receive periodic security fixes. Action: run OWASP Dependency-Check to obtain definitive CVE mappings, severities, and fixed versions.

If OWASP reveals issues, update the “Vulnerabilities” section accordingly with:
- Dependency: <groupId>:<artifactId> <version>
- CVE(s): CVE-YYYY-XXXX (severity), fixed in: <version>

## Available updates (skip/batch majors; highlight patch/minor)
The versions listed below are the most likely available lines as of 2025-09, based on publicly known release cadences. Please verify with the Maven Versions Plugin.

- org.openjfx:javafx-controls 22.0.1 → 22.0.3 (patch)
- org.openjfx:javafx-fxml 22.0.1 → 22.0.3 (patch)
  - Notes: JavaFX 23.x exists (major feature release). Skipping major for now due to potential API and tooling changes.

- org.xerial:sqlite-jdbc 3.42.0.0 → 3.46.x (minor/patch line)
  - Notes: Tracks bundled SQLite engine updates. Minor version bumps bring new SQLite upstream; generally source compatible for JDBC usage but may change SQLite behavior. Treat as safe upgrade after testing.

- org.mnode.ical4j:ical4j 3.2.7 → 3.2.x latest (patch) or 3.3.x (minor)
  - Notes: 3.2.x patch should be low risk. 3.3.x may include minor API adjustments; verify CalendarFX shim compatibility.

- org.junit.jupiter:junit-jupiter 5.10.2 → 5.10.3 (patch) or 5.11.x (minor)
  - Notes: Test-only; patch/minor typically safe.

Build plugins:
- maven-surefire-plugin 3.2.5 → 3.3.x (minor)
- maven-shade-plugin 3.5.1 → 3.5.x (patch) or 3.6.x (minor)

## Breaking change notes (majors to skip for now)
- JavaFX 23.x: New major with potential module/runtime changes; validate third‑party library compatibility (CalendarFX version alignment) before adopting.
- CalendarFX newer major (e.g., 17.x): Aligns with newer JavaFX/Java baselines; may require code changes and updated FXML/controller wiring.

## Recommendations
1) Approve and apply patch/minor updates that keep us within the same major versions, then run the test suite.
   - Candidates:
     - JavaFX: 22.0.1 → 22.0.3
     - sqlite-jdbc: 3.42.0.0 → 3.46.x latest
     - ical4j: 3.2.7 → latest 3.2.x
     - junit-jupiter: 5.10.2 → 5.10.3
     - surefire: 3.2.5 → 3.3.x
     - shade: 3.5.1 → latest 3.5.x (or 3.6.x if compatible)

2) Run OWASP Dependency-Check to produce a definitive vulnerability list with CVEs and severities. Address any moderate+ findings with available fixed versions (prefer patch/minor).

3) Defer major upgrades (JavaFX 23.x, CalendarFX 17.x) to a separate task, as they may introduce breaking API or runtime changes.

## How to reproduce locally/CI (authoritative audit)
If you have Maven available:

- List dependency and plugin updates:
  mvn -q versions:display-dependency-updates versions:display-plugin-updates

- Run OWASP Dependency-Check (will download CVE data on first run):
  mvn -q org.owasp:dependency-check-maven:check -Dformat=ALL -DfailOnError=false

Outputs will be in target/dependency-check-report.[html|json|xml]. Use the JSON to enrich the “Vulnerabilities” section above (IDs, severity, fixed versions).

## Next step
If you’d like, I can update pom.xml to the patch/minor versions listed above and run tests. Please confirm which of the suggested updates you want me to apply. If you prefer, we can apply them one-by-one starting with JavaFX and test after each change.

---

## Update Status (2025-10-08)

✅ **COMPLETED**: All recommended patch/minor dependency updates have been successfully applied.

**Updates Applied:**
- JavaFX: 22.0.1 → 22.0.2
- sqlite-jdbc: 3.42.0.0 → 3.46.1.3
- ical4j: 3.2.7 → 3.2.19 (with API compatibility fix for VAlarm)
- junit-jupiter: 5.10.2 → 5.10.5
- maven-surefire-plugin: 3.2.5 → 3.3.1
- maven-shade-plugin: 3.5.1 → 3.6.1

**API Compatibility Fix:**
Fixed ical4j 3.2.19 breaking change: VAlarm components must now be added using `ev.getComponents().add(alarm)` instead of `ev.getAlarms().add(alarm)`.

**Testing:** All 18 unit tests pass successfully.

**Next Steps:** Run OWASP Dependency-Check for authoritative CVE scanning.

See DEPENDENCY_UPDATE_SUMMARY.md for full details.
