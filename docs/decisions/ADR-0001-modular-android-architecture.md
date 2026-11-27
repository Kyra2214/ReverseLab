# ADR-0001: Arquitetura Android modular

- Estado: Aceita.
- Data: 2026-11-26.
- Contexto: o ReverseLab combina interface, armazenamento, análise determinística, IA e biblioteca de conhecimento. Misturar essas responsabilidades dificultaria substituir ferramentas e preservar o funcionamento offline.

## Decisão

Separar o projeto em `:app`, `:core:domain`, `:core:storage` e `:core:analysis`.

- `:app` concentra Activity, Compose, navegação e apresentação.
- `:core:domain` contém contratos e invariantes sem depender de interface ou provedores.
- `:core:storage` controla persistência, SAF, procedência e preservação.
- `:core:analysis` coordena extração e adaptadores de ferramentas.

## Consequências

- Regras de domínio não devem depender de UI, Room ou rede.
- Extração determinística e interpretação por LLM permanecem separadas.
- Novos módulos poderão ser adicionados quando houver responsabilidade concreta, evitando fragmentação prematura.
