# TODO

## Test harness (utilityScripts/)

- [ ] **Open a project automatically.** `/load-sysml` fails with "No active MagicDraw project!" when CATIA Magic has no project open, even with `persist: false`. The harness should open (or create) whatever kind of project it needs: a blank SysML v2 project, kept as a scratch project the harness owns, without touching the user's own projects. Requested by Daniel, 2026-11-27.
