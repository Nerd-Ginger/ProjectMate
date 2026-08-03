# Changelog

All notable changes to ProjectMate. Format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

Anything a user would notice gets a line here, in the same commit that ships it.

## [Unreleased]

### Added
- Feature-request import: reads the JSON contract the website will serve, with
  per-request error reporting so one bad entry never loses the rest of a batch,
  and deduplication so re-importing the same export changes nothing
- Repository scaffolding: `.gitignore`, `.editorconfig`, and contributor
  conventions in `CLAUDE.md`
- Documentation set: purpose and non-goals, roadmap, architecture, data model,
  the feature-request JSON contract, infrastructure costs, and a decision log
