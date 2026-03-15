# Calculus API

**Java 21 · Spring Boot 3.4 · Railway · Hand-coded symbolic engine.**

A Spring Boot REST API that exposes a symbolic mathematics engine as HTTP endpoints. Differentiation, integration, and implicit differentiation — computed by an engine written entirely in plain Java.

**Live:** https://calculus-api-production.up.railway.app

---

## What Makes It Different

This engine was written from scratch — tokenizer, parser, AST, differentiator, integrator, simplifier, printer. No external math library. The benchmark for correctness was testing the system with different equation and trying to ensure it could solve them.

The engine has zero Spring dependencies. It is pure Java, testable in isolation without starting a Spring context. The REST layer is a thin wrapper around it.

A plain HTML/CSS/JavaScript frontend is served directly from the same Spring Boot JAR — no separate hosting required.

---

## Architecture

```
HTTP Request
     │
     ▼
┌─────────────────────────────────────┐
│         CalculusController          │
│   Receives request · Calls service  │
│   Returns ResponseEntity            │
│   Zero math logic                   │
└────────────────┬────────────────────┘
                 │ calls only
┌────────────────▼────────────────────┐
│          CalculusService            │
│   Calls engine · Catches exceptions │
│   Builds CalcResponse               │
│   Only layer that touches engine    │
└────────────────┬────────────────────┘
                 │ calls only
┌────────────────▼────────────────────┐
│     Differential_and_Integral       │
│         (Math Engine)               │
│   Pure Java · Zero Spring deps      │
│   Tokenizer → Parser → AST          │
│   Differentiator → Simplifier       │
│   → Printer                         │
└─────────────────────────────────────┘
```

The controller has zero math logic. The engine has zero Spring awareness.

---

## How the Engine Works

Every expression goes through the same internal pipeline:

```
Input string: "x^3 + 2x"
      │
      ▼
Tokenizer      → [IDENT:x, CARET, NUMBER:3, PLUS, NUMBER:2, IDENT:x, EOF]
      │
      ▼
Parser         → AST: Add(Pow(Var(x), Num(3)), Mul(Num(2), Var(x)))
      │
      ▼
Differentiator → applies calculus rules node by node
      │
      ▼
Simplifier     → reduces and cleans the result AST
      │
      ▼
Printer        → "3x^2 + 2"
```

**The Tokenizer** walks the input character by character and produces typed tokens. Throws on unexpected characters — first validation layer.

**The Parser** uses recursive descent parsing. Each grammar rule is a method. Precedence hierarchy: addition/subtraction → multiplication/division → exponentiation → unary → primary. Ensures `2 + 3 * x` parses as `2 + (3 * x)`, not `(2 + 3) * x`.

**The AST** is a tree of typed Java objects: `Num`, `Var`, `Add`, `Sub`, `Mul`, `Div`, `Pow`, `Neg`, `Func`, `DyDx`. Every mathematical concept maps to one node type.

**The Differentiator** pattern-matches on AST node type and applies the corresponding calculus rule — constant rule, power rule, product rule, quotient rule, chain rule for trig, natural log, exponential, inverse trig, and variable exponents.

**The Integrator** handles power rule, trig functions with chain rule, exponential, natural log via integration by parts, u-substitution, simple partial fractions, and recursive integration by parts for `x^n * sin(x)`, `x^n * cos(x)`, and `x^n * exp(x)` patterns. When no pattern matches, it returns an unevaluated `Integral(expression)` node — the service detects this and reports it honestly.

**The Simplifier** runs in two passes: constant folding and identity elimination first, then like-term collection, coefficient combination, and nested power collapse.

---

## API Contract

Base URL (live): `https://calculus-api-production.up.railway.app/api/v1/calc`

All endpoints accept and return `application/json`. Every response — success or failure — uses the same shape:

```json
{
  "success": true,
  "result": "3x^2 + 2",
  "error": null,
  "expression": "x^3 + 2x",
  "operation": "DIFFERENTIATE"
}
```

### Endpoints

**POST** `/differentiate`
```json
Request:  { "expression": "x^3 + 2x" }
Response: { "success": true, "result": "3x^2 + 2", ... }
```

**POST** `/differentiate/nth`
```json
Request:  { "expression": "x^4", "order": 2 }
Response: { "success": true, "result": "12x^2", ... }
```

**POST** `/integrate`
```json
Request:  { "expression": "x^2" }
Response: { "success": true, "result": "(x^3)/3 + C", ... }
```

**POST** `/integrate/definite`
```json
Request:  { "expression": "x^2", "lowerBound": 0, "upperBound": 3 }
Response: { "success": true, "result": "9", ... }
```

**POST** `/differentiate/implicit`
```json
Request:  { "equation": "x^2 + y^2 = 1" }
Response: { "success": true, "result": "(-(2x))/(2y)", ... }
```

**Error response (HTTP 400):**
```json
{ "success": false, "result": null, "error": "Unexpected character '@' at position 1", ... }
```

---

## Running Locally

**Prerequisites:** Java 21, Maven

```bash
git clone https://github.com/Kunzoick/calculus-api.git
cd calculus-api/calculus-api
mvn spring-boot:run
```

Open `http://localhost:8080` — the frontend loads automatically. The frontend derives its API base URL from `window.location.origin`, so it works identically in local and production with no configuration change.

---

## Tech Stack

| Technology | Purpose |
|---|---|
| Java 21 | LTS release, selected for Railway compatibility |
| Spring Boot 3.4 | REST framework, static file serving |
| Spring Web | `@RestController`, routing, `ResponseEntity` |
| Jakarta Validation | DTO-level validation before service is called |
| Maven | Build tool, detected automatically by Railway |
| Railway | Cloud deployment, HTTPS, PORT management |
| Plain HTML/CSS/JS | Frontend — no framework, no build step |

No database, no Redis, no JWT, no Spring Security, no Lombok, no frontend framework. Stateless compute only.

---

## Tests

20 engine tests covering the core differentiation and integration rules. Tests instantiate the engine directly — no Spring context required, consistent with the engine's zero-framework design.

Includes regression tests for power rule and basic differentiation to confirm new features (inverse trig, variable exponents, integration by parts extensions) did not break existing behaviour.

---

## Known Limitations

**Variable exponents with complex expressions** (`x^(x+1)`) are not supported.

**General integration by parts is not supported.** Only specific patterns are handled — arbitrary product expressions fall through to the unevaluated result.

**Some integrals return unevaluated.** When no pattern matches, the engine returns `Integral(expression)` rather than throwing. Returning a wrong answer would be worse.

**Only differentiates and integrates with respect to x.** Multi-variable calculus is not supported.

**Implicit differentiation only supports y as the dependent variable.**

**Definite integral fails if the antiderivative is unevaluated.** Symbolic antiderivative is computed first — if that fails, the definite integral cannot be evaluated.

---

*Calculus API · v2.0 · Kunzoick*
