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
- Se um lote tiver exatamente um APK base importado com sucesso, splits e OBB importados no mesmo lote recebem associação heurística a esse grupo.
- Se houver mais de um APK base, splits e OBB permanecem em grupo ambíguo de lote. Se não houver base, a associação fica limitada ao lote.

Associação heurística não é confirmação de que os arquivos pertencem ao mesmo pacote. A interface sinaliza essa condição. A validação por package name, manifesto e metadados internos será feita em etapa posterior; associações confirmadas pelo usuário ou por manifesto terão confiança distinta.

## Identidade e procedência

Cada registro preserva SampleId, lote de importação, URI de origem, nome original, caminho privado relativo, formato preliminar, MIME, tamanho, SHA-256, estado, papel e grupo de associação. O URI SAF não é convertido em caminho absoluto.

## Limites desta etapa

Ainda não há extração de APK/XAPK/APKS/OBB, leitura de AndroidManifest.xml, resolução de package name, inspeção de splits por ABI/densidade/idioma ou análise de conteúdo. O original permanece intocado; a cópia privada é a amostra de trabalho.
