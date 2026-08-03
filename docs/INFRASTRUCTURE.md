# Infrastructure & hosting costs

*Written because the Squarespace → Cloudflare migration is partly a cost
exercise, and the phase-2 feature-request portal must not quietly undo the
saving.*

Last updated: 2026-08-03

> **Verify before committing spend.** Prices below were correct at the time of
> writing and are a planning aid, not a quote. Check
> <https://www.cloudflare.com/plans/> and the Workers/D1 pricing pages before
> acting on them.

---

## The headline

**You do not need a server.** Not a VPS, not a container host, not a managed
database. The whole feature-request portal fits inside Cloudflare's free tier,
and the realistic worst case is **$5/month**.

| Scenario | Monthly |
|---|---|
| Portal live, normal personal traffic | **$0** |
| Same, but you want Workers Paid for headroom and no daily caps | **$5** |
| Squarespace today (for comparison) | ~$16–23 |

Moving the site and adding the portal should still land **$190–270/year cheaper**
than the current Squarespace bill.

---

## Recommended stack

Everything on Cloudflare — one vendor, one dashboard, no servers to patch.

| Component | Role | Free tier | Paid |
|---|---|---|---|
| **Pages** | The static site itself | Unlimited requests and bandwidth, 500 builds/month | Not needed |
| **Workers** | `POST /api/requests`, `GET /api/requests?since=` | 100,000 requests/day | $5/mo → 10M requests |
| **D1** | SQLite storage for submitted requests | 5 GB storage, 5M row-reads/day | Included in Workers Paid |
| **Turnstile** | Bot/spam protection on the form | Unlimited | Free |
| **R2** | Attachments, only if ever wanted | 10 GB storage, **zero egress fees** | $0.015/GB-month |
| **Registrar** | Domain | — | At cost, ~$10/yr, no markup |

### Why this is the right shape for this problem

A feature-request form is close to the ideal serverless workload: traffic is
spiky and low, requests are tiny, and there is no long-running process to keep
warm. Paying monthly for an always-on box to serve a handful of form posts a
week is paying for idle time.

The operational argument matters more than the money, though. A VPS is a thing
you own: OS updates, TLS renewal, database backups, disk filling up, and a
2 a.m. reboot that takes the form offline until you notice. Workers + D1 has
none of that. For a single-person side project, the maintenance burden is the
real cost.

### Sizing sanity check

100,000 Worker requests **per day** on the free tier. A personal site's feature
form realistically sees single-digit submissions per week. The app polling for
new requests hourly is ~720 requests/month. The free tier is roughly three
orders of magnitude larger than needed — the $5 plan is headroom and peace of
mind, not a requirement.

D1's 5 GB free storage against feature requests measured in kilobytes: not a
constraint in any realistic future.

---

## What the portal actually needs

```
Visitor → Pages (static form + Turnstile)
              │  POST /api/requests
              ▼
          Worker ──► D1  (validate, rate-limit, store)
              ▲
              │  GET /api/requests?since=…   (admin token)
              │
       ProjectMate app (WorkManager, hourly pull)
```

Three endpoints, one table:

- `POST /api/requests` — public, Turnstile-verified, rate-limited by IP
- `GET /api/requests?since=` — **token-protected**, returns the JSON described
  in [`FEATURE_REQUEST_SCHEMA.md`](FEATURE_REQUEST_SCHEMA.md)
- `POST /api/requests/:id/ack` — optional, marks a request as pulled

### Security notes

- The `GET` endpoint must require a secret token stored as a Worker secret
  (`wrangler secret put`) — never in the repo, never in the app's source. It
  exposes submitter email addresses.
- Turnstile on the form, plus a Worker-side rate limit, keeps the D1 table from
  being filled with junk.
- Collect the minimum: a name and an optional email. Say so on the form.

---

## The VPS alternative (not recommended)

Kept here so the decision stays revisitable rather than assumed.

You'd want a VPS if you ever need long-running background jobs, a real Postgres,
Docker workloads, or you want to be portable off Cloudflare entirely.

| Option | Cost | Notes |
|---|---|---|
| **Hetzner CX22** (2 vCPU, 4 GB) | ~€4.49/mo (~$4.90) | Best price/performance; raised from €3.29 in April 2026 |
| **Fly.io** shared-cpu-1x, 256 MB | ~$2–5/mo | Scales to zero; Postgres extra |
| **Oracle Cloud Always Free** (ARM, 4 cores / 24 GB) | $0 | Genuinely free but capacity is often unavailable, and accounts get reclaimed if idle |
| **DigitalOcean / Linode** basic droplet | $4–6/mo | Predictable, well-documented |

Even at $4/month the cash difference is small. The reason to say no is the
ongoing ownership: patching, backups, uptime, and TLS. **Recommendation: skip
it.** If phase 3 sync ever outgrows Workers + D1, revisit — and record the
change in [`DECISIONS.md`](DECISIONS.md).

---

## Migration checklist (Squarespace → Cloudflare)

- [ ] Inventory current pages, forms and redirects before cancelling anything
- [ ] Export any Squarespace form submissions worth keeping — they're lost after
- [ ] Move the domain to Cloudflare Registrar (at cost) or at least point DNS
- [ ] Rebuild the site as static and deploy to Pages from this GitHub org
- [ ] Map old URLs → new with Pages `_redirects` so existing links survive
- [ ] Verify TLS, `www` → apex redirect, and email DNS (MX/SPF/DKIM) **before**
      the Squarespace renewal date
- [ ] Only then cancel Squarespace

Email is the classic trap: Squarespace bundles Google Workspace for some plans.
Confirm where mail lands before switching DNS.

---

## Cost review

Revisit whenever a component is added, when Cloudflare changes pricing, or
annually at renewal. Any change that introduces a recurring cost gets recorded
here **and** as an entry in [`DECISIONS.md`](DECISIONS.md).
