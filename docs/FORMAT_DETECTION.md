# Detecção inicial de formatos

## Método

A inspeção lê somente os primeiros 32 bytes do documento via URI SAF. Não extrai arquivos, não percorre o contêiner, não executa código e não cria cópia temporária.

## Assinaturas reconhecidas

- DEX: magic dex\n.
- ELF: magic 0x7F ELF.
- Imagens: PNG, JPEG, GIF e WebP.
- PDF: %PDF-.
- Vídeo: contêineres com ftyp e EBML/Matroska.
- Áudio: ID3/MP3, FLAC, Ogg e WAV.
- Contêiner ZIP: assinatura local/central ZIP.

Quando o cabeçalho é ZIP, a extensão pode classificar provisoriamente APK, OBB, XAPK, APKS ou AAB. Isso confirma o contêiner ZIP e registra a extensão como pista; não comprova ainda os arquivos internos ou a integridade do pacote. A inspeção interna de manifestos, splits, toc.pb e OBB será feita durante a importação controlada.

## Confiança

- Assinatura binária: identificação por bytes de cabeçalho.
- Assinatura do contêiner + extensão: ZIP confirmado e subtipo sugerido pela extensão.
- MIME do provedor: indicação fornecida pelo document provider, não prova criptográfica.
- Não identificado: extensão incompatível ou cabeçalho insuficiente.

## Limites

A inspeção é preliminar. Não verifica assinatura digital, estrutura interna completa, malware, integridade criptográfica nem compatibilidade de instalação. Esses assuntos pertencem a módulos posteriores.
