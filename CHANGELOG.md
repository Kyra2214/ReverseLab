# Changelog

Todas as alterações relevantes do ReverseLab serão registradas aqui.

## 2026-11-26

### Added
- Estrutura Android modular inicial com `:app`, `:core:domain`, `:core:storage` e `:core:analysis`.
- Aplicação inicial em Kotlin e Jetpack Compose com tema dark e navegação inferior de cinco áreas.
- Documento inicial de arquitetura, changelog e retomada da execução.
- Contratos tipados de domínio para projetos, amostras, evidências, escopos, execuções e descobertas.
- Máquina de estados com transições explícitas para processamento e retomada.
- Regras de integridade para caminhos de origem, SHA-256 e referências de evidência.
- Documentação dos contratos em `docs/DOMAIN_CONTRACTS.md`.
- Gate de governança para exclusão, sobrescrita, limpeza e deduplicação física, com confirmação exata de escopo.
- Persistência de permissão de leitura SAF por URI, sem solicitar acesso amplo ao armazenamento.
- Documentação da política de arquivos e permissões.
- Três ADRs para arquitetura modular, preservação de dados e toolchain fixada.
- Catálogo central de versões Gradle e inventário inicial de dependências/licenças.
- Modelo de erros com códigos, severidade, retry e IDs de correlação.
- Logs estruturados com redação de credenciais, tokens, segredos em URLs e cabeçalhos de autenticação.
- Persistência SQLite de execuções e histórico transacional de estados, sem API de exclusão.
- Exportação de backup ZIP com manifesto, snapshot SQLite consistente e controle de autorização do destino.
- Documentação do formato de backup, escopo e limitações de restauração.
- Tela inicial como central de comando, com atalhos reais para Analisar e para as três áreas da Biblioteca.
- Navegação funcional de Projetos, Biblioteca Técnica e Biblioteca Visual, com estados vazios explícitos.
- Seletor múltiplo do Android integrado à tela Analisar.
- Permissões persistentes de leitura SAF restauradas a partir dos URIs autorizados, sem cópia ou análise prematura dos arquivos.
- Detecção preliminar por assinatura de cabeçalho para contêineres Android, DEX, ELF, imagens, áudio, vídeo e PDF, sem extração ou execução.
- Importação controlada para área privada, com registro PENDING antes da cópia e hash SHA-256 calculado em streaming.
- Catálogo SQLite de amostras, identificação de duplicatas sem deduplicação física e associação heurística de APK base, splits e OBB com estado de confiança explícito.
- Histórico de importação agrupado por lote e medição de bytes/arquivos na pasta privada de amostras, sem limpeza automática.
- Inventário interno de APK/OBB/XAPK/APKS/AAB e ZIP por metadados do diretório central, sem extração, com limites de segurança e alertas estruturais.
- Histórico persistente de cada inspeção interna e migração SQLite aditiva do catálogo de amostras.
- Checkpoints completos `ReverseLab_CHECKPOINT_001.zip` a `ReverseLab_CHECKPOINT_012.zip`, verificados estruturalmente.

### Deferred
- Confirmação de associações entre APK base, splits e OBB e identificação de conjuntos incompletos, pendentes na Fase 1.
- Interpretação de AndroidManifest.xml, certificados e conteúdo DEX, nas fases seguintes.
- Execução de builds, testes e validações para a fase final, conforme a política de execução vigente.
- Gradle Wrapper binário oficial pendente de inclusão.


## 2026-11-27

### Added
- Revisão explícita de associações entre APK base, splits e OBB: confirmar grupo, separar grupo, vincular individualmente a um APK base ou separar uma amostra.
- Estado de domínio USER_REJECTED para registrar associações rejeitadas pelo usuário.
- Auditoria transacional SQLite das revisões, com valores anteriores/novos, ação e horário.
- Histórico das decisões de associação apresentado no lote de importação.

### Clarified
- Confirmação humana de associação não equivale à validação técnica de completude; comparação de manifestos e package name continua pendente.
- Nenhum arquivo importado ou derivado é removido durante a revisão de associações.

### Added
- Analisador estrutural conservador de conjuntos APK/split/OBB, integrado ao histórico da tela Analisar, distinguindo ausência de base, ambiguidade, manifesto ausente, inventário pendente e identidade não verificada sem declarar completude prematuramente.

### Deferred
- Integração da avaliação estrutural na tela de histórico e persistência de resultados derivados.
- Decodificação e comparação de AndroidManifest.xml, package name, splitName, ABI, densidade, idioma e dependências na Fase 2.


### Added
- Parser de Android Binary XML para extrair package, split, configForSplit e uses-split de APKs individuais, com limite de leitura.
- Persistência dos metadados do manifesto no inventário SQLite, com migração aditiva para catálogo versão 4.
- Correlação de package name, splitName, dependências e alvos configForSplit na avaliação de grupos APK/splits; divergências e referências ausentes são sinalizadas.
- Exibição dos metadados de manifesto no histórico da amostra.

