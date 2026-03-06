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
- A GitHub Personal Access Token (for extraction only)

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

Output is written to `output/{owner}_{repo}/` as one JSON file per pull request plus an `extraction_meta.json` file.

### Step 2: Generate Summaries (offline)

```bash
./gradlew :PullRequestSummariser:run --args="summarize --input output/owner_repo/"
```

Optional flags:

- `--max-diff-lines N` — truncate diffs after N lines (default: `500`)

Output is written to `summaries/{owner}_{repo}/` as one markdown file per pull request plus a `repo_summary.md` file.

## What Gets Extracted

Per pull request:
- Metadata (title, number, state, author, dates, labels, milestone)
- Description/body
- Commit list with messages and authors
- File changes with additions, deletions, and patches
- Review comments (inline code review)
- Issue comments (general discussion)
- Review decisions (approved, changes requested, etc.)

## What Gets Summarised

Per pull request markdown:
- **Overview** — title, author, dates, status, labels
- **Intent** — PR description and commit messages
- **Scope of Changes** — files modified, lines changed, languages
- **Key Diffs** — actual code changes (truncated if large)
- **Discussion** — review decisions, code review comments, general comments
- **Verdict** — final status (merged, closed, open)

Repository-level summary:
- Pull request statistics and merge rate
- Contributors ranked by PR count
- Areas of work by top-level directory
- Languages touched
- Timeline and index of all PRs

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
    │   ├── DiscussionMarkdownWriter.java  — review/comment markdown sections
    │   ├── RepoMarkdownWriter.java        — repo-level summary markdown
    │   ├── JsonFields.java                — safe JSON field access utility
    │   └── Languages.java                 — file extension to language mapping
    └── test/java/org/fifties/housewife/
        └── *Test.java                     — JUnit 5 + AssertJ tests
```

## Dependencies

- [Gson](https://github.com/google/gson) — JSON parsing
- Java 11+ `HttpClient` — HTTP requests (no external HTTP dependency)
- JUnit 5 + AssertJ — testing
