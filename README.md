# PullRequestSummariser

A three-step pipeline for extracting GitHub pull request data, generating markdown summaries, and feeding them to an AI for analysis. Designed for air-gapped corporate environments where AI tools cannot access the internet directly.

## Architecture

```
Step 1 (online)              Step 2 (air-gapped)            Step 3 (air-gapped)

┌────────────────────┐       ┌────────────────────┐       ┌────────────────────┐
│  extract           │       │  summarize         │       │  AI analysis       │
│  (GitHub REST API) │ ────> │  (local processing)│ ────> │  (local LLM or     │
│                    │ JSON  │                    │  MD   │   corporate AI)    │
└────────────────────┘       └────────────────────┘       └────────────────────┘
```

Steps 1 and 2 are provided by this tool. Step 3 is performed by pasting the generated markdown summaries into your local AI of choice.

## Prerequisites

- Java 21+
- A GitHub Personal Access Token (PAT) for extraction

### Creating a GitHub Personal Access Token

1. Go to [GitHub Settings → Developer settings → Personal access tokens](https://github.com/settings/tokens)
2. Choose **Fine-grained tokens** (recommended) or **Tokens (classic)**
3. Click **Generate new token**
4. For fine-grained tokens:
   - Set a token name and expiration
   - Under **Repository access**, select the repos you want to extract from (or all)
   - Under **Permissions → Repository permissions**, grant **Pull requests: Read-only**
5. For classic tokens:
   - Select the **`repo`** scope (grants read access to pull requests, commits, files, and comments)
6. Click **Generate token** and copy it immediately — it won't be shown again

Set the token in your terminal before running extraction:

```bash
export GITHUB_TOKEN=ghp_your_token_here
```

## Build

From the repository root:

```bash
./gradlew :PullRequestSummariser:build
```

Run tests:

```bash
./gradlew :PullRequestSummariser:test
```

Check for dependency updates:

```bash
./gradlew :PullRequestSummariser:dependencyUpdates
```

### Self Test

The project includes integration tests that run the full pipeline against its own PR #1 on GitHub. These tests are excluded from the normal build and CI — they only run when explicitly invoked.

```bash
export GITHUB_TOKEN=ghp_your_token_here
./gradlew :PullRequestSummariser:integrationTest
```

This extracts live data from the GitHub API for `fiftiesHousewife/PullRequestSummariser` PR #1, verifies the extracted JSON structure, transforms it to markdown, and asserts the output contains expected sections. A second test does the same via the CSV input path, writing to a temp directory and round-tripping through `loadPullRequests`.

Requires a valid `GITHUB_TOKEN` with read access to this repository.

## Usage

### Step 1: Extract Pull Request Data (online)

Set your GitHub token:

```bash
export GITHUB_TOKEN=ghp_your_token_here
```

Extract from a specific repository:

```bash
./gradlew :PullRequestSummariser:run --args="extract --repo owner/repo"
```

Extract from all repositories for a user:

```bash
./gradlew :PullRequestSummariser:run --args="extract --user username"
```

Extract from a CSV file of pull request URLs:

```bash
./gradlew :PullRequestSummariser:run --args="extract --csv prs.csv"
```

The CSV file should contain one GitHub pull request URL per line (or as the first column):

```
https://github.com/owner/repo-a/pull/1
https://github.com/owner/repo-a/pull/5
https://github.com/other/repo-b/pull/12
```

Pull requests are grouped by repository so that repo-level analysis is performed once per repo. Lines starting with `#` are treated as comments.

Optional flags (for `--repo` and `--user` modes):

- `--state all|open|closed` — filter by PR state (default: `all`)
- `--limit N` — limit the number of PRs extracted

### Rate Limiting

The tool respects GitHub API rate limits. When the remaining quota drops below 10 requests, it automatically pauses until the rate limit resets. A GitHub Personal Access Token (`GITHUB_TOKEN`) is required for all extraction modes.

Output is written to `output/{owner}/{repo}/` as one JSON file per pull request plus an `extraction_meta.json` file.

### Step 2: Generate Summaries (offline)

```bash
./gradlew :PullRequestSummariser:run --args="summarize --input output/owner/repo/"
```

Optional flags:

- `--max-diff-lines N` — truncate diffs after N lines (default: `500`)

Summaries are written alongside the extracted JSON in `output/{owner}/{repo}/` as one markdown file per pull request plus a `repo_summary.md` file.

## Output Format

### Step 1 Output: JSON (one file per pull request)

Each pull request is saved as `output/{owner}/{repo}/pr_{number}.json` containing:

- **Metadata** — title, number, state, author, created/updated/merged/closed dates, labels, milestone
- **Body** — the full pull request description
- **Commits** — SHA, message, author, and date for each commit
- **Files** — filename, status (added/modified/deleted), additions, deletions, and full patch diff
- **Review comments** — inline code review comments with file path, line number, author, and body
- **Issue comments** — general PR discussion comments
- **Reviews** — review decisions (approved, changes requested, etc.) with author and body

An `extraction_meta.json` file is also written per repo with the extraction timestamp and rate limit status.

### Step 2 Output: Markdown Summaries

#### Per-Pull-Request Summary (`pr_{number}_summary.md`)

Each summary is a self-contained markdown document designed to be pasted directly into an AI chat:

```markdown
# Pull Request #1: Add CSV-based pull request extraction with rate limiting

- **Author:** fiftiesHousewife
- **Status:** Open
- **Created:** 2026-03-06T10:20:00Z
- **Updated:** 2026-03-06T10:26:59Z
- **Branch:** `feature/csv-pull-request-extraction` -> `main`

## Intent

- New --csv option accepts a file of GitHub PR URLs (one per line or first CSV column)
- Groups PRs by repository so repo-level extraction meta is written once per repo
...

### Commit Messages

- `bb64d2d` Add CSV-based pull request extraction with rate limiting (Pippa Newbold)
- `cfba89b` Fix CLAUDE.md violations: final fields, abbreviation, accessor methods (Pippa Newbold)
- `636b09f` Add PAT creation instructions to README (Pippa Newbold)

## Scope of Changes

- **Files changed:** 12
- **Lines added:** 558
- **Lines removed:** 57
- **Languages:** Java (10), Markdown (1), .kts (1)

### Files

- `PullRequestSummariser/README.md` — modified (+47/-4)
- `PullRequestSummariser/src/main/java/org/fifties/housewife/CsvPullRequestExtract.java` — added (+112/-0)
...

## Key Diffs

### `PullRequestSummariser/src/main/java/org/fifties/housewife/CsvPullRequestExtract.java`
```diff
+package org.fifties.housewife;
+import com.google.gson.GsonBuilder;
...
```

## Discussion

### Review Decisions
- **reviewer**: Approved — Ship it (2024-01-14T00:00:00Z)

### Code Review Comments
**reviewer** on `src/App.java` line 42 (2024-01-14T00:00:00Z):
> Consider extracting this into a helper method.

### General Comments
**contributor** (2024-01-14T00:00:00Z):
> This looks good to me.

## Verdict

**Still open**
```

Sections are omitted when empty (e.g. no Discussion section if there are no reviews or comments). Diffs are truncated at `--max-diff-lines` (default 500) with a note indicating how many lines were omitted.

#### Repository Summary (`repo_summary.md`)

An aggregate view across all extracted pull requests:

```markdown
# Repository Summary: fiftiesHousewife/PullRequestSummariser

- **Extraction date:** 2026-03-06T10:30:47.402210Z
- **Total pull requests extracted:** 1

## Pull Request Statistics

- **Merged:** 0
- **Closed (not merged):** 0
- **Open:** 1

## Contributors

- **fiftiesHousewife**: 1 pull requests

## Areas of Work (by top-level directory)

- `PullRequestSummariser/`: 12 file changes

## Languages

- **Java**: 10 files changed
- **Markdown**: 1 files changed
- **.kts**: 1 files changed

## Pull Request Timeline

- **2026-03-06** Pull Request #1: Add CSV-based pull request extraction... [Open]

## Pull Request Index

| # | Title | Author | Status | Created |
|---|-------|--------|--------|--------|
| #1 | Add CSV-based pull request extraction... | fiftiesHousewife | Open | 2026-03-06 |
```

## Project Structure

```
PullRequestSummariser/
├── build.gradle.kts
└── src/
    ├── main/java/org/fifties/housewife/
    │   ├── Main.java                      — entry point (extract/summarize)
    │   ├── PullRequestExtract.java        — repo/user extraction orchestrator
    │   ├── CsvPullRequestExtract.java     — CSV-based extraction orchestrator
    │   ├── CsvPullRequestReader.java      — parses CSV file of PR URLs
    │   ├── PullRequestUrl.java            — parses GitHub PR URLs
    │   ├── ExtractArguments.java          — CLI argument parsing for extract
    │   ├── PullRequestSummarize.java      — offline summary orchestrator
    │   ├── GitHubClient.java              — HTTP client with pagination and rate limiting
    │   ├── PullRequestDataMapper.java     — maps API responses to consolidated JSON
    │   ├── PullRequestMarkdownWriter.java — per-PR markdown generation
    │   ├── DiffMarkdownWriter.java        — diff section markdown
    │   ├── DiscussionMarkdownWriter.java  — review/comment markdown sections
    │   ├── RepoMarkdownWriter.java        — repo-level summary markdown
    │   ├── TimelineMarkdownWriter.java    — timeline and index markdown
    │   ├── JsonFields.java                — safe JSON field access utility
    │   └── Languages.java                 — file extension to language mapping
    └── test/java/org/fifties/housewife/
        └── *Test.java                     — JUnit 5 + AssertJ tests
```

## Dependencies

- [Gson](https://github.com/google/gson) — JSON parsing
- [Log4j2](https://logging.apache.org/log4j/2.x/) + [Lombok](https://projectlombok.org/) — logging via `@Log4j2`
- Java 21 `HttpClient` — HTTP requests (no external HTTP dependency)
- JUnit 5 + AssertJ — testing
