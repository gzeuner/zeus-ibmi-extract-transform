# IBM i / JT400 Notes (V1)

## Zweck
Diese Notizen beschreiben die technische V1-Basis fuer JDBC read-only Selektionen gegen DB2 for i mit JT400/JTOpen, ohne produktive Verbindung in den Tests.

## Dependency-Strategie
In `pom.xml` ist JT400 als optionale Runtime-Dependency hinterlegt:
- Group: `net.sf.jt400`
- Artifact: `jt400`
- Scope: `runtime`
- Optional: `true`

Dadurch bleibt der Build fuer lokale/offline Tests schlank (H2), waehrend produktnahe Laufzeitkonfiguration JT400 aktivieren kann.

## Driver und URL
Typischer IBM i JDBC Driver:
- `com.ibm.as400.access.AS400JDBCDriver`

Beispiel-URL mit Platzhaltern:
- `jdbc:as400://YOUR_HOSTNAME;naming=sql;errors=full`

## Credentials
Empfohlen:
- Username und Passwort ueber ENV
- Passwort nicht in Config committen

Beispiel ENV-Konzept:
- `ZEUS_IBMI_DB_USER`
- `ZEUS_IBMI_DB_PASSWORD`
- optional `ZEUS_IBMI_DB_PASSWORD_ENV` als indirection

## Libraries vs Schemas
Bei IBM i ist Objektaufloesung vom Naming-Mode abhaengig:
- `naming=sql`: schema.table
- `naming=system`: library/file(member)-nahe Semantik

Die Query-Gestaltung und Qualifizierung sollte mit der Zielumgebung abgestimmt werden.

## CCSID / Encoding
Fuer korrekte Zeichenverarbeitung (Umlaute/Sonderzeichen):
- Encoding-Konzept und CCSID in Test- und Zielumgebung explizit pruefen
- Export-Dateien werden in V1 als UTF-8 geschrieben

## Sicherheits- und Betriebsgrenzen
- V1 ist read-only-first (SELECT/WITH Guard)
- Keine produktive IBM-i-Verbindung in den automatisierten Tests
- Keine IBM-i-Kommandos oder administrativen Pfade
- Produktive Freigabe nur nach Infrastruktur-, Security- und SQL-Review
