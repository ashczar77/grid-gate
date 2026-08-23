# GridGate

An AI phone agent that finds available providers when unreliable power makes normal booking impossible, powered by [CALL-E](https://www.heycall-e.com/).

GridGate calls providers one at a time when outages make it unclear who is still operating. You set a deadline, budget, and list of numbers. GridGate uses CALL-E to place the calls, stops at the first provider that can help, and returns structured results you can act on. Dry-run mode is on by default; live calls require an explicit second step.

Built for the [CALL-E: Your Code Is Calling](https://call-e.devpost.com/) hackathon.

## How it works during outages

GridGate runs on cloud infrastructure, not on your home laptop. CALL-E places the calls from its platform too. Load shedding at your house is the problem GridGate helps with, not a reason the service stops.

You submit a run (deadline, budget, provider list) while you have connectivity. That can be before the slot, over mobile data when Wi‑Fi is down, or from a scheduled job. GridGate calls each provider and asks whether they can still operate during the outage window. Many businesses answer from backup power. GridGate stops at the first firm yes within budget and returns the quote and what they said on the call.

## Stack

- Java 21
- Spring Boot 3
- CALL-E Developer API (REST)
- H2 (local development)

## Status

Phase 1 in progress: scaffold, domain model, and cascade success rules.

## Quick start

```bash
export CALLE_API_KEY="your_key"

mvn test
mvn spring-boot:run
```

The app listens on `http://localhost:8080`. HTTP routes are added in later phases.

## Configuration

| Variable | Description |
|----------|-------------|
| `CALLE_API_KEY` | CALL-E API key |
| `CALLE_BASE_URL` | Default `https://api.heycall-e.com` |
| `GRIDGATE_WEBHOOK_URL` | Public URL for CALL-E webhooks (Phase 5) |

## License

MIT (to be confirmed before submission)
