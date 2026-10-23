# Answering review findings

For the implementer: how to act on an agent review's findings and answer them. `REVIEW.md` holds
the reviewer's rules. You need only its `[escalate]` conditions, and only when a `[fix]` finding
looks like it needs a decision.

Answer every finding as **fixed** (cite the commit) or **deferred** (give the reason):

- `[fix]`: fix it without asking, then answer on its thread. If it turns out to be wrong, or fixing
  it meets an `[escalate]` condition, don't fix it: answer it as deferred and say why.
- `[escalate]`: don't implement it. Answer on its thread as deferred to the supervisor. Once the
  supervisor decides and the fix is committed, answer there again as fixed, with the commit and
  what was decided.
- `[note]`: no action. Answer all notes together in one pull request comment.
- A finding with no disposition counts as `[escalate]`. A supervisor reply on a finding sets its
  disposition.
- A local review has no threads. Its findings go in `pr-<number>-review-findings.md` at the
  repository root, which is never committed, and each answer goes under its finding there.
