# ADR-0003: Toolchain fixada e centralizada

- Estado: Aceita.
- Data: 2026-11-26.
- Contexto: versões dinâmicas tornam builds não reproduzíveis e dificultam a investigação de incompatibilidades.

## Decisão

Centralizar versões em `gradle/libs.versions.toml` e fixar:

- Android Gradle Plugin 9.3.1.
- Kotlin / Compose Compiler Plugin 2.4.21.
- Compose BOM 2025.11.00.
- AndroidX Activity Compose 1.11.0.
- Java 17, compileSdk 36 e targetSdk 36.
- Gradle 9.5.0 como distribuição planejada do Wrapper.

A matriz publicada pelo Kotlin 2.4.21 declara suporte ao AGP até 9.3.1 e Gradle até 9.7.0. A combinação será confirmada na validação final, sem executar builds durante a implementação.

## Consequências

- Atualizações de versões exigem alteração explícita e registro no changelog.
- Não utilizar curingas ou versões dinâmicas.
- O Wrapper binário oficial continua pendente de inclusão.
