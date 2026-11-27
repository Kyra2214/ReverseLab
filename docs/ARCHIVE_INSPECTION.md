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

Para XAPK/APKS, o inspetor percorre até 32 entradas APK, abre cada uma como fluxo ZIP aninhado e lê somente o manifesto raiz. Não há extração em disco, cópia adicional nem execução. A leitura é limitada a 128 entradas por APK, 2 MiB por manifesto e 16 MiB agregados. O resultado mantém o caminho virtual e a identidade declarada, sem criar amostra independente nem afirmar completude.
