### Background

What do your changes do?

### Testing

What did you test? What are the steps to reproduce? Which devices did you test on?

### Meta

- Jira Ticket(s): [AWA-XXXX](https://arcpublishing.atlassian.net/browse/AWA-XXXX)

### Related Pull Requests

Please provide links to related PRs if any

### Developer Checklist

- [ ] Title is in format `Feature:<ticket> <description>`, `Fix:<ticket> <description>`.
- [ ] A meaningful description of the change is provided.
- [ ] Screenshot is added for UI changes.
- [ ] Testing is described and has been done.
- [ ] My changes are backwards compatible.
- [ ] A link to the Jira ticket is provided.
- [ ] PR targets `develop` branch, a feature branch, or a `hotfix/vXYZ` branch.
- [ ] If the PR is not ready for merge (work in progress, has a blocker), add the corresponding label to it (`not ready for merge`) and describe what work is remaining.
- [ ] PR changes `raw/config.json` and requires prod configs update (https://github.com/WashPost/android-prod-configs) and PR is linked.
- [ ] If the PR includes any analytics related changes, sign off has been received from one of the members from the analytics team.
- [ ] If the PR includes any ads related changes, sign off has been received from one of the members from the ads team.
- [ ] My PR has A/B test changes and if so, the test param key has been added to FirebaseConfigListener.ABTests.ACTIVE_AB_PARAMS.

### Reviewer Checklist
- [ ] Developer checklist is complete.
- [ ] Test coverage has been evaluated and is sufficient (does it meet the goal of the project?).
- [ ] Code is clean, well documented, meets architectural standards, and follows the style guide.