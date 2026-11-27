# Política de arquivos e permissões

## Princípio de preservação

O ReverseLab não executa limpeza automática, exclusão por falta de espaço, limpeza ao encerrar análise, substituição silenciosa ou deduplicação física automática.

## Gate para operações

`StorageGovernance` avalia cada solicitação antes de uma operação de armazenamento:

- Leitura, criação de derivado e exportação de cópia podem prosseguir conforme as permissões disponíveis.
- Exclusão, sobrescrita, limpeza e deduplicação física são destrutivas.
- Uma operação destrutiva iniciada por rotina automática é negada.
- Uma operação destrutiva iniciada pelo usuário exige confirmação explícita.
- A confirmação precisa corresponder ao ID da solicitação, à ação e ao conjunto exato de alvos exibido. Confirmação para outro conjunto não autoriza a operação.
- O gate decide se uma operação pode prosseguir; ele não executa exclusões nem substituições.

## Android Storage Access Framework

`SafPermissionManager` persiste somente a permissão de leitura concedida pelo seletor de documentos. O ReverseLab não solicita permissão ampla de armazenamento nesta fundação. Permissões são mantidas por URI, não por caminhos absolutos inventados.

A liberação de uma permissão SAF remove o acesso persistente do aplicativo, não o documento do dispositivo. Qualquer fluxo de liberação deve ser iniciado pelo usuário e explicado na interface.

## Limites desta etapa

Esta etapa implementa contratos e a gestão de permissões SAF. A interface do seletor, importação de pacotes, cópia para área de trabalho, persistência de amostras e operações concretas de arquivo serão implementadas nas fases correspondentes. Não foram executados builds ou testes.
