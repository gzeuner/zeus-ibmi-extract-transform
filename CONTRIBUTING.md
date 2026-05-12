# Contributing

## Prerequisites
- Java 21+
- Maven 3.9+

## Local Workflow
1. Create a feature branch.
2. Implement with tests.
3. Run:
   - `./mvnw spotless:apply`
   - `./mvnw verify`
4. Open a PR with migration notes and behavior-impact summary.

## Commit Style
Use conventional commits:
- `feat:` new feature
- `fix:` bug fix
- `refactor:` structural changes without behavior changes
- `test:` test-only changes
- `docs:` documentation changes
- `build:` build/CI/dependency changes

## Quality Gates
- Keep dry-run safety and read-only guard intact.
- Never introduce credential leakage in logs/manifests.
- Keep CLI and configuration backward-compatible unless explicitly approved.
- JaCoCo minimum coverage gate is enforced in `mvn verify`.

## Testing Strategy
- Unit tests for config, guardrails, output writers, and manifest logic.
- Integration tests using H2 `MODE=DB2`.
- Optional future Testcontainers profile for IBM i compatible environments.

## Security
- Do not commit real credentials.
- Prefer env-based secret resolution.
- If a change affects masking/security behavior, include focused tests.