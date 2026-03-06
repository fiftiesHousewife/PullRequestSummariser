# Pull Request Summary Tool

A two-step pipeline for extracting GitHub pull request data and generating AI-ready markdown summaries. Designed for air-gapped corporate environments where AI tools don't have internet access.

```
Step 1 (online)                         Step 2 (offline)
┌──────────────────────┐  JSON files   ┌──────────────────────────────┐
│  PullRequestExtract   │ ──────────>  │  PullRequestSummarize        │
│  (GitHub API)         │              │  (local processing)          │
└──────────────────────┘              └──────────────────────────────┘
```

**Step 1** runs on an internet-connected machine to pull data from GitHub's REST API and save it as JSON files. **Step 2** runs offline to parse the JSON and produce self-contained markdown summaries you can paste directly into an AI chat.

## Prerequisites

- Java 21+
- A [GitHub Personal Access Token](https://github.com/settings/tokens) (for extraction)

## Build

```bash
./gradlew build
```

## Usage

### Step 1: Extract pull request data

Set your GitHub token and run the extractor:

```bash
export GITHUB_TOKEN=ghp_your_token_here
```

Extract from all repos for a user:

```bash
./gradlew :PullRequestSummary:run --args="extract --user fiftieshousewife"
```

Extract from a specific repo:

```bash
./gradlew :PullRequestSummary:run --args="extract --repo fiftieshousewife/my-repo"
```

Options:

| Flag | Default | Description |
|------|---------|-------------|
| `--user <name>` | | Extract from all repos owned by this user |
| `--repo <owner/repo>` | | Extract from a specific repo |
| `--state <all\|open\|closed>` | `all` | Filter pull requests by state |
| `--limit <n>` | unlimited | Max pull requests to extract per repo |

Output is written to `output/{owner}_{repo}/`:

```
output/
  fiftieshousewife_my-repo/
    pr_1.json
    pr_2.json
    extraction_meta.json
```

### Step 2: Generate summaries

This step is fully offline — copy the `output/` directory to your air-gapped environment and run:

```bash
./gradlew :PullRequestSummary:run --args="summarize --input output/fiftieshousewife_my-repo/"
```

Summarize multiple repos at once:

```bash
./gradlew :PullRequestSummary:run --args="summarize --input output/fiftieshousewife_repo1/ output/fiftieshousewife_repo2/"
```

Options:

| Flag | Default | Description |
|------|---------|-------------|
| `--input <dirs...>` | required | One or more directories containing extracted JSON |
| `--max-diff-lines <n>` | `500` | Truncate diffs beyond this many lines |

Output is written to `summaries/{owner}_{repo}/`:

```
summaries/
  fiftieshousewife_my-repo/
    pr_1_summary.md        # per-pull-request summary
    pr_2_summary.md
    repo_summary.md        # aggregate repo summary
```

## What gets extracted

For each pull request, the extractor pulls:

- **Metadata** — title, number, state, author, dates, labels, milestone
- **Description** — the pull request body
- **Commits** — messages, authors, timestamps
- **File changes** — filenames, additions, deletions, full diffs
- **Review comments** — inline code review feedback
- **Issue comments** — general discussion
- **Reviews** — approval/rejection decisions

## What the summaries contain

Each per-pull-request summary is a self-contained markdown document with:

- **Overview** — title, author, dates, status, branch info
- **Intent** — description and commit messages
- **Scope of changes** — files modified, lines changed, languages
- **Key diffs** — actual code changes (truncated if large)
- **Discussion** — review decisions, code comments, general discussion
- **Verdict** — final status (merged, closed, or still open)

The repo-level summary aggregates:

- Pull request statistics and merge rate
- Active contributors
- Areas of work by directory
- Languages touched
- Timeline of all pull requests

## Rate Limits

The extractor uses authenticated GitHub API requests (5,000 requests/hour). Remaining rate limit is logged after each repo extraction and saved in `extraction_meta.json`.

## Tests

```bash
./gradlew :PullRequestSummary:test
```
