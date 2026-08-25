# Safety and Consent Architecture

GridGate coordinates automated phone calls to local service providers during power emergencies. Because outbound phone calls interact with human business owners, safety, transparency, and consent are enforced at the architectural level.

## 1. Two-Gate Execution Model

GridGate separates plan generation from phone call execution into two distinct gates.

1. **Gate 1: Dry-Run Plan (`POST /api/runs`)**
   - Creates the sequential dial plan in `PLAN_READY` status.
   - Masks all phone numbers in API responses and persisted records.
   - Places zero phone calls and consumes zero CALL-E credits.
   - Allows users to inspect provider order, budget ceiling, and prompt parameters.

2. **Gate 2: Explicit Live Arming (`POST /api/runs/{id}/live`)**
   - Requires explicit human consent before any outbound phone call is initiated.
   - Transitions the run to `PENDING` and begins sequential dialing.
   - Live calls cannot start without this secondary confirmation.

```
[User Request]
       │
       ▼
┌──────────────┐
│ Gate 1: Plan │  ──>  status: PLAN_READY (masked phones, 0 calls placed)
└──────────────┘
       │
       ▼  (Explicit Human Consent)
┌──────────────┐
│ Gate 2: Live │  ──>  status: RUNNING (sequential CALL-E dials)
└──────────────┘
```

## 2. AI Identity Disclosure First

Every call placed by GridGate begins with an explicit AI disclosure statement.

- The caller identifies itself as an AI assistant within the opening five seconds.
- The assistant states the customer suburb and Eskom load-shedding Stage.
- The assistant states the service need and deadline clearly.

Example task prompt opening:
> "IMPORTANT: You are an AI assistant calling on behalf of a customer. Identify yourself as an AI at the very start of the call."

## 3. Privacy and Phone Number Masking (POPIA)

To protect personal identifiable information (PII) under South Africa's Protection of Personal Information Act (POPIA):

- All provider phone numbers returned by public API endpoints (`GET /api/runs`, `GET /api/runs/{id}`, SSE streams) are masked (for example: `+1415****101`).
- E.164 phone numbers are only stored internally to allow CALL-E dispatch.
- Raw logs do not display full recipient phone numbers.

## 4. No Auto-Booking and No Financial Commitments

GridGate gathers information only. It does not complete bookings or authorize payments.

- The assistant quotes a budget ceiling to determine whether the provider can service within budget.
- The prompt strictly forbids committing to payment, taking credit card details, or confirming a binding agreement over the phone.
- The customer receives structured quotes and spoken evidence to complete the booking directly.

Prompt instruction:
> "BUDGET CEILING: R1800 ZAR (no auto-booking; do not commit to any payment). Do NOT make or imply any booking or payment commitment."

## 5. Fail-Closed Validation and Ambiguity Handling

Provider responses can be noisy during load-shedding emergencies. GridGate applies strict fail-closed criteria:

- If a provider's operating status or price is unclear, CALL-E records the value as `UNKNOWN`.
- GridGate does not guess missing information.
- Ambiguous responses transition the run to `HALTED_AMBIGUOUS` or advance to the next provider, rather than assuming success.
- A run only marks a provider as `SUCCESS` when service availability is confirmed, operating status during load-shedding is verified, and the quoted price is within budget.

## 6. Immediate Cancellation Support

Users can cancel an active run at any point using `POST /api/runs/{id}/cancel`.

- Transitions the run to `CANCELLED`.
- Prevents any subsequent providers in the sequence from being called.
- Idempotent: repeated cancel requests return `200 OK`.
