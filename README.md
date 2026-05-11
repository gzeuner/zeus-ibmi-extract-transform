# zeus-ibmi-extract-transform

## 1. Projektbeschreibung
`zeus-ibmi-extract-transform` ist ein CLI-first Werkzeug, um read-only SQL-Selektionen gegen DB2 for i (IBM i) kontrolliert auszufuehren und in neutrale Ausgabedateien zu exportieren.

## 2. Status
Early extraction / work in progress.

## 3. Quickstart (Dry-Run und lokal)
Voraussetzungen:
- Java 17+
- Maven 3.9+

Build und Tests:
```bash
mvn test
mvn verify
```

Dry-Run mit Beispielkonfiguration (Default, keine Query-Ausfuehrung):
```bash
java -jar target/zeus-ibmi-extract-transform-0.1.0-SNAPSHOT.jar --config config/example.application.properties
```

Explizite Execute-Ausfuehrung:
```bash
java -jar target/zeus-ibmi-extract-transform-0.1.0-SNAPSHOT.jar --config config/example.application.properties --execute
```

## 4. Beispielkonfiguration
Beispieldatei:
- `config/example.application.properties`

Wichtige Properties:
- `db.driver`
- `db.url`
- `db.user`
- `db.password` (nur wenn bewusst gewuenscht)
- `db.passwordEnv` (empfohlen)
- `db.allowEmptyPassword`
- `query.sql`
- `output.directory`
- `output.formats`
- `run.manifest.enabled`
- `query.fetchSize` (optional)
- `query.timeoutSeconds` (optional)

Prioritaet:
- CLI-Argumente > ENV > Config-Datei > Defaults

## 5. CLI Usage
```text
--help | -h
--version | -v
--config <file>
--execute
--db-driver <class>
--db-url <url>
--db-user <user>
--db-password <pwd>
--db-password-env <ENV_VAR>
--query <sql>
--output-dir <path>
--output-formats <csv>
--manifest-enabled
--manifest-disabled
--fetch-size <n>
--query-timeout-seconds <n>
--allow-empty-password
```

## 6. Dry-Run vs Execute
Dry-Run ist Default:
- ohne `--execute` wird keine JDBC-Query ausgefuehrt
- Query-Guard und Konfiguration werden validiert
- geplante Outputs werden angezeigt
- Secrets werden maskiert

Execute nur mit `--execute`:
- read-only Query wird ausgefuehrt
- Outputs werden geschrieben
- RunManifest wird bei aktivierter Manifest-Option geschrieben

## 7. Output-Formate
V1 unterstuetzt:
- `xml`
- `json`
- `csv`
- `md`

`jsonl` ist aktuell noch nicht umgesetzt (Roadmap).

## 8. RunManifest
Manifest-Felder (Auszug):
- tool name + version
- startedAt / finishedAt / durationMillis
- status (`SUCCESS`, `FAILED`, `DRY_RUN`)
- dryRun
- configSource (ohne Secret-Werte)
- queryHash
- queryPreview (gekuerzt)
- outputDirectory
- outputFiles
- outputFormats
- rowCount / columnCount
- errorClass / errorMessage (maskiert)
- javaVersion / osName / osVersion

## 9. IBM i / JT400 Konfiguration
Driver Class fuer IBM i:
- `com.ibm.as400.access.AS400JDBCDriver`

JDBC-URL-Beispiel mit Platzhaltern:
- `jdbc:as400://YOUR_HOSTNAME;naming=sql;errors=full`

Empfohlene Credential-ENVs:
- `ZEUS_IBMI_DB_USER`
- `ZEUS_IBMI_DB_PASSWORD`
- optional: `ZEUS_IBMI_DB_PASSWORD_ENV`

Hinweise:
- DB2 for i nutzt oft Library-/Schema-Konzepte, die vom Naming-Mode abhaengen (`sql` vs `system`).
- CCSID/Encoding sollte fuer produktive Umgebungen explizit getestet werden.
- V1 ist read-only-first (SELECT/WITH Guardrail), aber kein vollstaendiger Schutz gegen jede Fehlkonfiguration.

Weitere Details:
- `docs/ibmi-jt400-notes.md`

## 10. Security / Secrets
- Keine echten Credentials im Repository.
- Secrets bevorzugt aus ENV beziehen (`db.passwordEnv`).
- CLI/Manifest-Fehlermeldungen werden maskiert.
- Produktive Nutzung nur mit separatem Security Review und Betriebsfreigabe.

## 11. Exit-Codes
- `0` Erfolg
- `1` Allgemeiner Fehler
- `2` Konfigurationsfehler
- `3` Query-Guard-Fehler
- `4` JDBC-/SQL-Fehler
- `5` Output-/Dateisystemfehler
- `6` Manifest-Fehler

## 12. Roadmap
- CLI-UX weiter stabilisieren (inkl. erweiterte Report-Ausgaben)
- optionale `jsonl`-Ausgabe
- Vergleichslauf gegen Legacy-Verhalten (`zeus-access-400`)
- haertere Integrations- und Kompatibilitaetstests fuer IBM-i-spezifische SQL-Faelle

## 13. Bezug zu `zeus-access-400`
Das Projekt ist der kontrollierte Nachfolgepfad fuer den Legacy-Selektionskern. Die Migration erfolgt schrittweise, testgestuetzt und ohne Big-Bang-Umbau.

## Disclaimer
Dieses Projekt ist kein offizielles IBM-Produkt und steht in keiner Verbindung zu IBM.
Es gibt keine Gewaehr fuer produktive Eignung.
Entwicklerinnen und Entwickler bleiben fuer Konfiguration, Ausfuehrung und Bewertung der Ergebnisse verantwortlich.
Read-only Guardrails reduzieren Risiko, ersetzen aber kein fachliches und betriebliches Review vor produktivem Einsatz.
