# Changelog

Todas as alterações relevantes do ReverseLab serão registradas aqui.

## 2026-11-26

### Added
- Estrutura Android modular inicial com `:app`, `:core:domain`, `:core:storage` e `:core:analysis`.
- Aplicação inicial em Kotlin e Jetpack Compose com tema dark e navegação inferior de cinco áreas.
- Documento inicial de arquitetura, changelog e retomada da execução.
- Contratos tipados de domínio para projetos, amostras, evidências, escopos, execuções e descobertas.
- Máquina de estados com transições explícitas para processamento e retomada.
- Regras de integridade para caminhos de origem, SHA-256 e referências de evidência.
- Documentação dos contratos em `docs/DOMAIN_CONTRACTS.md`.
- Checkpoints completos `ReverseLab_CHECKPOINT_001.zip` e `ReverseLab_CHECKPOINT_002.zip`, verificados estruturalmente.

### Deferred
- Execução de builds, testes e validações para a fase final, conforme a política de execução vigente.
- Gradle Wrapper binário oficial pendente de inclusão.
