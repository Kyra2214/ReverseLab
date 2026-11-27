# Pipeline de importação de amostras

## Fluxo

1. O usuário seleciona um ou mais documentos no SAF e concede acesso de leitura.
2. O detector inspeciona somente o cabeçalho e fornece um formato preliminar.
3. Ao tocar em Importar selecionados, o sistema cria um registro PENDING antes de copiar.
4. O arquivo é copiado uma única vez para a área privada do app, em diretório exclusivo por SampleId.
5. SHA-256 e tamanho são calculados durante o mesmo fluxo de cópia.
6. O registro recebe estado IMPORTED, PARTIAL ou FAILED, caminho relativo e hash disponível.
7. Duplicatas são identificadas por SHA-256, mas cada importação mantém seu próprio registro e sua própria cópia. Nenhum arquivo é removido ou mesclado automaticamente.

## Interrupções e preservação

O registro PENDING é criado antes da cópia. Se a operação falhar, o estado é atualizado para PARTIAL ou FAILED e qualquer artefato parcial existente permanece no armazenamento. Se o processo for interrompido abruptamente, o registro PENDING e os bytes já gravados permanecem para recuperação posterior. Não há limpeza automática.

## Agrupamento de pacotes

- Arquivos APK comuns são candidatos a APK base.
- Nomes iniciados por split_ ou config. são candidatos a split APK.
- OBB é classificado como candidato a expansão.
- XAPK, APKS e AAB são tratados como contêineres individuais nesta etapa.
- Se um lote tiver exatamente um APK base importado com sucesso e ao menos um split/OBB importado, os itens anexáveis recebem uma associação heurística candidata.
- Se houver mais de um APK base, splits e OBB permanecem em grupo ambíguo de lote. Se não houver base, a associação fica limitada ao lote.
- O usuário pode confirmar o grupo heurístico, separar todos os seus membros, vincular manualmente um split/OBB a um APK base ou separar individualmente um item.
- Cada revisão é gravada na auditoria em uma única transação SQLite, preservando grupo e confiança anteriores/novos, ação e horário.
- A interface apresenta o histórico de revisões junto ao lote. USER_REJECTED registra explicitamente que o usuário separou aquela amostra.

Associação heurística não é confirmação de que os arquivos pertencem ao mesmo pacote. A confirmação humana registra uma decisão do usuário, mas não equivale à verificação técnica por manifesto. Foi adicionada a classe `PackageSetCompletenessAnalyzer` em `core/analysis`: ela distingue grupo sem anexos, ausência de APK base, múltiplos APKs base, manifesto estruturalmente ausente, inventário ainda indisponível e identidade ainda não verificada. O analisador usa somente os inventários já registrados e não extrai arquivos. Mesmo quando encontra manifestos, não declara o conjunto completo: package name, splitName, dependências e splits obrigatórios continuam aguardando a leitura de manifestos da Fase 2.

## Identidade e procedência

Cada registro preserva SampleId, lote de importação, URI de origem, nome original, caminho privado relativo, formato preliminar, MIME, tamanho, SHA-256, estado, papel e grupo de associação. O URI SAF não é convertido em caminho absoluto.

## Limites desta etapa

Ainda não há extração de APK/XAPK/APKS/OBB, decodificação de manifestos internos em APKs aninhados de XAPK/APKS, resolução de referências de recursos, leitura de manifestos protobuf de AAB ou inspeção completa de variantes por ABI/densidade/idioma. XAPK/APKS continuam registrados como contêineres, sem promover automaticamente cada APK interno a uma amostra individual. O original permanece intocado; a cópia privada é a amostra de trabalho.

### Metadados de identidade do manifesto

Para APKs individuais, `ArchiveInspector` lê a entrada raiz `AndroidManifest.xml` com limite de 2 MiB e usa `BinaryAndroidManifestParser` para interpretar o XML binário Android sem executar o pacote. São registrados `package`, `split`, `configForSplit` e referências `uses-split`, junto ao estado da interpretação. Os campos são persistidos no inventário SQLite (migração aditiva para versão 4). Durante a importação em lote, APKs com manifesto interpretável têm seu papel corrigido a partir do atributo `split` e são agrupados por `package` coincidente. A confiança `MANIFEST_VERIFIED` é usada somente para base/splits cujo package name foi lido dos manifestos; OBBs anexados permanecem com confiança heurística. Manifestos ausentes, inválidos ou acima do limite permanecem sem identidade declarada e recebem estado/motivo explícito. O parser não resolve IDs de recursos nem decodifica manifestos protobuf de AAB.


### Manifestos de APKs aninhados em XAPK/APKS

O inventário abre entradas APK como fluxos ZIP aninhados e procura somente a entrada raiz `AndroidManifest.xml`. São persistidos caminho virtual, package name, split, configForSplit, uses-split e estado do parsing. Nenhum APK interno é extraído, salvo como amostra independente ou alterado; o contêiner original permanece a única amostra importada.

Limites por inspeção: até 32 APKs internos, 1ª entrada ZIP de cada APK interno e 16 MiB agregados de manifestos internos, além do limite individual de 2 MiB. Esses metadados permitem futura correlação lógica, mas não comprovam que todas as variantes necessárias foram fornecidas.

### Manifesto externo XAPK

O `manifest.json` da raiz é lido com limite de 256 KiB. O inventário persiste package name, nome, versão, version code, caminhos declarados em `split_apks` e estado do parsing. A UI apresenta esses dados e a análise virtual compara o package name externo com os manifestos binários dos APKs internos, além de sinalizar caminhos declarados que não foram encontrados. A migração SQLite 6 adiciona o campo de metadados sem substituir ou apagar inventários anteriores.

A associação continua restrita ao lote de importação. Nenhum APK interno vira amostra independente, nenhum arquivo é extraído e a divergência entre manifest.json e manifestos binários é sinalizada como incompatibilidade. O metadado externo não comprova a presença de todas as variantes. APKS `toc.pb` e AAB protobuf permanecem sem interpretação nesta etapa.

### Tabela de conteúdo APKS (`toc.pb`)

O inspetor lê `toc.pb` com limite de 4 MiB e interpreta a mensagem protobuf pública `BuildApksResult`: package name, variantes, módulos, dependências, caminhos de APK, split IDs, master split e presença de targeting. O leitor ignora campos desconhecidos pelos respectivos wire types e rejeita estruturas truncadas ou inválidas. O resultado é persistido no SQLite versão 7 e exibido na tela Analisar.

Os caminhos do TOC são correlacionados com entradas APK do mesmo contêiner. A correlação respeita o limite de 32 APKs internos e não classifica como ausentes os caminhos que ficaram fora da janela de inspeção. Os manifestos binários internos continuam sendo a fonte para package/split de cada APK. A leitura do TOC não extrai arquivos, não executa código e não garante completude de variantes.

A interpretação de targeting cobre ABI, multi-ABI, idioma, densidade e SDK mínimo, incluindo valores e alternativas declaradas por APK. Dimensões adicionais do protobuf permanecem preservadas apenas como presença genérica de targeting e não são inferidas.

A tela apresenta targeting por APK: ABI/multi-ABI, idioma, densidade e SDK mínimo, com valores e alternativas. O analisador usa os metadados do TOC para verificar package name, caminhos declarados e dependências entre módulos. O limite de 32 APKs e o estado de truncamento impedem conclusões falsas sobre entradas não inspecionadas.
