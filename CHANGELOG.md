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
- Gate de governança para exclusão, sobrescrita, limpeza e deduplicação física, com confirmação exata de escopo.
- Persistência de permissão de leitura SAF por URI, sem solicitar acesso amplo ao armazenamento.
- Documentação da política de arquivos e permissões.
- Três ADRs para arquitetura modular, preservação de dados e toolchain fixada.
- Catálogo central de versões Gradle e inventário inicial de dependências/licenças.
- Modelo de erros com códigos, severidade, retry e IDs de correlação.
- Logs estruturados com redação de credenciais, tokens, segredos em URLs e cabeçalhos de autenticação.
- Persistência SQLite de execuções e histórico transacional de estados, sem API de exclusão.
- Checkpoints completos `ReverseLab_CHECKPOINT_001.zip` a `ReverseLab_CHECKPOINT_005.zip`, verificados estruturalmente.

### Deferred
- Execução de builds, testes e validações para a fase final, conforme a política de execução vigente.
- Gradle Wrapper binário oficial pendente de inclusão.
