# core:analysis

Camada responsável por orquestrar análise e integrar parsers/adaptadores de ferramentas determinísticas.

Responsabilidades iniciais:
- Detectar formatos por assinaturas binárias curtas e pistas de MIME/extensão.
- Inspecionar documentos via URI SAF sem extração, execução ou cópia temporária.
- Manter extração determinística e interpretação por IA separadas.

A identificação de APK/XAPK/APKS/AAB/OBB por extensão e cabeçalho ZIP é preliminar; a estrutura interna será verificada durante a importação.
