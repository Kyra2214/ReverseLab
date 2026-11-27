# Seleção de documentos e permissões SAF

## Fluxo implementado

A tela Analisar usa `ActivityResultContracts.OpenMultipleDocuments` para abrir o seletor de documentos do Android e permitir seleção múltipla de arquivos de diferentes formatos.

Para cada URI retornado:
1. Solicita persistência apenas da permissão de leitura concedida pelo seletor.
2. Confirma que o URI aparece nas permissões persistentes do ContentResolver.
3. Consulta o nome de exibição por `OpenableColumns.DISPLAY_NAME`.
4. Apresenta o documento na lista de acessos persistentes.

Ao abrir a tela novamente, a lista é reconstruída a partir das permissões de leitura persistentes que o app ainda possui.

## Segurança e escopo

- Não solicitar `MANAGE_EXTERNAL_STORAGE` nem permissões amplas de armazenamento.
- Não converter URI em caminho absoluto.
- Não copiar, extrair, executar ou analisar os arquivos nesta etapa.
- A seleção e a concessão de acesso não autorizam exclusão, sobrescrita ou deduplicação.
- A etapa seguinte implementará detecção de formatos e a importação controlada.
