# core:storage

Camada responsável pela persistência local, importação controlada, procedência, permissões SAF e políticas de preservação.

Regras obrigatórias:
- Nenhuma exclusão, limpeza, sobrescrita ou deduplicação física automática.
- Operações destrutivas exigem ação iniciada pelo usuário e confirmação vinculada ao conjunto exato de alvos apresentado.
- O acesso a documentos usa permissões concedidas pelo Storage Access Framework; não solicitar acesso amplo ao armazenamento.
- A liberação de uma permissão SAF revoga acesso do app, mas nunca apaga o documento.
- Arquivos originais e derivados mantêm identidades e procedência próprias.
