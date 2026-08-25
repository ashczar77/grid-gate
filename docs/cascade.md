# Cascade State Machine and Orchestration

GridGate runs a sequential phone cascade. Instead of calling multiple businesses simultaneously, it dials providers one by one. It stops immediately at the first provider that confirms availability within budget and before the deadline.

## 1. Lifecycle State Machine

A GridGate run progresses through well-defined lifecycle states:

```
                  ┌──────────────┐
                  │  PLAN_READY  │  (Gate 1: Dry-run plan created)
                  └──────┬───────┘
                         │
                         │ POST /api/runs/{id}/live (Gate 2 Consent)
                         ▼
                  ┌──────────────┐
                  │   PENDING    │
                  └──────┬───────┘
                         │
                         │ First dial started via CALL-E
                         ▼
                  ┌──────────────┐
                  │   RUNNING    │ ◄─── Next provider dialed (Webhook step=CONTINUE)
                  └──────┬───────┘
                         │
        ┌────────────────┼────────────────┬────────────────┐
        │                │                │                │
        ▼                ▼                ▼                ▼
 ┌─────────────┐  ┌─────────────┐  ┌──────────────┐  ┌───────────┐
 │  FULFILLED  │  │  EXHAUSTED  │  │   HALTED_    │  │ CANCELLED │
 │  (Winner)   │  │ (No match)  │  │  AMBIGUOUS   │  │ (User API)│
 └─────────────┘  └─────────────┘  └──────────────┘  └───────────┘
```

### State Definitions

| State | Description |
|---|---|
| `PLAN_READY` | Initial state. Plan created with masked phone numbers. Zero calls placed. |
| `PENDING` | Live arming consented. The run is ready for the first dial. |
| `RUNNING` | An outbound phone call is actively in progress through CALL-E. |
| `FULFILLED` | Terminal state. A provider confirmed availability within budget. Cascade stopped early. |
| `EXHAUSTED` | Terminal state. All providers were dialed, but none could fulfill the request. |
| `HALTED_AMBIGUOUS` | Terminal state. A callee gave conflicting or uninterpretable answers requiring human review. |
| `CANCELLED` | Terminal state. The user explicitly stopped the run via `POST /api/runs/{id}/cancel`. |

## 2. Hard Success Criteria

GridGate applies strict rule evaluation to every completed call result:

1. **Can Service:** The callee explicitly answered `YES`.
2. **Operating During Outage:** The callee confirmed they operate during the requested Eskom Stage (`YES`).
3. **Price Check:** Quoted price is less than or equal to the budget ceiling.
4. **Deadline Check:** Confirmed ETA or cutoff is before the specified deadline.

When all four conditions are met, the orchestrator returns `CascadeStep.FULFILLED`. The run records the winning provider ID, transitions to `FULFILLED`, and skips all remaining providers.

## 3. Early Exit and Resource Efficiency

Calling five service providers in parallel wastes callee time and burns platform credits. GridGate enforces sequential execution:

- Provider 1 is called first.
- If Provider 1 answers `REJECTED` or is `UNREACHABLE`, GridGate advances to Provider 2.
- If Provider 2 answers `YES` with a quote within budget, GridGate marks the run `FULFILLED`.
- Providers 3, 4, and 5 are never called.

## 4. Idempotency and Webhook Deduplication

Network retries and duplicate webhooks must not trigger duplicate phone calls. GridGate uses a two-layer deduplication design.

### Call Placement Idempotency
When initiating a call via CALL-E, GridGate includes a deterministic idempotency key in the `Idempotency-Key` HTTP header:

```
gridgate_{runId}_{providerId}
```

If the API call is retried due to a transient network timeout, CALL-E recognizes the key and avoids creating a duplicate call.

### Webhook Event Deduplication
CALL-E sends completion payloads to `POST /calle/webhook` with a unique `CALL-E-Event-Id` header.

1. GridGate checks the `processed_webhook_events` database ledger before processing.
2. If the event ID already exists, the controller returns `200 OK` immediately with `{"status": "duplicate_ignored"}`.
3. If new, the event ID is stored in the database, the attempt result is recorded, and the cascade advances to the next step.
