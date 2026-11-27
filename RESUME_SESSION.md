# ReverseLab — Estado de retomada

- Branch: `main`.
- Commit de referência antes da implementação: `a69ea8f772035778975dd78b0927b455a815cd2b`.
- Commit do submódulo 0.1: `b4eb0b9e9059ae1b1df0bbc14d8f0e9030a123c6`.
- Commit do submódulo 0.2: `576fdd210bc0857af9318f0dc9e02ad50e852d7e`.
- Commit do submódulo 0.3: `34d539bd53e4bf81459b48d8297b4048147b9d41`.
- Commit do submódulo 0.4: `b34dc6556e02f94216f502a081c864dbb354b2fa`.
- Commit do submódulo 0.5: `cc865c8694bde2c73935d1262cbdfa930605f47a`.
- Commit do submódulo 0.6: `4d3c995d3e3a184726b07beb612bebad48dc0b82`.
- Commit do submódulo 1.1: `a1b313db661003fc0dd1295ec85e2060a02b3043`.
- Commit do submódulo 1.2: `b504c53559407dd542a88177e0bc6f3eee434335`.
- Commit do submódulo 1.3: `f1798cbcb17738393633ae477242e7153c7fdf77`.
- Commits do submódulo 1.4: `5222df8bf4b74dd3a58c1bcd566c64bfe500c00e` e `1c66315cd005c72252846f05f0a61c0522d29e17`.
- Commits do submódulo 1.5: `9b342feb2463818c63696891809d8022229144e1` e `be8c98b3b22a5e09f7078c5f562d30050c64bef8`.
- Commits do submódulo 1.6: `41778e97743dc6b3bb1d90d48249f47bd50b0684` e `0c59e7eaa16203e8dd4c5fe163d32d7d68b83c5e`.
- Fase 0: implementação dos seis submódulos concluída; validação final pendente.
- Fase 1: 1.1, 1.2, 1.3, 1.5 e 1.6 concluídos; 1.4 parcial; 1.7 é o próximo.
- Submódulo 1.7: revisão manual de associações implementada parcialmente no commit 55199248a6fda06c1488edcb0ac37e14ae69d6ae; integração do estado USER_REJECTED e apresentação detalhada do histórico corrigidas no checkpoint seguinte. A detecção técnica de completude por manifesto permanece pendente. Commit de integração/documentação: `ba052c9629df3d0b8c1f4aa0837db7e1aa2c10a0`.
- Commit mais recente: `113d03c500358b33b7d944a6dc2271d21610ff56` adiciona `PackageSetCompletenessAnalyzer` no módulo core:analysis. O analisador é conservador e está integrado ao histórico da tela Analisar; identifica ausência de APK base, múltiplas bases, manifesto estruturalmente ausente, inventário pendente e identidade ainda não verificada.
- Commit `155faf3` adicionou parser Android Binary XML; `bb3046b` integrou a leitura do manifesto ao inventário APK; `4fe5749` limitou leitura compatível com API mínima; `c3151fd` persistiu metadados com migração SQLite 4; `85a34dc` correlacionou package name/splitName/dependências; `26b03b0` exibiu metadados no histórico; `5df6e78` tornou a migração segura para catálogos antigos.
- Fase 1.7 continua parcial: não há parsing de APKs aninhados em XAPK/APKS nem manifesto protobuf AAB; a presença de todos os splits obrigatórios não pode ser inferida apenas pelos manifestos presentes. Próximo foco: completar a análise de estruturas compostas e refinar a identificação de conjuntos incompletos.
- Testes/builds: não executados por regra de execução; ficam acumulados para a validação final.
- Pendência técnica: Gradle Wrapper binário oficial não foi obtido neste ambiente; não foi criado binário fictício.
- Checkpoints 001 a 011: pacotes completos e integridade estrutural confirmada.
- Checkpoint 012: `ReverseLab_CHECKPOINT_012.zip` corresponde ao submódulo 1.6; não representa o commit posterior 55199248a6fda06c1488edcb0ac37e14ae69d6ae.
- Checkpoint 013: pacote ZIP do commit mais recente ainda precisa ser gerado e ter integridade estrutural verificada.
- ZIPs: não gerar, por instrução atual do usuário.
- Progresso global estimado: inferior a 1%; Fase 0 com 6/6 submódulos de implementação concluídos; Fase 1 com 1.1, 1.2, 1.3, 1.5 e 1.6 implementados, 1.4 parcial e 1.7 parcialmente implementado. Validação final pendente. A validação final continua pendente.

- Importação Fase 1.4: o `SampleImportCoordinator` agora inspeciona APKs importados, persiste inventário/manifesto, corrige BASE/SPLIT pelo atributo split e agrupa por package name com confiança MANIFEST_VERIFIED. OBB permanece heurístico. XAPK/APKS ainda não expandem seus APKs internos como amostras individuais.

- Ajuste adicional `285c864`: em múltiplas bases com mesmo package, bases e splits permanecem no mesmo grupo ambíguo para revisão. `e011363` atualiza o contrato documental do avaliador.
- Estado de execução: 1.4 e 1.7 parcialmente implementados. Pendência atual de 1.4: correlacionar logicamente os manifestos virtuais de APKs internos com amostras/base sem descartar ou duplicar o contêiner original. Próxima pendência de 1.7: estender parsing para estruturas compostas e refinar os requisitos de completude sem inferir splits ausentes.


## Atualização 2026-11-27

- Submódulo trabalhado: extensão de 1.4 para leitura de manifestos em APKs aninhados de XAPK/APKS.
- Implementado: parsing por fluxo ZIP aninhado, sem extração; persistência SQLite 5; exibição de package/split/configForSplit/uses-split por caminho virtual; limites explícitos de leitura.
- Não implementado ainda: associação lógica desses membros a amostras externas ou bases, parsing de toc.pb/manifest.json e prova de completude de variantes.
- Estado: Fase 1.4 parcialmente implementada; Fase 1.7 parcialmente implementada. Nenhum build/teste executado.
- Próximo requisito: correlação segura dos APKs internos por package name/splitName, mantendo as entradas como referências virtuais vinculadas ao contêiner original. O parser lê conservadoramente apenas a primeira entrada ZIP do APK interno; quando o manifesto não está nela, registra MANIFEST_NOT_FIRST sem percorrer dados potencialmente expansivos.
- ZIPs: ignorados por instrução explícita do usuário.

- Commit mais recente: `a6c82205e437344865cf2fd671b4a9458054a45e`.
