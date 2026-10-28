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
- **Done when**: for `[fix]` only, a check the implementer can run that shows the fix works.

## Dispositions

The disposition says who acts. It is separate from severity: a severe finding can still need the
supervisor's decision.

### `[fix]`: the implementer fixes it without asking

All of these hold:

1. This pull request introduces it.
2. It breaks what the feature's spec, the constitution, or AGENTS.md requires, or it is a defect
   whose correct behavior is not in question: a crash, a race, data loss, a security hole such as
   a leaked secret or an injection, or a broken build or test.
3. You confirmed it in the code.
4. There is one reasonable fix, and you state it.
5. The fix stays within the files and behavior this pull request already changes.
6. The implementer can verify the fix without a device, a login, or the supervisor.

### `[escalate]`: the supervisor decides

Any one of these is enough:

- The fix changes intended behavior (constitution Principle I).
- The fix edits a `spec.md`, or a path the implementer may not change: the constitution,
  AGENTS.md, this file, `docs/answering-reviews.md`, `LICENSE.md`, `.claude/skills/`,
  `.github/workflows/`, or `.devcontainer/`.
- The fix changes the supervisor's workflow, adds a dependency, or adds recurring cost.
- More than one fix is reasonable. Give each with its trade-off, and say which you recommend.
- Verifying the fix needs real hardware, a login, or a supervisor action.
- It would be `[fix]` if true, but you could not confirm it.
- It is serious, but it predates this pull request or lies outside it (Principle VII: adjacent
  work is recorded, not built).

If you cannot decide between `[fix]` and `[escalate]`, use `[escalate]`. Phrase each escalation
as the question the supervisor has to answer.

### `[note]`: no action required

Anything else worth knowing: readability, naming, a small refactor, extra hardening, or a minor
problem that predates this pull request. List at most three notes. If you found more, give their
count in the summary instead.

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
earlier one is resolved: fixed by the change, or its answer accepted by the supervisor on its
thread. Then the first line is `Nothing to fix or decide.`, and every thread the review has
opened, notes included, is resolved.

Order findings `[fix]`, `[escalate]`, `[note]`, most severe first within each.
