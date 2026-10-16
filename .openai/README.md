# AI project notes

This directory stores internal AI-assisted development context, planning documents and generated task artifacts for GuessTheNumber.

- [Project memory](MEMORY.md): current architecture, decisions, known issues and continuation context, including the standing rule to maintain Android while adding Windows support.
- [Project goals and working rules](promptgoal.md): instructions for future development work.
- [Global localization implementation plan](GLOBAL_LOCALIZATION_IMPLEMENTATION_PLAN.md).
- [KMP shared domain boundary](plans/kmp-shared-domain-boundary.md): shared JVM/Android save model, exact-decimal architecture and the canonical cross-platform app-icon requirement.
- [Historical generated artifacts](artifacts/): task plans, walkthroughs and related notes.
- [Roadmaps and technical plans](docs/): roadmaps and planning/design documents moved from the public-facing docs directory.
- [Windows Feature Parity Plan](../docs/WINDOWS_FEATURE_PARITY_PLAN.md): the single source of truth for the exact Android-to-Windows port, shared KMP code/assets and parity gates.

Obsolete one-off generated plans and task checklists are removed when superseded. Historical artifacts are retained under artifacts/.

Keep user-facing documentation such as the root README, release changelog and gameplay/content documentation in their existing locations. Never store secrets, API keys, passwords, tokens or credentials.