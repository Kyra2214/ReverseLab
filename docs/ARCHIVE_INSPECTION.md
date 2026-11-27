# Inventário interno de contêineres

## Escopo

O inventário abre somente a cópia privada importada e lê o diretório central ZIP. Nenhuma entrada é extraída, executada ou modificada. O inventário registra nomes, tamanhos comprimidos/descomprimidos, diretórios e alertas estruturais.

## Assinaturas internas reconhecidas

- APK: AndroidManifest.xml, classes*.dex, resources.arsc e bibliotecas em lib/.
- XAPK: manifest.json e entradas APK internas.
- APKS: toc.pb e entradas APK internas.
- AAB: BundleConfig.pb e base/manifest/AndroidManifest.xml.
- OBB: contêiner ZIP declarado como OBB, com inventário das entradas.
- ZIP genérico: inventário sem confirmação de subtipo Android.

A presença desses elementos é evidência estrutural preliminar. Não confirma assinatura digital, compatibilidade de instalação ou vínculo entre pacotes.

## Limites de segurança

- Arquivos acima de 512 MiB não são inspecionados nesta etapa.
- O inventário percorre no máximo 50.000 entradas e limita o total de caracteres dos nomes.
- A interface armazena no máximo 300 nomes de entradas por inspeção, mantendo a contagem até o limite global.
- Alertas são limitados para evitar consumo descontrolado de memória.
- Caminhos absolutos, segmentos .., nomes excessivos, entradas duplicadas, tamanhos desproporcionais e taxas de compressão incomuns são sinalizados.
- A inspeção usa metadados ZIP e não descompacta conteúdo.

## Persistência

Cada execução de inventário recebe ID próprio e é adicionada ao histórico SQLite. Uma nova inspeção não substitui o resultado anterior. A migração do catálogo é aditiva e preserva registros existentes.

## Limitações pendentes

A etapa seguinte deve interpretar package name e versões no manifesto binário, decodificar toc.pb/manifestos quando aplicável e resolver associação de splits/OBB com evidências mais fortes.


## Leitura de manifestos de APKs internos

Para XAPK/APKS, o inspetor percorre até 32 entradas APK, abre cada uma como fluxo ZIP aninhado e lê somente o manifesto raiz. Não há extração em disco, cópia adicional nem execução. A leitura é limitada a 1ª entrada por APK, 2 MiB por manifesto e 16 MiB agregados. O resultado mantém o caminho virtual e a identidade declarada, sem criar amostra independente nem afirmar completude.

## Manifesto XAPK

Para XAPK, o inventário também interpreta o `manifest.json` da raiz, limitado a 256 KiB. São lidos package name, nome, versão e caminhos declarados em `split_apks`; caminhos inseguros são ignorados. O parsing é persistido no SQLite (migração aditiva versão 6) e exibido na tela Analisar. A identidade declarada no JSON é comparada com os package names dos APKs internos. Divergências e arquivos declarados que não aparecem no inventário são sinalizados, sem extração ou execução. O JSON é metadado auxiliar: não substitui a leitura dos manifestos binários nem prova a completude do conjunto.

## Tabela de conteúdo APKS (toc.pb)

Para APKS, o inspetor interpreta o protobuf `BuildApksResult` de Bundletool com leitor de wire format próprio, sem dependência externa e com limite de 4 MiB. São extraídos package name, variantes, módulos, dependências entre módulos, caminhos dos APKs, split IDs, indicação de master split, tipo de metadado e presença de targeting. Campos protobuf desconhecidos são ignorados conforme seu wire type; estruturas inválidas recebem estado explícito. Os caminhos são validados antes de serem aceitos. A comparação entre caminhos declarados e entradas internas respeita o limite de 32 APKs: quando a cobertura é parcial, o sistema não acusa falsamente os itens além do limite como ausentes.

Os dados são persistidos no SQLite versão 7 por migração aditiva e apresentados na tela Analisar. A tabela de conteúdo descreve o pacote produzido, não substitui os manifestos binários de cada APK nem comprova que todas as variantes necessárias a um dispositivo específico estão presentes.

### Targeting de dispositivos no toc.pb

O leitor também decodifica os valores e alternativas de ABI, multi-ABI, idioma, densidade de tela e SDK mínimo nas descrições APK. Os dados são apresentados como targeting declarado pelo Bundletool, não como recomendação de instalação. Outras dimensões, como textura, sanitizer, grupos de dispositivos e países, ainda não são interpretadas semanticamente.

A versão atual do parser de `toc.pb` também decodifica targeting de ABI, multi-ABI, idioma, densidade e SDK mínimo. Cada APK mantém os valores e alternativas retornados pelo protobuf. A leitura é descritiva e não seleciona variantes automaticamente.
