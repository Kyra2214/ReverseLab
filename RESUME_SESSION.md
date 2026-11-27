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
- Checkpoints ZIP: não fazem parte do fluxo atual, por instrução explícita do usuário; o trabalho segue diretamente na branch main.
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
- Próximo requisito: correlação segura dos APKs internos por package name/splitName, mantendo as entradas como referências virtuais vinculadas ao contêiner original. O parser lê conservadoramente apenas a primeira entrada ZIP de cada APK interno; quando o manifesto não está nela, registra MANIFEST_NOT_FIRST sem percorrer dados potencialmente expansivos.
- ZIPs: ignorados por instrução explícita do usuário.

- Commit do refinamento de segurança do parser: `a6c82205e437344865cf2fd671b4a9458054a45e`.

- Último commit de documentação/checkpoint: atualização de retomada após o refinamento do parser (registrado no histórico da branch `main`).

## Atualização 2026-11-27 — correlação de APKs virtuais

- Branch: `main`.
- Commit da implementação do analisador: `17977088cc5a9491fbfe6a89266ae5a4476e2b90`.
- Commit da integração da tela Analisar: `cd42503023b236fcaa8c8b4ea37e00e71e45abad`.
- Novo `NestedPackageAssociationAnalyzer`: cruza package name de APKs internos XAPK/APKS com APKs externos somente no mesmo lote; avalia bases, splitName, uses-split e configForSplit.
- UI apresenta caminhos virtuais e motivos por contêiner/package. Não cria registros de amostra, não extrai APKs e não muda associações persistidas.
- Fase 1.4: correlação lógica e apresentação implementadas; interpretação de metadados toc.pb/manifest.json e revisão de completude continuam pendentes.
- Fase 1.7: avaliação técnica de identidade e dependências ampliada para membros virtuais; revisão final de completude continua pendente.
- Build/testes não executados por instrução; validação permanece acumulada para a etapa final. ZIPs ignorados.

## Atualização 2026-11-27 — manifesto XAPK

- Commit do modelo de domínio: `06ae0de0aa4fca892e8f2fa35a08e235907c1442`.
- Commit da leitura limitada do `manifest.json`: `81d633d004f693090f62e6e7d27d39d953b21c95`.
- Commit da persistência SQLite 6: `3020802362f42b245d9307fa33d361be893188c3`.
- Commit da comparação de identidade e caminhos: `c7946e4378b6d93b4ba89c11ad3f810c92eb1a9c`.
- Commit da exibição na tela Analisar: `5c98eae4c3f6abb95211cfa41d425ca991d8225b`.
- Ajuste de precedência de incompatibilidade: `87cd5d7653221aa49bd4590622fbbb831f2efc65`.
- SQLite versão 6 armazena package name, nome, versão, version code, caminhos de `split_apks` e status, preservando registros existentes por migração aditiva.
- Limite do JSON: 256 KiB. Nenhum APK interno é extraído ou promovido a amostra independente.
- Próxima pendência de 1.4/1.7: interpretar `toc.pb` de APKS e aprimorar a avaliação de variantes sem inferir completude indevida.
- Builds/testes não executados por instrução; validação final permanece pendente.

## Atualização 2026-11-27 — toc.pb APKS

- Modelo de domínio de variantes/módulos/APKs: commits `0e0ecae2581becba83e6cd3f235f8747d622fec2` e `40e51aa8cf82df2bfcbb44d9f7578cca2deb3717`.
- Parser protobuf: `6f0ccaf0f63b10006a7d620fb80e05ab3d7b2968`.
- Integração ao inventário: `4ee9ed0e61f75f595d458b91c60f8ee95440e177`.
- Persistência SQLite 7: `fbbd55c63f0325890b41ae0828590cdb891670be`.
- UI de variantes e módulos: `b7c9e46dc561620eb7ffbc3ddedb6bf1f42c8a62`.
- Correlação de package, caminhos e dependências: `93541ce5cf82066c257f764096525345caf6549e` e `b4ab3659e230019cefa3efc9a799c85a783ed31f`.
- Correção de cobertura parcial do limite de APKs: `74557a7b661d10506739cb920d8edb1ee2c4fb9d` e `79518d485527b48a376cf3ee18b493a17dffe5aa`.
- Limites: toc.pb 4 MiB; 32 APKs internos; manifestos binários até 2 MiB por APK e 16 MiB agregados.
- Ainda pendente: interpretação semântica de ABI/idioma/densidade/SDK targeting e avaliação de completude por dispositivo.
- Builds/testes não executados por instrução; validação final permanece pendente.

## Atualização 2026-11-27 — targeting APKS

- Commit da ampliação do modelo: `50c595ea0193df71ce5f3e8f2414d1f8e85dc060`.
- Commit da decodificação de ABI/idioma/densidade/SDK/multi-ABI: `f6c09d3f39856c6523b28378e5c0d847cae21c4a`.
- Commit da persistência dos resumos: `a246710676a7294553a27bdcae95aeab10597812`.
- Commit da exibição na UI: `2cf49c2c3dcb7f971a07b7b6afe5c5ec8c160314`.
- O parser agora mostra valores e alternativas por APK; não escolhe variantes nem afirma cobertura total.
- Próxima pendência: interpretar dimensões adicionais de targeting e definir critérios de completude por dispositivo com evidências suficientes.
- Build/testes não executados por instrução; validação final permanece pendente.

## Atualização 2026-11-27 — targeting e salvaguardas APKS/XAPK

- Commit da correlação conservadora com truncamento: `7919e058dce62ad6ca871acdca381746a9c5e610`.
- Commit de targeting decodificado: `f6c09d3f39856c6523b28378e5c0d847cae21c4a`.
- Commit de persistência dos resumos de targeting: `a246710676a7294553a27bdcae95aeab10597812`.
- Commit de UI de targeting: `2cf49c2c3dcb7f971a07b7b6afe5c5ec8c160314`.
- Commits de proteção contra alertas falsos e overflow: `9a12a90eaa9774b599c5c832f09d7bafac06352f`, `84dbd99c47a6a5b10e4cb445da9a078f60861f86`, `301fa6d2e6bed9132b5169dff56a281764c62930`.
- Último checkpoint de documentação anterior: `8ddae0dace9b62ed0b5cabbbc9e753863210d416`.
- O analisador não acusa caminhos como ausentes se a lista de APKs internos estiver limitada ou truncada.
- Próxima pendência: completar targeting adicional e avaliação de variantes por dispositivo; depois avançar para o próximo item da Fase 1.
- Build/testes não executados, conforme instrução.

## Atualização 2026-11-27 — apresentação de vínculos virtuais

- Commit: `f0b6aa41a05d6965e888913d8d7fd3fd9448b49d`.
- A UI da tela Analisar agora apresenta nomes dos APKs externos correspondentes, resolvidos a partir dos IDs do analisador no lote atual.
- Não há alteração da associação persistida dos registros nem cópia/extração de membros virtuais.
- Último estado de documentação antes desta nota: `6e8f5ed4cc2791f49d79c836187733af671cc6a9`.
