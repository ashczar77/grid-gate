# GridGate

An AI phone agent that finds available providers when unreliable power makes normal booking impossible, powered by [CALL-E](https://www.heycall-e.com/).

GridGate calls providers one at a time when outages make it unclear who is still operating. You set a deadline, budget, and list of numbers. GridGate uses CALL-E to place the calls, stops at the first provider that can help, and returns structured results you can act on. Dry-run mode is on by default; live calls require an explicit second step.

Built for the [CALL-E: Your Code Is Calling](https://call-e.devpost.com/) hackathon.

## Stack

- Java 21
- Spring Boot 3
- CALL-E Developer API (REST)
- H2 (local development)

## Status

Phase 1 in progress: project scaffold and domain model (run, provider attempts, money/currency support).

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
