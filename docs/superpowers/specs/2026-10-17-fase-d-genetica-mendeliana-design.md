# Fase D — Genética Mendeliana de Pelagem

## Visão Geral

A Fase D introduz 3 pelagens de cavalo exclusivas e colecionáveis, inexistentes na paleta vanilla, controladas por genética Mendeliana clássica (um gene de locus único, dominante/recessivo, por pelagem). Diferente da Fase C (atributos ocultos e traços de linhagem, com herança guiada por dieta), a Fase D é deliberadamente mais simples: reprodução puramente Mendeliana, sem nenhuma influência de item ou dieta. O objetivo é um "endgame para colecionadores" — pelagens que nunca aparecem em cavalos selvagens e só podem ser obtidas através de um pequeno programa de seleção de linhagem guiado pelo jogador.

Esta fase se aplica **exclusivamente ao Cavalo (`Horse`)**. Burro, Mula, Cavalo-Esqueleto e Cavalo-Zumbi não possuem variação de cor/marcação no vanilla e ficam fora do escopo.

## Escopo

### Pelagens Exclusivas

| Pelagem | Gene | Descrição visual |
|---|---|---|
| Preto Absoluto | `Obsidian` | Preto puro, sem nenhuma marcação, tom mais escuro e com leve brilho em relação ao "Black" vanilla |
| Albino | `Albino` | Branco puro, crina e cauda também brancas, olhos rosados/vermelhos (visualmente distinto do "White" vanilla, que mantém crina contrastante) |
| Pangaré Prateado | `Silver Dapple` | Padrão de manchas metálicas/prateadas, sem equivalente em nenhuma combinação vanilla |

Cada pelagem é controlada por **um gene independente de locus único**: `AA` (homozigoto dominante, nenhum traço), `Aa` (heterozigoto, portador sem efeito visual) ou `aa` (homozigoto recessivo, pelagem manifesta). Os 3 genes são independentes entre si — um cavalo pode, em teoria, ser `aa` puro em mais de um gene simultaneamente, embora isso seja estatisticamente raro.

### Fora de Escopo

- Nenhum efeito mecânico/estatístico associado às pelagens exclusivas — são puramente cosméticas.
- Nenhum item ou mecânica de dieta influencia a herança destes genes (ao contrário da Fase C).
- Nenhuma pelagem exclusiva nova fora das 3 listadas acima nesta fase.
- Burro, Mula, Cavalo-Esqueleto, Cavalo-Zumbi.

## Modelo Genético e Spawn Selvagem

Cavalos selvagens sorteiam a zigosidade de cada um dos 3 genes de forma independente, no momento do spawn, com baixa probabilidade de `Aa` (portador) e **nunca** `aa` puro:

- `AA` (nenhum traço): probabilidade alta (maioria dos spawns).
- `Aa` (portador, sem efeito visual): probabilidade baixa: 15% por gene (calibrado após playtest; era 3%).
- `aa` (pelagem manifesta): 0% em spawn selvagem. Só surge através de reprodução guiada.

Isso cria um motivo orgânico para o jogador explorar o mundo caçando cavalos selvagens e testando-os com a Lente do Criador até encontrar portadores, antes de iniciar um programa de cruzamento para estabilizar o alelo recessivo na linhagem.

## Reprodução: Herança Mendeliana Pura

Ao cruzar dois cavalos, cada um dos 3 genes é sorteado **independentemente** pela tabela clássica de Punnett, sem nenhuma influência de item, dieta ou outro sistema:

| Cruzamento | Resultado no filhote |
|---|---|
| `AA` × `AA` | 100% `AA` |
| `AA` × `Aa` | 50% `AA`, 50% `Aa` |
| `AA` × `aa` | 100% `Aa` |
| `Aa` × `Aa` | 25% `AA`, 50% `Aa`, 25% `aa` |
| `Aa` × `aa` | 50% `Aa`, 50% `aa` |
| `aa` × `aa` | 100% `aa` |

Essa lógica vive em uma classe de lógica pura, `HorseCoatInheritance`, testável isoladamente via JUnit 5 — mesmo padrão usado para `AttributeInheritance`/`TraitInheritance` na Fase C, mas sem nenhuma dependência de itens de dieta.

## Renderização e Resolução de Conflito Visual

