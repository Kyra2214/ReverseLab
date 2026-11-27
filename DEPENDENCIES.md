# Inventário de dependências e licenças

Inventário das dependências declaradas diretamente no projeto. A árvore transitiva completa e os avisos de licença devem ser gerados na validação final, depois que a resolução de dependências estiver disponível.

| Dependência | Versão fixada | Uso | Licença declarada / origem |
|---|---:|---|---|
| Android Gradle Plugin | 9.3.1 | Build Android | Apache-2.0; distribuição oficial Android/Google |
| Kotlin Compose Compiler Plugin | 2.4.21 | Compilação Compose | Apache-2.0; JetBrains Kotlin |
| Compose BOM | 2025.11.00 | Alinhamento de versões Compose | BOM de metadados; componentes AndroidX relacionados usam Apache-2.0 |
| AndroidX Activity Compose | 1.11.0 | Integração Activity/Compose | Apache-2.0; repositório AndroidX |
| AndroidX Compose UI | via BOM | UI declarativa | Apache-2.0; repositório AndroidX |
| AndroidX Compose UI Tooling Preview | via BOM | Preview de UI | Apache-2.0; repositório AndroidX |
| AndroidX Compose UI Tooling | via BOM, debug | Ferramentas de desenvolvimento | Apache-2.0; repositório AndroidX |
| AndroidX Material 3 | via BOM | Componentes visuais | Apache-2.0; repositório AndroidX |
| AndroidX Material Icons Extended | via BOM | Ícones da navegação | Apache-2.0; repositório AndroidX |
| Gradle | 9.5.0 planejado | Sistema de build | Apache-2.0; Wrapper oficial ainda não incorporado |
| Java Development Kit | 17 | Toolchain | Distribuição do JDK a definir no ambiente de desenvolvimento |

## Repositórios e referências

- AndroidX: https://github.com/androidx/androidx (repositório declara Apache-2.0).
- Kotlin: https://github.com/JetBrains/kotlin (licença principal Apache-2.0; revisar avisos de terceiros incluídos na distribuição).
- Android Gradle Plugin: https://developer.android.com/build/releases/about-agp
- Compatibilidade Kotlin/Gradle/AGP: https://kotlinlang.org/docs/gradle-configure-project.html
- Apache License 2.0: https://www.apache.org/licenses/LICENSE-2.0

## Controles

- Não adicionar dependências sem registrar finalidade, versão, licença, origem e manutenção.
- Revisar licenças transitivas e componentes nativos antes da publicação.
- Gerar SBOM e relatório final de licenças na fase de estabilização.
- Não declarar a auditoria de dependências concluída apenas com este inventário direto.
