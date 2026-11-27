# Navegação e telas iniciais

## Navegação global

A barra inferior mantém cinco destinos: Início, Biblioteca, Analisar, IA e Ajustes. Cada destino substitui o conteúdo central sem remover a barra inferior.

## Início

A tela inicial funciona como central de comando, com apresentação do laboratório, acesso direto à área Analisar e atalhos para Projetos, Biblioteca Técnica e Biblioteca Visual. Os atalhos abrem a área correspondente da Biblioteca.

## Biblioteca

A raiz da Biblioteca apresenta três áreas distintas: Projetos, Biblioteca Técnica e Biblioteca Visual. Cada área possui uma tela própria, breadcrumb visual e retorno para a raiz.

Os estados vazios são explícitos e não simulam conteúdo. Os itens aparecerão quando os fluxos de importação e catalogação forem implementados.

## Analisar: seleção, importação e histórico

A tela Analisar abre o seletor múltiplo de documentos do Android. Cada URI selecionado recebe permissão persistente de leitura e é exibido pelo nome fornecido pelo provedor de documentos.

Após a seleção, um executor em segundo plano lê somente 32 bytes do cabeçalho. Ao solicitar importação, o app cria um registro PENDING, copia o conteúdo para a área privada, calcula SHA-256 em streaming e atualiza o estado para IMPORTED, PARTIAL ou FAILED. O arquivo original nunca é alterado.

A tela também apresenta o histórico agrupado por lote e a ocupação real da pasta privada de amostras. O usuário pode atualizar o histórico manualmente. A ocupação não representa o espaço total do aplicativo.

## Inventário interno de contêineres

Dentro de cada lote expandido, amostras importadas com formato de contêiner podem receber uma inspeção estrutural manual. O inventário mostra entradas, tamanhos, marcadores de manifesto/DEX/recursos/bibliotecas e alertas. A operação lê o diretório central ZIP, não extrai nem executa entradas e preserva o histórico de inspeções anteriores.

## Áreas ainda em construção

IA e Ajustes possuem destinos de navegação próprios, mas seus fluxos funcionais serão entregues nas fases correspondentes. A interface identifica essas áreas como em desenvolvimento e não apresenta controles sem ação.
