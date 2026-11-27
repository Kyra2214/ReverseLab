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

## Limites

Estes são contratos de dados e invariantes locais. Ainda não representam persistência Room, validação semântica automática, execução de IA ou teste reproduzível. Essas implementações pertencem às fases correspondentes do roadmap.
