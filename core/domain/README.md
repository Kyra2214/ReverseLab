# core:domain

Camada de domínio independente da interface. Define identificadores tipados, localizadores de origem, evidências, escopos, execuções de análise, descobertas e estados compartilhados.

Regras:
- Não depender de Android UI, provedores de IA ou implementações concretas de armazenamento.
- Conclusões precisam referenciar evidências rastreáveis.
- Caminhos de evidência são relativos e não podem escapar da raiz da amostra.
- Execuções externas identificam o provedor; consumo e custo são opcionais quando o serviço não os informa.
- Estados de processamento só podem avançar por transições explicitamente permitidas.
