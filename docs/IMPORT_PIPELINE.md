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
