# Detecção inicial de formatos

O detector usa um pequeno cabeçalho binário, MIME e extensão como pistas. A extensão nunca é tratada como prova isolada. A detecção inicial é preliminar e não extrai nem executa conteúdo.

Os formatos reconhecidos nesta camada incluem APK/ZIP, DEX, ELF, imagens, áudio, vídeo e PDF. Contêineres Android são encaminhados para inventário interno após a importação.

## Inspeção interna

Depois da importação, o inventário de contêineres lê o diretório central ZIP da cópia privada para identificar manifestos e entradas APK/DEX/OBB/APKS/AAB. Essa inspeção não extrai arquivos e possui limites explícitos de tamanho e quantidade de entradas. Consulte [Inventário interno de contêineres](ARCHIVE_INSPECTION.md).
