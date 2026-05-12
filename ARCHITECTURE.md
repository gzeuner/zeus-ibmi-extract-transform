# Architecture

## Layering
- `de.zeus.ibmi.infrastructure.cli`: CLI entrypoint and user interaction rendering.
- `de.zeus.ibmi.application`: orchestration services (`ExtractOrchestrator`, `ExtractService`, `RunManifestService`).
- `de.zeus.ibmi.domain`: immutable core records and sealed format model.
- `de.zeus.ibmi.infrastructure`: adapters for Spring launcher/config/security.
- `de.zeus.ibmi.selection|output|runmanifest|connection|query`: extraction engine and adapters preserved for compatibility.

## C4 (Container / Component)
```plantuml
@startuml
!include https://raw.githubusercontent.com/plantuml-stdlib/C4-PlantUML/master/C4_Container.puml

Person(operator, "Operator")
System_Boundary(sys, "zeus-ibmi-extract-transform") {
  Container(cli, "CLI (Picocli)", "Java 21", "Argument parsing, help, dry-run/execute mode")
  Container(app, "Application Services", "Java 21", "Orchestration and run lifecycle")
  Container(engine, "Extraction Engine", "JDBC", "Read-only guard, query execution, output generation")
  Container(manifest, "RunManifest Service", "JSON", "Audit metadata and checksums")
}
SystemDb(db2i, "IBM i DB2")
System_Ext(fs, "Filesystem")

Rel(operator, cli, "Runs command")
Rel(cli, app, "Delegates resolved command")
Rel(app, engine, "Runs guarded query")
Rel(engine, db2i, "SELECT/WITH only")
Rel(engine, fs, "Writes exports")
Rel(app, manifest, "Builds + writes manifest")
Rel(manifest, fs, "Writes JSON manifest")
@enduml
```

## Key Design Decisions
- Dry-run remains default for operational safety.
- Read-only SQL guard is enforced before any JDBC execution.
- Config precedence is preserved exactly (`CLI > ENV > file > defaults`).
- Output format extension point is a sealed `OutputWriter` strategy interface.
- Spring Boot is used as a thin runtime shell for configuration/validation support without web stack.
