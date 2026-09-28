# Contributing to Dilauro AI-OS

Thank you for your interest in contributing! Dilauro AI-OS is an early-stage research project and all contributions are welcome.

## How to Contribute

### 1. Report Issues
Use [GitHub Issues](https://github.com/marcosinzaurralde95/dilauro-aios/issues) to report bugs, ask questions, or suggest features. Please check existing issues before opening a new one.

### 2. Propose Changes (RFCs)
For significant changes (new components, architectural decisions), open an RFC document in `docs/rfcs/` before writing code. This helps align the team before implementation.

### 3. Submit Pull Requests

```bash
# Fork the repo, then:
git clone https://github.com/YOUR_USERNAME/dilauro-aios.git
cd dilauro-aios
git checkout -b feat/your-feature-name
# ... make your changes ...
git push origin feat/your-feature-name
# Open a PR on GitHub
```

**PR requirements:**
- All CI checks must pass (`cargo build`, `cargo clippy`, `cargo fmt --check`)
- Include a clear description of what changed and why
- Reference related issues with `Closes #123` or `Related to #123`

## Code Style

- Run `cargo fmt` before committing
- Run `cargo clippy -- -D warnings` and fix all warnings
- Keep `unsafe` blocks minimal and always document why they are needed
- Write doc comments (`///`) for all public functions and types

## Branch Naming

| Type | Pattern | Example |
|------|---------|---------|
| Feature | `feat/description` | `feat/adaptive-scheduler` |
| Bug fix | `fix/description` | `fix/memory-leak-allocator` |
| Docs | `docs/description` | `docs/kernel-architecture` |
| Refactor | `refactor/description` | `refactor/interrupt-handlers` |

## Commit Messages

Follow [Conventional Commits](https://www.conventionalcommits.org/):

```
feat(scheduler): add ML-based priority adjustment
fix(memory): resolve double-free in slab allocator
docs(kernel): add interrupt handling diagram
```

## Areas Where Help is Needed

- 🦀 Rust `no_std` kernel development
- 🤖 ML inference on bare-metal targets
- 🧱 WASM / WASI runtime integration
- 📐 Architecture documentation and diagrams
- 🧪 Testing infrastructure for OS components

## Code of Conduct

Be respectful, constructive, and collaborative. We're all here to build something cool.
