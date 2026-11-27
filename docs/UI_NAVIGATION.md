# Navegação e telas iniciais

## Navegação global

A barra inferior mantém cinco destinos: Início, Biblioteca, Analisar, IA e Ajustes. Cada destino substitui o conteúdo central sem remover a barra inferior.

## Início

A tela inicial funciona como central de comando, com apresentação do laboratório, acesso direto à área Analisar e atalhos para Projetos, Biblioteca Técnica e Biblioteca Visual. Os atalhos abrem a área correspondente da Biblioteca.

## Biblioteca

A raiz da Biblioteca apresenta três áreas distintas: Projetos, Biblioteca Técnica e Biblioteca Visual. Cada área possui uma tela própria, breadcrumb visual e retorno para a raiz.

Os estados vazios são explícitos e não simulam conteúdo. Os itens aparecerão quando os fluxos de importação e catalogação forem implementados.

## Analisar: seleção e detecção inicial

A tela Analisar abre o seletor múltiplo de documentos do Android. Cada URI selecionado recebe permissão persistente de leitura e é exibido pelo nome fornecido pelo provedor de documentos. A lista é reconstruída a partir das permissões persistentes concedidas anteriormente.

Após a seleção, um executor em segundo plano lê apenas 32 bytes do cabeçalho e classifica assinaturas conhecidas. Não há cópia, extração, execução ou análise profunda do conteúdo. A detecção interna de pacotes e a criação de amostras pertencem à próxima etapa.

## Áreas ainda em construção

IA e Ajustes possuem destinos de navegação próprios, mas seus fluxos funcionais serão entregues nas fases correspondentes. A interface identifica essas áreas como em desenvolvimento e não apresenta controles sem ação.
