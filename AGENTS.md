# Multipaz Instructions

## Fork Workflow
- `third_party/multipaz` must track the fork at `git@github.com:dmascord/multipaz.git`.
- Make changes directly in the submodule on a dedicated branch.
- Validate behavior, commit there, and push with `git push fork <branch>`.
- Then stage the submodule SHA in the main repo with `git add third_party/multipaz`.
- Do not leave required Multipaz changes as unpushed local-only edits.
- Remove stale fork branches after they land upstream; do not keep patch files under `scripts/patches/multipaz/`.

## When To Change Multipaz
- Prefer fixing standards and interoperability issues in issuer/verifier server code or config first.
- Use Multipaz overrides only when the issue is outside our control or the change is generic client hardening.
- Any new Multipaz override should explain why server-side remediation was insufficient and what must happen to remove the override later.

## Related Project Learnings
- For local iOS builds, `MULTIPAZ_EXTERNAL_DIR` defaults to `third_party/multipaz`.
- When debugging mobile flows that touch Multipaz, inspect device logs first.
