# Contributing to Kriptofolio

Start with [AGENTS.md](AGENTS.md) for protected historical branches, development commands,
data safety and release restrictions. It is the canonical project instruction for people and agents.
You do not need a private method repository, paid playbook or the author's review service to contribute.

1. Agree on a small change and its acceptance criteria. Preserve the original tutorial branches.
2. Create a separate branch and working copy from the intended target branch. Do not mix another
   contributor's work into the change.
3. Implement the change and run the applicable build and test commands from `AGENTS.md` with
   the documented JDK 21 and a configured Android SDK. Keep the exact commands, toolchain,
   actual results and any checks you could not run in the pull request. The two narrow lint
   exceptions and their reasons are in `app/lint.xml`; do not add a baseline. Documentation-only
   evidence should check the instructions and links, without claiming an Android build was run.
4. Open a pull request with the problem, resulting behavior, evidence and remaining limitations.
5. Check the pull request's review tier. The paths it changes decide the tier, and the
   [review tier workflow](.github/workflows/review-tier.yml), which holds the exact path lists,
   shows it as a label:
   - **`review: docs`**: only documentation that is not a rule, such as `README.md`, other
     Markdown, files under `docs/` and store listing texts and images. No independent review is
     needed: the evidence checks the links and facts, `git diff --check` is clean, and the owner
     reads the change before merging.
   - **`review: rules`**: any file that instructs people and agents or defines the contract and
     the plan: `AGENTS.md`, `CLAUDE.md`, `CONTRIBUTING.md`, `RELEASING.md`, `UPGRADE-NOTES.md`,
     `SUPPORT.md`, `LICENSE`, `config/`, `.github/`, `docs/decisions/`, `docs/roadmap.md`,
     `docs/BEHAVIOUR.md`, `docs/behaviour-parity.md` and `docs/ui-inventory.md`.
   - **`review: code`**: anything else.

   For `rules` and `code`, ask a teammate or a fresh agent session to review the exact HEAD using
   the target branch's [review rules](config/review-rules.md). Record the reviewed SHA, concrete
   findings and scope. Automated review is optional; absence of a bot is not a passing review.
   A label never lowers what the paths require; if the label is missing or wrong, the paths decide.
6. Fix findings, rerun relevant checks and, for `rules` and `code`, review the new revision. Merge
   requires authorization; signing and publishing an application release remain separate actions.

```mermaid
flowchart LR
    S[Small agreed change] --> B[Branch and implementation]
    B --> T[Applicable checks and evidence]
    T --> P[Pull request]
    P --> Q{Review tier from changed paths}
    Q -->|docs| O[Owner reads; links and facts checked]
    Q -->|rules or code| R[Independent review of exact HEAD]
    R --> F{Fixes needed?}
    F -->|Yes| B
    F -->|No| M[Authorized merge]
    O --> M
```

The public repository is the source of application code and reproducible project evidence.
Any educational product built around it is separate and does not grant merge or release permission.
