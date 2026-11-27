# Contratos de domínio

## Identidade

`ProjectId`, `SampleId`, `EvidenceId` e `AnalysisRunId` são identificadores tipados. Eles evitam confundir IDs de entidades diferentes entre camadas.

## Procedência e evidências

`SourceLocator` aponta para uma amostra e caminho relativo, podendo registrar caminho interno de contêiner, SHA-256 e intervalo de linhas. Caminhos absolutos e segmentos `..` são rejeitados. O SHA-256, quando presente, precisa ter 64 caracteres hexadecimais.

`EvidenceReference` registra tipo, classificação, descrição, módulo e instante de captura. A classificação usa os quatro estados do roadmap: Observado, Interpretado, Hipótese e Validado.

## Escopo de análise

`AnalysisScope` descreve um módulo, amostras e evidências incluídas. A inclusão de dados de outros módulos fica explícita em `includesCrossModuleData`; escopos não podem ser vazios.

## Execuções

`AnalysisRun` mantém motor, provedor/modelo, versão de prompt, estado, horário, tokens e custo informado. Uma execução externa exige identificação do provedor. Métricas de consumo/custo podem ficar ausentes quando a API não as fornece.

## Descobertas

`Finding` exige ao menos uma referência de evidência. Isso impede registrar uma conclusão sem trilha de origem no contrato de domínio.

## Estados

`ProcessingState.canTransitionTo` centraliza as transições válidas. Estados terminais (`CANCELLED` e `COMPLETED`) não podem ser reabertos. Uma nova tentativa deve criar ou retomar uma execução por fluxo explícito, não alterar silenciosamente o histórico concluído.

## Erros e diagnóstico

`AppError` separa código estável, severidade, possibilidade de repetição, mensagem adequada ao usuário e ID de correlação. `DiagnosticEvent` representa logs estruturados; `DiagnosticLogger` é o contrato para destinos substituíveis.

## Persistência de execuções

`AnalysisRunStore` define inserção, consulta por ID/projeto e transição condicional de estado. Não expõe exclusão de histórico. A implementação SQLite registra eventos de transição para permitir auditoria e retomada.


## Revisão de associações de amostras

SampleAssociationUpdate representa a alteração solicitada pelo usuário e SampleAssociationReviewRecord registra os valores anteriores e novos, a ação e o instante da revisão. O catálogo não oferece exclusão de amostras. A implementação SQLite atualiza os vínculos e grava a auditoria em uma única transação. USER_REJECTED significa que a associação foi explicitamente separada pelo usuário; não significa que o pacote foi tecnicamente analisado ou considerado incompatível.
