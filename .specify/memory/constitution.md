<!--
  Sync Impact Report
  ==================
  Version change: [TEMPLATE] → 1.0.0 (MAJOR — initial ratification)
  Modified principles: N/A (first version)
  Added sections:
    - Core Principles (5): Layered Architecture, Security First, Performance & Caching, Test Discipline, Simplicity
    - Technology Stack & Constraints
    - Development Workflow
    - Governance
  Removed sections: N/A
  Templates requiring updates:
    - .specify/templates/plan-template.md ✅ (no changes needed — Constitution Check section is advisory)
    - .specify/templates/spec-template.md ✅ (no changes needed — requirements structure aligns)
    - .specify/templates/tasks-template.md ✅ (no changes needed — phase structure aligns with layered architecture)
  Follow-up TODOs: none
-->

# Blogs Workspace Constitution

## Core Principles

### I. Layered Architecture

All backend code MUST follow the Controller-Service-Mapper three-layer
pattern. Each layer has a single, non-negotiable responsibility:

- **Controller**: HTTP request/response handling, parameter validation,
  calling Service methods. Controllers MUST NOT contain business logic.
- **Service**: Business logic and transaction management. Services MUST
  NOT directly access HTTP context.
- **Mapper**: Data access via MyBatis Plus. Mappers MUST NOT contain
  business logic.

Frontend code MUST separate concerns: routing (`router/`), state
management (`store/`), UI components (`components/`), and API calls
(`utils/axios.ts`). State stores (Pinia) MUST be domain-scoped
(user, article, notification, siteConfig).

Rationale: Clear separation prevents tangled code, enables independent
testing of each layer, and makes the codebase navigable for
contributors.

### II. Security First

Security is non-negotiable at every system boundary:

- Authentication MUST use Spring Security + JWT tokens. The
  `JwtInterceptor` MUST gate all protected endpoints.
- OAuth (GitHub) flows MUST validate state parameters to prevent CSRF.
- All user input MUST be validated before reaching the Service layer.
- AI content moderation (DeepSeek API) MUST run on user-generated
  content (articles, comments) before persistence.
- Sensitive word filtering MUST be applied to all public-facing text.
- SQL injection is prevented by MyBatis Plus parameterized queries;
  string concatenation in queries is FORBIDDEN.
- File uploads MUST be validated for type, size, and scanned before
  storage in TOS.

Rationale: The platform handles user accounts, OAuth tokens, and
public content. A single vulnerability compromises all users.

### III. Performance & Caching

The system MUST use a two-level caching strategy:

- **Redis**: Distributed cache for article view counts and hot data.
- **Caffeine**: Local cache to reduce Redis round-trips.
- Article view counts MUST be synced to the database on application
  shutdown (graceful flush).

Asynchronous processing MUST use Spring Events for:

- `ArticleViewCountChangeEvent` — view count changes
- `ArticleLikeCountChangeEvent` — like count changes
- `NotificationEvent` — notification creation

Access logs (`website_access_log`) MUST be written in batch, not
per-request.

Rationale: The blog serves public traffic; synchronous DB writes on
every view would bottleneck the system.

### IV. Test Discipline

The project defines a health stack that MUST pass before any merge:

- **Type check**: `cd frontend && npx vue-tsc --noEmit`
- **Lint**: `cd frontend && npx eslint .`
- **Test**: `mvn test`
- **Dead code**: `cd frontend && npx knip`

All four checks MUST pass. Failures in any check block the merge.

When fixing a bug, a reproduction test SHOULD be written first to
confirm the fix. When adding a feature, tests SHOULD cover the happy
path and at least one edge case.

Rationale: Untested code is a liability. The health stack catches
regressions before they reach production.

### V. Simplicity

Minimum code that solves the problem. Nothing speculative.

- No features beyond what was asked.
- No abstractions for single-use code.
- No "flexibility" or "configurability" that was not requested.
- No error handling for impossible scenarios.
- Surgical changes only: touch only what is necessary, match existing
  style, do not "improve" adjacent code.

Ask: "Would a senior engineer say this is overcomplicated?" If yes,
simplify.

Rationale: Over-engineered code is harder to maintain than simple code
that occasionally needs a targeted extension.

## Technology Stack & Constraints

The following versions are pinned. Upgrades require explicit approval
and a migration plan.

| Component       | Version   | Notes                          |
|-----------------|-----------|--------------------------------|
| Java            | 21        | LTS, required                  |
| Spring Boot     | 3.5.6     | Backend framework              |
| MyBatis Plus    | 3.5.5     | ORM / data access              |
| Vue             | 3.4+      | Frontend framework             |
| Element Plus    | 2.7+      | UI component library           |
| Vite            | 5.2+      | Frontend build tool            |
| Node.js         | 18+       | Frontend runtime               |
| MySQL           | 8.0+      | Primary database               |
| Redis           | 6.0+      | Distributed cache              |

Key architectural constraints:

- Markdown editor: `md-editor-v3` — do not replace without team review.
- Image compression: frontend Web Worker before TOS upload.
- Theming: CSS variables in `frontend/public/css/theme/light.css` and
  `dark.css`. New themes MUST follow the same variable contract.
- API requests: all go through `frontend/src/utils/axios.ts` with JWT
  auto-attachment.

## Development Workflow

### Backend

```bash
mvn spring-boot:run          # Run locally
mvn clean package -DskipTests # Build
mvn test -Dtest=Class#method # Run single test
mvn compile                   # Compile only
```

### Frontend

```bash
cd frontend
npm install    # Install deps
npm run dev    # Dev server
npm run build  # Production build
npm run preview # Preview build
```

### Database

Execute in order: `database/schema.sql` then `database/data.sql`.

### Code Review Gates

- All PRs MUST pass the health stack (typecheck, lint, test, deadcode).
- All PRs MUST NOT introduce secrets (`.env`, credentials, API keys).
- All PRs touching security-critical paths (auth, file upload, content
  moderation) MUST be reviewed by a second contributor.

## Governance

This constitution is the highest-authority document for the blogs
workspace project. All development practices, code reviews, and
architectural decisions MUST comply with these principles.

**Amendment process**:

1. Propose the change with rationale (issue or discussion).
2. Update this document with the amendment.
3. Increment the version per semantic versioning:
   - MAJOR: principle removal or incompatible redefinition.
   - MINOR: new principle or material expansion.
   - PATCH: wording clarification, typo fix, non-semantic refinement.
4. Propagate changes to dependent templates
   (`plan-template.md`, `spec-template.md`, `tasks-template.md`).
5. Commit with message: `docs: amend constitution to vX.Y.Z (reason)`.

**Compliance**: All PRs and code reviews MUST verify adherence to the
Core Principles above. Violations MUST be justified in the
"Complexity Tracking" section of the implementation plan.

**Runtime guidance**: Use `CLAUDE.md` for day-to-day development
instructions. This constitution supersedes `CLAUDE.md` on matters of
architecture and principle.

**Version**: 1.0.0 | **Ratified**: 2026-05-29 | **Last Amended**: 2026-05-29
