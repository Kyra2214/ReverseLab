# ReverseLab

**Laboratório Android de engenharia reversa, descoberta tecnológica e inteligência reutilizável.**

O ReverseLab é um projeto Android voltado inicialmente à análise de APKs, conjuntos de split APKs, XAPK/APKS e arquivos OBB. Seu objetivo principal não é clonar aplicativos: é investigar como tecnologias, sistemas e mecanismos são construídos, registrar evidências e transformar descobertas em conhecimento técnico reutilizável.

O produto final de cada investigação é um conjunto organizado de **descobertas, explicações, evidências, referências visuais e possibilidades de implementação própria**.

## Objetivos centrais

- Analisar a estrutura e os componentes de pacotes Android.
- Investigar código, recursos, bibliotecas, formatos e comportamentos observáveis.
- Usar uma mini LLM local como primeira analista, com conclusões vinculadas a evidências.
- Criar um sandbox isolado para experimentos dinâmicos em etapas posteriores.
- Construir o **Knowledge Vault**, uma biblioteca cumulativa de conhecimento técnico e visual.
- Capturar ideias e padrões de engenharia para inspirar implementações originais em outros projetos.
- Preservar os arquivos e resultados até que o usuário autorize explicitamente sua exclusão.

## Princípios inegociáveis

1. **Descoberta, não clonagem:** priorizar a compreensão de mecanismos e ideias, não a reprodução integral de aplicativos ou a redistribuição de ativos protegidos.
2. **Evidência antes de conclusão:** separar fatos observados, interpretações da IA e hipóteses ainda não validadas.
3. **Privacidade e processamento local:** priorizar análise offline e não enviar amostras a serviços externos sem escolha explícita do usuário.
4. **Isolamento:** nunca executar amostras desconhecidas diretamente no ambiente principal.
5. **Sem exclusão automática:** nenhum arquivo importado, extraído, relatório, evidência, miniatura ou artefato de análise será apagado sem autorização explícita do usuário.
6. **Sem duplicação desnecessária:** trabalhar sobre a cópia importada pelo usuário e evitar cópias sucessivas. Criar apenas os artefatos realmente necessários, identificando-os e relacionando-os à amostra.
7. **Procedência:** registrar de qual amostra, arquivo, caminho e contexto veio cada descoberta ou imagem.
8. **Modularidade:** permitir trocar ferramentas de análise e modelos de IA sem reescrever o aplicativo inteiro.

## Roadmap

O plano completo, dividido em fases, entregáveis, critérios de aceite, arquitetura e decisões técnicas, está em [ROADMAP.md](ROADMAP.md).

## Estado do projeto

- [x] Repositório criado.
- [x] Visão e princípios iniciais documentados.
- [ ] Roadmap técnico aprovado e refinado.
- [ ] Implementação iniciada.

Nenhuma funcionalidade é considerada implementada apenas por estar descrita neste documento.
