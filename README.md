# ReverseLab

**Laboratório Android de engenharia reversa, descoberta tecnológica e inteligência reutilizável.**

O ReverseLab investiga como tecnologias, sistemas e mecanismos de aplicativos são construídos. O objetivo é registrar evidências e transformar descobertas em conhecimento técnico reutilizável, não clonar aplicativos completos.

## Estado atual

- Fase 0 — implementação concluída; validação final pendente.
- Último submódulo concluído: Fase 1.6 — inventário interno de APK/OBB/XAPK/APKS/AAB.
- O submódulo 1.4 permanece parcial porque as associações de APK/splits/OBB ainda precisam de confirmação.
- Próximo: Fase 1.7 — revisão de grupos e detecção de conjuntos incompletos.
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
- [Erros, logs e estado persistente](docs/ERRORS_AND_LOGGING.md)
- [Backup e exportação](docs/BACKUP_EXPORT.md)
- [Navegação e telas iniciais](docs/UI_NAVIGATION.md)
- [Seleção de documentos e permissões SAF](docs/IMPORT_ACCESS.md)
- [Detecção inicial de formatos](docs/FORMAT_DETECTION.md)
- [Pipeline de importação](docs/IMPORT_PIPELINE.md)
- [Histórico e ocupação de armazenamento](docs/IMPORT_HISTORY.md)
- [Inventário interno de contêineres](docs/ARCHIVE_INSPECTION.md)
- [Decisões arquiteturais](docs/decisions/)
- [Inventário de dependências e licenças](DEPENDENCIES.md)
- [Changelog](CHANGELOG.md)
- [Estado de retomada](RESUME_SESSION.md)
