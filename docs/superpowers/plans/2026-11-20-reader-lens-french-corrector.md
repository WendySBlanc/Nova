# Reader Lens French Corrector Implementation Plan

1. Add failing tests for correction orchestration, local edit application, and fail-open behavior.
2. Port Proofbird's French tokenization and GECToR edit decoder to pure Kotlin.
3. Add an ONNX Runtime adapter with a narrow, testable inference interface.
4. Quantize Proofbird 1.3.0 dynamically to INT8 and validate its output against FP32.
5. Add the pinned corrector model manifest and reuse the resumable verified model store.
6. Run the corrector after Index-Translate and include its identity in the cache schema.
7. Wire dependencies and native lifecycle in `AppModule`.
8. Run focused tests, the full Lens test suite, formatting, and static checks.
9. Ask before building an APK.
