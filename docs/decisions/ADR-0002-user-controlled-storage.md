# ADR-0002: Armazenamento sob controle do usuário

- Estado: Aceita.
- Data: 2026-11-26.
- Contexto: amostras e evidências podem ter valor de investigação e não podem desaparecer por limpeza automática ou otimização de espaço.

## Decisão

Nenhum arquivo original, derivado, evidência, relatório ou artefato será apagado, sobrescrito ou deduplicado fisicamente automaticamente. Operações destrutivas exigem ação iniciada pelo usuário e confirmação vinculada ao conjunto exato de alvos exibido.

Acesso a arquivos selecionados usa Android Storage Access Framework e permissões persistentes mínimas, sem solicitar acesso amplo ao armazenamento.

## Consequências

- Hashes identificam duplicatas, mas não autorizam remoção.
- Liberar uma permissão SAF revoga acesso do app, não apaga o documento.
- O sistema deve informar espaço ocupado e permitir gerenciamento manual, sem expurgo automático.
