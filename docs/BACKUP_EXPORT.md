# Backup e exportação

## Formato atual

A exportação cria um pacote ZIP `ReverseLabBackup` com:

- `manifest.json`: versão do formato, instante de criação, versão do esquema e conteúdo incluído;
- `analysis-state.db`: snapshot consistente do banco local de execuções e eventos.

O pacote declara explicitamente que não contém APKs/OBBs originais nem credenciais. Amostras originais permanecem sob o controle do usuário e não são copiadas implicitamente.

## Destino e autorização

A interface deve obter o destino por Android Storage Access Framework, preferencialmente `ACTION_CREATE_DOCUMENT`, para criar um novo arquivo. Exportar para um documento existente é uma sobrescrita e exige `ExplicitUserConfirmation` que corresponda ao ID da solicitação, à ação e ao URI exato. A operação também precisa ter sido iniciada pelo usuário.

O exportador usa `StorageGovernance` antes de abrir o destino. Não há limpeza nem exclusão de backups anteriores.

## Consistência do SQLite

Antes de copiar, `SqliteAnalysisRunStore` solicita `wal_checkpoint(FULL)`, fecha o helper e copia o arquivo principal do banco. As operações do store são sincronizadas com a geração do snapshot. Se o checkpoint não puder ser concluído, a exportação falha em vez de produzir um backup declarado como consistente.

## Restauração

A restauração automática ainda não está implementada. O formato inclui versão explícita para permitir uma etapa futura de inspeção, compatibilidade e restauração em um banco novo. Não restaurar por sobrescrita do banco ativo. A futura restauração deverá validar o manifesto, inspecionar a integridade e oferecer criação de perfil/banco separado, preservando o banco atual.

## Limites

O backup atual cobre somente o banco de execuções e seus eventos. Não inclui arquivos importados, miniaturas, relatórios exportados ou modelos locais. Esses itens exigirão inventário e seleção explícita em versões futuras do formato.
