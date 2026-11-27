# Histórico de importação e uso de armazenamento

## Histórico

O histórico agrupa amostras pelo lote de importação e apresenta data, quantidade de arquivos, formatos, bytes registrados, estados IMPORTED/PARTIAL/PENDING/FAILED e candidatos a duplicata. Cada lote pode ser expandido para consultar nomes, hashes e nível de confiança das associações.

## Ocupação

A métrica de ocupação percorre somente a pasta privada de amostras do ReverseLab e soma os arquivos realmente presentes. Também apresenta quantidade de arquivos e registros do catálogo por estado.

Essa métrica não representa o espaço total do aplicativo: bancos SQLite, configurações, cache do sistema, modelos e outros dados ainda não entram nessa conta. A interface informa esse limite explicitamente.

## Atualização e preservação

O histórico é carregado ao abrir a área Analisar e pode ser atualizado manualmente. Após cada importação, a tela atualiza o histórico e a ocupação.

Não existe ação de limpeza automática. O histórico não oferece exclusão de registros, e a leitura de ocupação nunca remove arquivos. Amostras parciais, pendentes e possíveis duplicatas continuam visíveis.
