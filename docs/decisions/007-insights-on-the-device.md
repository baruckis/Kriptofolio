# ADR-007 — Insights on the device

- **Status:** proposed — the principles are agreed; the details below are confirmed by the owner
  before the Insights step starts
- **Date:** 2026-10-21

## Context

2.0.0 includes Insights, a Pro feature that writes a short educational comment about the user's
portfolio (ADR-002, ADR-006). Both platforms now offer a language model that runs on the device:
Gemini Nano through ML Kit GenAI on supported Android devices, and Apple's Foundation Models
framework on iOS 26 and later. A portfolio is sensitive financial data, and the app's promise is
that it stays on the device.

## Proposal

1. **One interface, platform engines.** `InsightsEngine` lives in shared code, with an on-device
   engine per platform and a fake engine for tests and fixtures mode.
2. **On the device first.** Where the platform model is available, nothing leaves the device.
3. **Your own key as a fallback.** Where it is not, the user may enter a key for an
   OpenAI-compatible endpoint of their choice; the request goes directly from the device to that
   provider. There is no Kriptofolio-paid model and no relay server.
4. **Only an anonymized summary leaves the device**, and only in the fallback: coin symbols,
   percentage shares and 24-hour changes. Never amounts, never the fiat value of the portfolio.
   A pure function produces that summary and a test asserts that its output contains no amount.
5. **Educational, not advice.** The comment explains concentration, diversification and recent
   movements; it never tells the user to buy or sell. Every answer carries a fixed disclaimer, is
   shown only in the session and is not stored. The prompt is a versioned file in the repository.
6. Store data-safety declarations state that, in the fallback, data is sent to a third party of
   the user's choice.

## Open before the step

- The exact devices and OS versions supported by each platform model at implementation time.
- The wording of the disclaimer and the tone of the prompt, in the owner's voice.

## Reopen when

A platform model becomes unavailable to third-party apps, or store policy treats the feature as
financial advice.
