# Answering review findings

For the implementer: how to act on an agent review's findings and answer them. `REVIEW.md` holds
the reviewer's rules. You need only its `[escalate]` conditions, and only when a `[fix]` finding
looks like it needs a decision.

Answer every finding as **fixed** (cite the commit) or **deferred** (give the reason):

- `[fix]`: fix it without asking, then answer on its thread. If it turns out to be wrong, or fixing
  it meets an `[escalate]` condition, don't fix it: answer it as deferred and say why.
- `[escalate]`: don't implement it. Raise it as blocked work (AGENTS.md) and answer on its thread
  as deferred to the supervisor, linking the issue. Once the supervisor decides and the fix is
  committed, answer there again as fixed, with the commit and what was decided.
- `[note]`: act on it or not, as you judge. Answer all notes together in one pull request
  comment, saying for each whether you acted on it. A note marked serious is recorded as an issue,
  linked in that comment.
- A finding with no disposition counts as `[escalate]`. A supervisor reply on a finding sets its
  disposition.
- Never resolve a review thread. The reviewer resolves its own once it finds the pull request has
  no issues, and the supervisor resolves theirs.
- A local review has no threads. Its findings go in `pr-<number>-review-findings.md` at the
  repository root, which is never committed, and each answer goes under its finding there.
