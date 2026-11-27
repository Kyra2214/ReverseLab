# Arquitetura inicial do ReverseLab

## Estado

Fundação inicial criada na Fase 0. Esta arquitetura é a separação inicial de responsabilidades, não uma declaração de que os módulos funcionais do roadmap já estejam implementados.

## Módulos Gradle

- `:app`: aplicação Android, Activity, Jetpack Compose, navegação e apresentação.
- `:core:domain`: entidades, estados, regras e contratos sem dependência da UI.
- `:core:storage`: persistência, importação, procedência, backup, SAF e preservação dos dados.
- `:core:analysis`: orquestração de análise e adaptadores de ferramentas determinísticas.

## Direção das dependências

- `:app` consome os módulos de núcleo.
- `:core:storage` e `:core:analysis` poderão consumir contratos de `:core:domain` quando forem implementados.
- `:core:domain` não depende de Android UI, banco de dados, rede ou provedores de IA.
- A mini LLM será integrada por uma interface substituível em fase própria.

## Decisões e inventário

- Decisões arquiteturais: `docs/decisions/`.
- Dependências e licenças: `DEPENDENCIES.md`.
- Versões centralizadas: `gradle/libs.versions.toml`.
- Política de preservação: `docs/STORAGE_POLICY.md`.
- Backup e exportação: `docs/BACKUP_EXPORT.md`.

## Decisões iniciais

1. Kotlin e Jetpack Compose para a aplicação Android.
2. Tema dark grafite azulado, ciano primário e violeta secundário.
3. Barra inferior com Início, Biblioteca, Analisar, IA e Ajustes.
4. `minSdk 26` como base inicial para APIs modernas de armazenamento e suporte a dispositivos ainda comuns; reavaliar na etapa de compatibilidade final.
5. `compileSdk 36`, `targetSdk 36` e Java 17 nesta fundação. Atualizações serão controladas, sem versões dinâmicas.
6. Nenhuma amostra será executada pelo app nesta fase.
7. Nenhum dado de usuário será apagado automaticamente.

## Toolchain fixada

- Android Gradle Plugin: 9.3.1.
- Gradle Wrapper pretendido: 9.5.0.
- Kotlin / Compose Compiler Plugin: 2.4.21.
- Compose BOM: 2025.11.00.
- AndroidX Activity Compose: 1.11.0.

A compatibilidade declarada do Kotlin 2.4.21 cobre AGP até 9.3.1 e Gradle até 9.7.0. A combinação precisa ser confirmada na validação final do projeto; nenhum build foi executado durante esta etapa por determinação do fluxo de implementação.

## Pendências da fundação

- Adicionar os binários/scripts oficiais do Gradle Wrapper quando o artefato binário estiver disponível no fluxo de versionamento.
- Definir estratégia e configuração de CI para a fase final, sem antecipar testes.
- Implementar persistência e permissões de arquivo em submódulos próprios.
- Inventariar licenças transitivas à medida que forem incorporadas.
