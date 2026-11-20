# Reader Lens French Corrector Design

## Goal

Improve the naturalness and grammatical accuracy of Reader Lens French output without changing the meaning, character names, tone, or bubble positions produced by Index-Translate 9B.

## Pipeline

1. OCR groups text into page bubbles.
2. Index-Translate 9B translates the complete page in contextual batches.
3. Proofbird GECToR French 1.3.0 corrects the translated bubbles as one page batch.
4. Corrected strings are mapped back to their original bubble identifiers.
5. The existing overlay renderer displays the result.

Proofbird is a post-processing pass only. It must never replace the translation model or alter bubble geometry.

## Model and runtime

- Model: `proofbird/gector-bert-fr`, Apache-2.0.
- Runtime: ONNX Runtime Android on CPU.
- Distribution: dynamically quantized INT8 ONNX weights with an in-graph ArgMax output, downloaded on first Lens use.
- Tokenization: local Kotlin implementation of the model's cased BERT normalizer and WordPiece vocabulary.
- Correction: up to five GECToR passes, stopping as soon as a pass makes no change.
- Storage: the existing resumable and checksum-verified Reader Lens model store.

The quantized model weights are not bundled in the APK. The small vocabulary and label files are bundled locally, while a model manifest pins the exact downloaded version, size, and SHA-256 digest.

## Reliability

- Translation remains available if the correction model cannot be downloaded or initialized.
- Any correction exception returns the unmodified Index translation instead of failing the chapter.
- A corrector version is part of the Lens cache identity so stale uncorrected pages are not reused.
- Native sessions are serialized and released with the Reader Lens engine.
- Dense pages are corrected in groups of at most eight bubbles, and a bubble above the model token limit remains unchanged.

## UX

The correction pass is transparent. Lens keeps one activation control and one progress state; it does not expose a second translation mode. The initial model preparation may include the extra corrector download, while already cached pages remain instant.

## Verification

- Pure Kotlin tests cover tokenization, edit tags, punctuation reconstruction, multipass stopping, and fail-open behavior.
- Pipeline tests prove that every translated bubble is corrected exactly once and keeps its identifier.
- Desktop ONNX validation compares FP32 and INT8 outputs on French reference sentences.
- Existing Reader Lens unit tests must remain green.
