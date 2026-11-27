# core:storage

Camada responsável pela persistência local, importação controlada, procedência, permissões SAF, políticas de preservação, registros persistentes de análise, catálogo de amostras, histórico/ocupação e exportação de backup.

Regras obrigatórias:
- Nenhuma exclusão, limpeza, sobrescrita ou deduplicação física automática.
- Operações destrutivas exigem ação iniciada pelo usuário e confirmação vinculada ao conjunto exato de alvos apresentado.
- O acesso a documentos usa permissões concedidas pelo Storage Access Framework; não solicitar acesso amplo ao armazenamento.
- A liberação de uma permissão SAF revoga o acesso do app, mas nunca apaga o documento.
- Arquivos originais e derivados mantêm identidades e procedência próprias.
- Histórico de execuções e catálogo de amostras não possuem métodos de exclusão.
- Hashes detectam duplicatas; não removem nem mesclam arquivos. Cada importação mantém registro e cópia próprios.
- Falhas de importação preservam os artefatos parciais e registram o estado.
- O relatório de ocupação contabiliza a pasta privada de amostras, sem alegar medir todo o espaço do aplicativo.
- Logs estruturados passam por redação de segredos antes de chegar ao Logcat.
- Backups são exportados somente por ação do usuário; sobrescrever destino existente exige confirmação exata.
