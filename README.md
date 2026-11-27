# ReverseLab

**Laboratório Android de engenharia reversa, descoberta tecnológica e inteligência reutilizável.**

O ReverseLab investiga como tecnologias, sistemas e mecanismos de aplicativos são construídos. O objetivo é registrar evidências e transformar descobertas em conhecimento técnico reutilizável, não clonar aplicativos completos.

## Estado atual

- Fase 0 — Fundação e governança: em andamento.
- Último submódulo concluído: 0.3 — política de arquivos e permissões. Próximo: 0.4 — decisões arquiteturais e licenças.
- A aplicação possui um esqueleto inicial em Kotlin e Jetpack Compose.
- Recursos de importação, análise estática, CryptoLab, IA e Knowledge Vault ainda serão implementados na ordem do [roadmap](ROADMAP.md).
- Testes, builds e validações estão reservados para a fase final do projeto, conforme o fluxo de execução vigente.

## Estrutura do repositório

```text
ReverseLab/
├── app/
├── core/
│   ├── domain/
│   ├── storage/
│   └── analysis/
├── docs/
├── gradle/
├── ROADMAP.md
├── CHANGELOG.md
└── RESUME_SESSION.md
```

## Princípios inegociáveis

1. Descoberta, não clonagem.
2. Evidência antes de conclusão.
3. Processamento local como padrão e consentimento antes de qualquer envio externo.
4. Amostras desconhecidas não são executadas no ambiente principal.
5. Nenhum arquivo, relatório, evidência ou artefato é apagado automaticamente.
6. Duplicatas são identificadas, nunca removidas sem autorização.
7. Toda descoberta conserva procedência e referências à origem.
8. A arquitetura deve permitir substituir ferramentas e modelos sem reescrever o aplicativo.

## Documentação

- [Roadmap completo](ROADMAP.md)
- [Arquitetura inicial](docs/ARCHITECTURE.md)
- [Contratos de domínio](docs/DOMAIN_CONTRACTS.md)
- [Política de arquivos e permissões](docs/STORAGE_POLICY.md)
- [Decisões arquiteturais](docs/decisions/)
- [Inventário de dependências e licenças](DEPENDENCIES.md)
- [Changelog](CHANGELOG.md)
- [Estado de retomada](RESUME_SESSION.md)
