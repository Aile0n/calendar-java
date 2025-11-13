# Third-Party Notices and Licenses
Version: 1.0.3 — Stand: 2025-11-13

This project depends on third‑party libraries. Their licenses are summarized below to help you understand your obligations when using, distributing, or modifying this software.

This list is based on pom.xml as of version 1.0.3 (2025-11-13). Always verify on Maven Central and vendor sites for the most current terms.

## Runtime dependencies

- com.calendarfx:view 12.0.1
  - License: Apache License 2.0
  - Project site: https://github.com/dlsc-software-consulting-gmbh/CalendarFX
  - License text: https://www.apache.org/licenses/LICENSE-2.0
  - Notes: CalendarFX is open source under Apache-2.0. When redistributing, include the Apache-2.0 license and any NOTICE file as required by the license.

- org.openjfx:javafx-controls 22.0.1
  - License: GNU General Public License, version 2, with the Classpath Exception (GPLv2 + CE)
  - Project site: https://openjfx.io/
  - License details: https://openjdk.java.net/legal/gplv2+ce.html

- org.openjfx:javafx-fxml 22.0.1
  - License: GPLv2 + Classpath Exception (same as above)
  - Project site: https://openjfx.io/

- net.sf.biweekly:biweekly 0.6.8
  - License: BSD 2‑Clause
  - Project site: https://github.com/mangstadt/biweekly
  - License text: https://opensource.org/license/bsd-2-clause/

- org.mnode.ical4j:ical4j 4.0.2
  - License: BSD 3‑Clause
  - Project site: https://github.com/ical4j/ical4j
  - License text: https://opensource.org/licenses/BSD-3-Clause

## Test dependencies

- org.junit.jupiter:junit-jupiter 5.10.2 (scope: test)
  - License: Eclipse Public License 2.0 (EPL‑2.0)
  - Project site: https://junit.org/junit5/
  - License: https://www.eclipse.org/legal/epl-2.0/

## Build plugins (do not affect runtime distribution of the app)

- maven-surefire-plugin 3.2.5 — Apache License 2.0
- maven-shade-plugin 3.5.1 — Apache License 2.0

## Additional notes and guidance

- CalendarFX (Apache-2.0): Include the Apache-2.0 license and (if provided) NOTICE file when redistributing. Verify the upstream repository for any additional notices.
- OpenJFX (GPLv2 + Classpath Exception): The Classpath Exception permits linking with independent modules without subjecting the entire application to GPL terms. However, you must still comply with the license's notice requirements.
- Apache 2.0 and BSD‑family licenses: These are permissive; include appropriate notices if you redistribute.

## Version History
- **1.0.3** (2025-11-13): Documentation/comment quality improvements and UI text polish; dependency versions unchanged from 1.0.2
- **1.0.1** (2025-10-16): Corrected CalendarFX license (now Apache-2.0); added Biweekly; updated ical4j to 4.0.2; removed sqlite-jdbc; documentation alignment with migration
- **1.0.0** (2025-10-09): UI improvements, toolbar reorganization
- **0.2.0** (2025-10-08): Persistence bugfixes, auto-save functionality
- **0.1.1** (2025-10-08): Documentation improvements
- **0.1.0** (2025-09-30): Initial release with ICS/VCS support

## How to verify or regenerate this list

If you have Maven available, you can generate an authoritative license inventory:

- Show declared licenses in the dependency tree:
  mvn -q org.codehaus.mojo:license-maven-plugin:2.4.0:aggregate-add-third-party -DdownloadLicenses=true

This will create a THIRD-PARTY file under target/ listing dependencies and their licenses as detected from POM metadata. Always review the output, especially for artifacts with custom or unusual license metadata.
