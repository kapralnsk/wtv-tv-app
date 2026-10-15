# Review instructions

How agent reviews of this repository decide what to report and who acts on each finding. This
covers the pull request review by `claude[bot]` and local `/code-review` runs. The constitution's
Agent review rules still apply. On conflict, the constitution wins and this file is corrected.

The supervisor's review time is the project's bottleneck (constitution Principle VII), and that
includes the time spent reading reviews. A finding the supervisor has to read but nobody needs to
act on is a cost, not a contribution.

## What a finding needs

- **Failure scenario**: the concrete inputs or state, and the wrong result. A claim that something
  "may", "might", or "could" break, without a scenario, is not a finding.
- **Evidence**: a `file:line` citation for each claim about behavior, or a command you ran and
  its output. Do not infer behavior from names, comments, or documentation.
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
   whose correct behavior is not in question: a crash, a race, data loss, a leaked secret, or a
   broken build or test.
3. You confirmed it in the code or by running it.
4. There is one reasonable fix, and you state it.
5. The fix stays within the files and behavior this pull request already changes.
6. The implementer can verify the fix without a device, a login, or the supervisor.

### `[escalate]`: the supervisor decides

Any one of these is enough:

- The fix changes intended behavior or needs a `spec.md` edit (constitution Principle I).
- The fix edits the constitution or a `spec.md`, or it edits AGENTS.md, this file, or
  `.devcontainer/` in a pull request that does not already change them.
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
- Additions the spec does not ask for: abstractions, options, configurability, or generality
  (Principle VII).
- Cases the product rules out: devices other than the two targets, input other than the D-pad,
  and anything in the spec's Out of scope section.
- A missing protection the repository does not claim, or one its documentation lists as not
  covered. A gap in a protection it does claim is a finding.

## Re-reviews

When this pull request already has a review from you:

- Say for each earlier `[fix]` and `[escalate]` finding whether it is resolved.
- Review only the changes since that review. Do not re-audit unchanged code.
- Report new `[fix]` and `[escalate]` findings only. Add no new notes.
- Do not raise again a finding that was answered as deferred, unless the new changes affect it.

## Summary

The first line counts findings by disposition, for example
`fix 1 · escalate 2 · note 3 (+4 not listed)`. If there is no `[fix]` or `[escalate]` finding, the
first line is `Nothing to fix or decide.` Then list the escalation questions.

Order findings `[fix]`, `[escalate]`, `[note]`, most severe first within each.