Quando um cavalo tem pelo menos um gene `aa` manifesto, a pelagem especial correspondente **substitui completamente** a textura vanilla de cor+marcação daquele cavalo — a cor/marcação vanilla "por baixo" deixa de ser renderizada.

Se um cavalo manifestar `aa` em **mais de um** gene simultaneamente, qual das pelagens é exibida é decidido por **sorteio aleatório no nascimento** (não a cada render) e fixado permanentemente nos dados do cavalo — o sorteio não é repetido em renders subsequentes nem ao recarregar o mundo.

A renderização é implementada como uma nova `HorseCoatOverrideLayer`, verificada antes da camada de cor vanilla: se houver gene manifesto, aplica a textura especial sorteada; caso contrário, a renderização vanilla de cor/marcação segue normalmente, sem nenhuma interferência.

## Arquitetura de Dados

### `HorseCoatGeneticsAttachment`

Novo `AttachmentType`, independente do `HorseGenetics` da Fase C (separação de responsabilidades: genética de cor é Mendeliana simples, enquanto atributos/traços da Fase C usam pesos guiados por dieta — misturar as duas num único attachment acoplaria lógicas conceitualmente diferentes). Armazena:

- Um campo `Zygosity { HOMOZYGOUS_DOMINANT, HETEROZYGOUS, HOMOZYGOUS_RECESSIVE }` por gene (3 campos: `obsidian`, `albino`, `silverDapple`).
- O resultado já sorteado de qual pelagem exibir, quando mais de um gene está manifesto simultaneamente (fixado no nascimento, serializado junto ao cavalo para persistir entre sessões).

Um `HorseCoatGeneticsAccess` expõe leitura/escrita controlada sobre o attachment, no mesmo padrão dos `Access` já usados nas Fases A, B e C.

### Handlers

- `HorseCoatSpawnHandler` — sorteia a zigosidade inicial dos 3 genes no evento de spawn de cavalos selvagens (frequência rara de `Aa`, nunca `aa`).
- `HorseCoatBreedingHandler` — conecta-se ao mesmo hook de nascimento já usado pela Fase C para aplicar `HorseCoatInheritance` aos 3 genes do filhote, e sorteia o desempate visual quando aplicável.

## Lente do Criador (extensão)

A tela existente (`BreederSpyglassScreen`, introduzida na Fase C) ganha 3 linhas adicionais, uma por gene, mostrando o rótulo do genótipo (ex: "Pangaré Prateado: Aa (portador)", "Preto Absoluto: aa (puro)", "Albino: AA (nenhum traço)") — reaproveitando o layout de linhas já estabelecido para atributos e traços de linhagem.

## Conquistas

Três novos advancements, um por pelagem:

- `strideandsire:obsidian_coat`
- `strideandsire:albino_coat`
- `strideandsire:silver_dapple_coat`

Cada um é disparado por um `CriteriaTrigger` customizado (`BredPureCoatTrigger`), acionado **exclusivamente no momento em que `HorseCoatBreedingHandler` grava `aa` puro no filhote recém-nascido**. A conquista nunca é concedida retroativamente a cavalos já existentes no mundo, nem disparada apenas por revelar o genótipo via Lente do Criador — precisa ser o nascimento de um espécime puro.

## Plano de Testes

- **JUnit 5** sobre `HorseCoatInheritance`: as 6 combinações de cruzamento possíveis por gene (tabela de Punnett acima), incluindo o sorteio de desempate entre pelagens quando mais de um gene manifesta `aa` simultaneamente.
- **GameTest** cobrindo o fluxo completo:
  - Spawn selvagem nunca gera `aa` puro em nenhum dos 3 genes (amostragem estatística sobre múltiplos spawns).
  - Cruzamento `Aa` × `Aa` produzindo um filhote `aa` aciona corretamente a `HorseCoatOverrideLayer` (textura especial visível) e a conquista correspondente.
  - A Lente do Criador exibe o genótipo correto para os 3 genes em um cavalo de teste com zigosidade conhecida.
  - Review Focus: regressão cruzada com as Fases A, B e C (equipar ferradura/alforje e verificar herança genética de atributos/traços da Fase C continuam funcionando normalmente em um cavalo que também carrega os genes de pelagem da Fase D).
