# Third-Party Notices and Licenses

This project depends on third‑party libraries. Their licenses are summarized below to help you understand your obligations when using, distributing, or modifying this software.

This list is based on pom.xml as of version 1.0.0 (2025-10-09). Always verify on Maven Central and vendor sites for the most current terms.

## Runtime dependencies

- com.calendarfx:view 12.0.1
  - License: CalendarFX commercial license by DLSC (vendor license). See https://dlsc.com/products/calendarfx/
  - Notes: CalendarFX is a commercial product. Use in production typically requires a paid license from DLSC unless you qualify under a specific offering. Confirm your organization's license.

- org.openjfx:javafx-controls 22.0.1
  - License: GNU General Public License, version 2, with the Classpath Exception (GPLv2 + CE)
  - Project site: https://openjfx.io/
  - License details: https://openjdk.java.net/legal/gplv2+ce.html

- org.openjfx:javafx-fxml 22.0.1
  - License: GPLv2 + Classpath Exception (same as above)
  - Project site: https://openjfx.io/

- org.xerial:sqlite-jdbc 3.42.0.0
  - License: Apache License 2.0
  - Project site: https://github.com/xerial/sqlite-jdbc
  - License text: https://www.apache.org/licenses/LICENSE-2.0

- org.mnode.ical4j:ical4j 3.2.7
  - License: BSD 3‑Clause
  - Project site: https://github.com/ical4j/ical4j
  - License: https://opensource.org/licenses/BSD-3-Clause

## Test dependencies

- org.junit.jupiter:junit-jupiter 5.10.2 (scope: test)
  - License: Eclipse Public License 2.0 (EPL‑2.0)
  - Project site: https://junit.org/junit5/
  - License: https://www.eclipse.org/legal/epl-2.0/

## Build plugins (do not affect runtime distribution of the app)

- maven-surefire-plugin 3.2.5 — Apache License 2.0
- maven-shade-plugin 3.5.1 — Apache License 2.0

## Additional notes and guidance

- CalendarFX: Please review DLSC's licensing terms before distributing or using this application. If you intend to distribute binaries, ensure that your CalendarFX license allows redistribution in your context.
- OpenJFX (GPLv2 + Classpath Exception): The Classpath Exception permits linking with independent modules without subjecting the entire application to GPL terms. However, you must still comply with the license's notice requirements.
- Apache 2.0 and BSD‑3‑Clause: Both are permissive licenses; include appropriate notices if you redistribute.
- EPL‑2.0 (test only): Applies to test artifacts; generally not redistributed with your application.

## Version History
- **1.0.0** (2025-10-09): UI improvements, toolbar reorganization
- **0.2.0** (2025-10-08): Persistence bugfixes, auto-save functionality
- **0.1.1** (2025-10-08): Documentation improvements
- **0.1.0** (2025-09-30): Initial release with ICS/VCS support

## How to verify or regenerate this list

If you have Maven available, you can generate an authoritative license inventory:

- Show declared licenses in the dependency tree:
  mvn -q org.codehaus.mojo:license-maven-plugin:2.4.0:aggregate-add-third-party -DdownloadLicenses=true

This will create a THIRD-PARTY file under target/ listing dependencies and their licenses as detected from POM metadata. Always review the output, especially for artifacts with vendor or custom licenses (e.g., CalendarFX).
