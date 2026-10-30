# Review instructions

How agent reviews of this repository decide what to report and who acts on each finding. This
covers the pull request review, posted as `github-actions[bot]`, and local `/code-review` runs. The
constitution's Agent review rules still apply. On conflict, the constitution wins and this file is
corrected.

The supervisor's review time is the project's bottleneck (constitution Principle VII), and that
includes the time spent reading reviews. A finding the supervisor has to read but nobody needs to
act on is a cost, not a contribution.

## What a finding needs

- **Failure scenario**: the concrete inputs or state, and the wrong result. A claim that something
  "may", "might", or "could" break, without a scenario, is not a finding.
- **Evidence**: a `file:line` citation for each claim about behavior, or the output of a
  read-only command such as `git grep`. Never run the code under review. Do not infer behavior
  from names, comments, or documentation.
- **Disposition**: `[fix]`, `[escalate]`, or `[note]`, by the rules below. Start the finding with
  it.
- **Done when**: for `[fix]` only, a check the implementer can run that shows the fix works. When
  only real hardware can show it, name that device check instead (condition 6).

## Dispositions

The disposition says who acts. It is separate from severity: a severe finding can still need the
supervisor's decision.

### `[fix]`: the implementer fixes it without asking

All of these hold:

1. This pull request introduces it.
2. It breaks what the feature's spec, the constitution, or AGENTS.md requires, or it is a defect
   whose correct behavior is not in question: a crash, a race, data loss, a security hole such as
   a leaked secret or an injection, or a broken build or test.
3. You confirmed it in the code. If you couldn't, it is still `[fix]`: say that it is
   unconfirmed and what would confirm it. The implementer confirms it before fixing it.
4. You state the fix. When more than one is reasonable, state the one you recommend, and each
   alternative with its trade-off in one line.
5. The fix stays within the files and behavior this pull request already changes.
6. The implementer can verify the fix without a login or the supervisor. A fix only real hardware
   can verify is still `[fix]`: the implementer lists its device check in the pull request
   description as unverified on device, and the supervisor runs it later (constitution, On-device
   checks).

### `[escalate]`: the supervisor decides

Any one of these is enough:

- The fix edits a locked path, `.github/workflows/`, or `.devcontainer/`. Locked
  paths are those matched by the `Edit(//workspace/…)` deny rules in the base branch's copy of
  `.devcontainer/managed-settings.json`, not the pull request's. Read it with
  `git show origin/<base>:.devcontainer/managed-settings.json`.
- This pull request is the implementer's and changes a locked path. Raise it even when the
  description says the supervisor asked for the change; the question is whether they did.
- The fix changes the supervisor's workflow or adds recurring cost.
- Verifying the fix needs a login or a supervisor action.

If you cannot decide between `[fix]` and `[escalate]`, use `[escalate]`. Phrase each escalation
as the question the supervisor has to answer. An open `[escalate]` finding blocks the pull request
until the supervisor answers it, so raise one only when a rule above requires it.

A new dependency is not escalated: check its justification against the constitution's
Dependencies rule (TV runtime cost, bundle size, Tizen compatibility). A missing or unconvincing
justification is a `[fix]`: justify it in the plan, or remove the dependency.

### `[note]`: no action required

Anything else worth knowing: readability, naming, a small refactor, extra hardening, or a minor
problem that predates this pull request. List at most three notes. If you found more, give their
count in the summary instead.

A serious problem that predates this pull request or lies outside it is also a `[note]`, outside
the three-note limit. Say that it is serious: the implementer records it as an issue (Principle
VII: adjacent work is recorded, not built).

## Spec changes

The agent owns specs and writes them from the supervisor's roadmap, `docs/phases.md`
(constitution Principle I). For a pull request that changes a `spec.md`, this review is the only
check of that spec against the supervisor's intent. Read the roadmap from the base branch, with
`git show origin/<base>:docs/phases.md`, and the agent decisions from the spec's Clarifications
section (AGENTS.md, Agent decisions). Report each of these as `[fix]`:

- A requirement that contradicts the roadmap, or something the roadmap puts in this spec's scope
  that the spec leaves out, with no agent decision recording why.
- An agent decision whose reason doesn't hold: the roadmap passage, SmartTwitchTV behavior, or
  observation it cites doesn't say what it claims, or it departs from the roadmap for a reason
  Principle I doesn't accept, such as effort or simplicity.
- A removed `[NEEDS CLARIFICATION]` marker, or a behavior change to an existing spec, with no
  agent decision recording it.

Done when the spec follows the roadmap there, or an agent decision records a departure whose
reason holds. A departure whose recorded reason holds is not a finding.

## Do not report

- Anything a formatter, linter, or type checker in this repository already enforces.
- Suggestions the constitution rules out. If the change itself does what the constitution rules
  out, that is a finding.
- A risk the documentation explicitly accepts, for example by listing it as not covered. A
  security hole the change itself opens is always a finding, whether or not anything claims the
  protection. An extra protection layer that nothing claims is a `[note]` at most.

## Re-reviews

When this pull request already has a review from you:

- Say for each earlier `[fix]` and `[escalate]` finding whether it is resolved.
- Review only the changes since that review. Do not re-audit unchanged code.
- Put new notes only on lines changed since that review.
- Do not raise again a finding that was answered as deferred, unless the new changes affect it.

## Summary

The first line counts findings by disposition, for example
`fix 1 · escalate 2 · note 3 (+4 not listed)`. Then list the escalation questions.

The pull request has no issues when this review has no `[fix]` or `[escalate]` finding, and every
earlier one is resolved:

- fixed by the change;
- for a `[fix]` answered as deferred, the reason holds: the finding was wrong, or fixing it meets
  an `[escalate]` condition, which you then raise as `[escalate]`;
- for an `[escalate]`, the supervisor has decided on its thread, and the change follows that
  decision.

Then the first line is `Nothing to fix or decide.`, and every thread the review has opened, notes
included, is resolved. On `experiment/autonomy`, such a pull request is approved and then merged
with no human review, so give that line only when it is earned.

Order findings `[fix]`, `[escalate]`, `[note]`, most severe first within each.
