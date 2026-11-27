# Erros, logs e estado persistente

## Erros de aplicação

`AppError` mantém código estável, severidade, possibilidade de repetição, mensagem amigável e ID de correlação. A mensagem técnica é opcional e não deve ser exibida diretamente como mensagem principal ao usuário.

Códigos iniciais incluem falha de acesso a arquivo, formato não suportado, arquivo inválido, divergência de integridade, armazenamento indisponível, falha de parsing, modelo indisponível, rede indisponível, provedor externo, transição inválida e erro desconhecido.

## Logs estruturados

`DiagnosticEvent` registra nome do evento, nível, ID de correlação, instante e atributos. `LogcatDiagnosticLogger` serializa eventos em JSON e `SensitiveDataRedactor` mascara campos cujas chaves indiquem credenciais/segredos, tokens Bearer e segredos em parâmetros de URL.

Não registrar conteúdo integral de APK/OBB, chaves, tokens, senhas ou cabeçalhos de autenticação. Eventos devem usar IDs e metadados mínimos.

## Persistência de execuções

`SqliteAnalysisRunStore` persiste metadados de `AnalysisRun` em SQLite e mantém um histórico de transições em `analysis_run_events`.

- Inserção usa falha em ID duplicado, sem substituir silenciosamente uma execução anterior.
- Consultas permitem recuperar uma execução ou listar por projeto.
- Transição é condicional ao estado esperado e valida `ProcessingState.canTransitionTo`.
- A atualização do estado e a gravação do evento ocorrem na mesma transação.
- Não existe método de apagar execução ou histórico.
- Migrações futuras devem preservar os dados; não usar `DROP TABLE` como atalho.

## Limites desta etapa

O logger atual usa Logcat e o armazenamento usa SQLite local. Rotação de logs, exportação de diagnóstico, criptografia do banco e migrações futuras serão avaliadas em fases próprias. Nenhum build ou teste foi executado.
