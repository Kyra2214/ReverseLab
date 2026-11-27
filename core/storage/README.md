# core:storage

Camada responsável pela persistência local, importação controlada, procedência, permissões SAF, políticas de preservação e registros persistentes de análise.

Regras obrigatórias:
- Nenhuma exclusão, limpeza, sobrescrita ou deduplicação física automática.
- Operações destrutivas exigem ação iniciada pelo usuário e confirmação vinculada ao conjunto exato de alvos apresentado.
- O acesso a documentos usa permissões concedidas pelo Storage Access Framework; não solicitar acesso amplo ao armazenamento.
- A liberação de uma permissão SAF revoga o acesso do app, mas nunca apaga o documento.
- Arquivos originais e derivados mantêm identidades e procedência próprias.
- Histórico de execuções não possui método de exclusão; mudanças de estado são registradas em tabela de eventos.
- Logs estruturados passam por redação de segredos antes de chegar ao Logcat.
