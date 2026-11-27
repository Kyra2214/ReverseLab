# core:analysis

Camada responsável por orquestrar análise e integrar parsers/adaptadores de ferramentas determinísticas.

Responsabilidades iniciais:
- Detectar formatos por assinaturas binárias curtas e pistas de MIME/extensão.
- Inspecionar documentos via URI SAF sem extração, execução ou cópia temporária.
- Inventariar metadados de contêineres ZIP/APK/OBB/XAPK/APKS/AAB sem extrair entradas.
- Aplicar limites de tamanho, quantidade de entradas, comprimento de caminhos e volume de avisos durante o inventário.
- Manter extração determinística e interpretação por IA separadas.

A identificação inicial por cabeçalho é preliminar. O inventário interno verifica nomes e metadados das entradas, mas ainda não interpreta AndroidManifest.xml, certificados, conteúdo DEX ou manifestos protobuf.