### Limitations
- Manifestos de APKs aninhados em XAPK/APKS e manifestos protobuf de AAB ainda não são decodificados.
- A identidade coincidente não comprova a presença de todos os splits requeridos; a avaliação permanece conservadora.
- Builds e testes permanecem reservados para a validação final.

### Added
- Importação em lote agora inspeciona manifestos de APKs individuais, corrige o papel base/split pelo atributo `split` e agrupa APKs pelo package name coincidente.
- Associação baseada em manifesto recebe confiança MANIFEST_VERIFIED; OBB permanece explicitamente heurístico.

### Deferred
- Leitura de manifestos dos APKs internos de XAPK/APKS e promoção desses itens a amostras vinculadas.

### Fixed
- A migração do inventário detecta colunas já existentes, inclusive quando um catálogo antigo cria a tabela durante o mesmo upgrade.
- Grupos com múltiplos APKs base de mesmo package permanecem juntos como ambíguos, em vez de separar as bases dos splits.
- Documentação do avaliador atualizada para refletir o uso real dos manifestos binários.


## 2026-11-27

### Added
- Leitura limitada de manifestos Android Binary XML em APKs aninhados de XAPK/APKS, sem extração em disco.
- Persistência dos caminhos virtuais, package name, split, configForSplit, dependências uses-split e status de parsing dos APKs internos.
- Migração SQLite 5 para armazenar manifestos aninhados, preservando inventários antigos.
- Apresentação dos manifestos internos no resumo de inspeção da tela Analisar.

### Limits
- APKs internos continuam sendo referências virtuais ao contêiner original, não amostras independentes.
- A correlação lógica desses membros com APKs externos/base e a confirmação de completude permanecem pendentes.
- Limites: 32 APKs internos, 1ª entrada por APK, 2 MiB por manifesto e 16 MiB agregados.
- Builds/testes não executados; ficam para a validação final. ZIPs não gerados por instrução do usuário.

## 2026-11-27

### Added
- Correlação lógica de manifestos de APKs internos XAPK/APKS com APKs externos do mesmo lote, por package name.
- Avaliação de base externa correspondente, conjunto autocontido, base ausente, ambiguidade, identidade não interpretada e conflitos de split/dependências.
- Exibição dos resultados de associação virtual na tela Analisar, sem criar amostras independentes ou duplicar arquivos.

### Limitations
- A correlação não altera a associação persistida do contêiner e não prova completude de todas as variantes.
- Interpretação de toc.pb/manifest.json e confirmação de completude permanecem pendentes.
- Builds/testes seguem reservados à validação final.

## 2026-11-27

### Added
- Parsing limitado do `manifest.json` raiz de XAPK: package name, nome, versão, version code e caminhos de APKs declarados.
- Persistência dos metadados XAPK no SQLite versão 6 com migração aditiva.
- Exibição do manifesto XAPK na inspeção e comparação com package names dos APKs internos, sinalizando divergências e caminhos declarados ausentes.

### Limits
- `manifest.json` limitado a 256 KiB; caminhos inseguros não são aceitos.
- O manifesto externo não substitui a validação dos manifestos binários internos nem prova completude.
- Interpretação de APKS `toc.pb` e AAB protobuf permanece pendente; builds/testes seguem para a validação final.

## 2026-11-27

### Added
- Parser protobuf limitado para `toc.pb` APKS, baseado na estrutura pública `BuildApksResult` do Bundletool.
- Inventário de package name, variantes, módulos, dependências, caminhos APK, split IDs, master split e metadados de targeting.
- Persistência SQLite versão 7 com migração aditiva e exibição dos dados na tela Analisar.
- Correlação entre caminhos do TOC e APKs internos, com proteção contra falsos alertas quando o limite de 32 entradas impede cobertura completa.

### Limits
- `toc.pb` limitado a 4 MiB; APKs internos continuam limitados a 32 por inspeção.
- Targeting é registrado como presente, mas ABI/idioma/densidade ainda não são interpretados semanticamente.
- A presença de variantes no TOC não comprova completude para todos os dispositivos.

## 2026-11-27

### Added
- Decodificação de targeting no `toc.pb` para ABI, multi-ABI, idioma, densidade de tela e SDK mínimo, incluindo valores e alternativas.
- Exibição dos resumos de targeting por APK na tela Analisar e persistência junto ao inventário APKS.

### Limits
- Targeting de textura, sanitizer, grupos de dispositivos e países ainda não é interpretado semanticamente.
- A presença de variantes e targeting no TOC não comprova cobertura completa de todos os dispositivos.

## 2026-11-27

### Added
- Decodificação de targeting APKS para ABI, multi-ABI, idioma, densidade e SDK mínimo, incluindo alternativas.
- Proteção contra falsos alertas de APK ausente quando o inventário interno atingiu o limite de 32 entradas ou foi truncado.
- Validação de overflow em varints protobuf.

### Clarified
- A ausência de um caminho no inventário só é reportada quando a cobertura de inspeção do contêiner é completa.

## 2026-11-27

### Improved
- A tela Analisar exibe os nomes dos APKs externos que correspondem aos manifestos virtuais do XAPK/APKS, além dos caminhos internos e motivos de associação.
