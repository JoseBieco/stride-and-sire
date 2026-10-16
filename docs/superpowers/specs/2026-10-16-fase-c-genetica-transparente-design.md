# Fase C — Genética Transparente

Status: aprovado para plano de implementação
Mod: Stride & Sire (Forge, Minecraft 26.3, Java 25)
Esta é a Fase 3 de 4 do roadmap do mod. Depende da arquitetura de data attachment
e dos handlers de evento já estabelecidos nas Fases A (Ferraduras) e B (Alforjes).
Fase seguinte (fora de escopo aqui): D — Genética Mendeliana de Pelagem, que vai
reaproveitar o vocabulário de dominante/recessivo introduzido aqui para os traços,
mas aplicado a cor de pelagem.

## 1. Objetivo

Tornar visível e controlável a genética de atributos e comportamento dos
equídeos, substituindo o RNG opaco da criação vanilla por um sistema em que o
jogador pode inspecionar, prever e selecionar deliberadamente. Cobre três
entregas que compartilham o mesmo alicerce de dados: a Lente do Criador
(inspeção), a criação guiada por dieta (direcionamento de herança), e os traços
de linhagem (mutações passivas acumuláveis).

## 2. Escopo

- Mobs afetados: todos os subtipos de `AbstractHorse` (Cavalo, Burro, Mula,
  Cavalo-Esqueleto, Cavalo-Zumbi), consistente com as Fases A e B.
- Três atributos recebem o sistema de nota: Velocidade, Capacidade de Salto, Vida
  Máxima.
- Três traços de linhagem: Dorso Largo (Draft), Casco Firme (Sure-footed), Sangue
  Frio (Fearless). Comportamento de cada um:
  - **Dorso Largo** — desbloqueia o Alforje Duplo de 18 slots (Fase B).
  - **Casco Firme** — sobe automaticamente elevações de 1,5–2 blocos.
  - **Sangue Frio** — não empina nem derruba o montador com dano repentino de
    zumbis ou explosões próximas de creepers.
- Genética de cor de pelagem é explicitamente fora de escopo (Fase D).

## 3. Modelo de Atributos Expandido

O teto de cada um dos 3 atributos é elevado em **+20% acima do máximo vanilla**
(ex.: Vida Máxima vanilla até 30 passa a ir até 36). O valor de cada atributo é
traduzido numa nota de E a S, dividida da seguinte forma sobre a faixa de 0% a
120% do máximo vanilla original:

| Nota | Faixa | Como alcançar |
|---|---|---|
| E | 0–25% | Selvagem ou cruzamento |
| D | 25–50% | Selvagem ou cruzamento |
| C | 50–75% | Selvagem ou cruzamento |
| B | 75–100% (teto vanilla) | Selvagem ou cruzamento |
| A | 100–110% (acima do vanilla) | **Só cruzamento guiado por dieta** |
| S | 110–120% (teto expandido) | **Só cruzamento guiado por dieta** |

Cavalos selvagens (spawn natural) nunca excedem 100% em nenhum atributo — ficam
sempre entre E e B. Só o cruzamento usando um item de dieta (seção 4) pode
produzir um filhote com algum atributo nas faixas A ou S.

## 4. Algoritmo de Cruzamento

### Sem item de dieta

Fórmula padrão vanilla (média dos dois pais + variação aleatória), mas com o
resultado limitado à faixa 0–100% — nunca produz nota acima de B.

### Com item de dieta

Usar um dos três itens abaixo para iniciar o acasalamento libera a faixa
expandida (0–120%) para os **três** atributos do filhote nesse cruzamento
específico:

| Item | Atributo favorecido |
|---|---|
| Cenoura Dourada | Velocidade |
| Maçã Dourada | Vida Máxima |
| Melancia Reluzente | Capacidade de Salto |

Para cada um dos três atributos: o valor padrão é a média dos pais + variação
aleatória, agora podendo ultrapassar 100% até o teto de 120%. Para o **atributo
favorecido** pelo item usado, há adicionalmente **20% de chance** de, em vez da
média, o filhote herdar a partir do valor do pai superior nesse atributo mais um
bônus aleatório adicional até 120% — garantindo uma herança do "melhor pai" com
chance real de entrar em A/S.

A Maçã de Ouro Enriquecida da ideia original do projeto foi descartada em favor
deste sistema de dieta — não existe mais como item separado.

## 5. Traços de Linhagem

Cada um dos 3 traços tem, por cavalo, um dos três estados: **ausente**,
**latente** (carregado geneticamente, herdável, mas sem manifestar o
comportamento nesse cavalo) ou **manifesto** (comportamento ativo). Um mesmo
cavalo pode acumular mais de um traço simultaneamente (ex.: Dorso Largo e Casco
Firme ao mesmo tempo).

A cada cruzamento, cada um dos 3 traços é sorteado de forma independente para o
filhote:

- Se **nenhum dos pais** carrega o traço (nem latente, nem manifesto): chance
  baixa de mutação espontânea — 2% de nascer latente, 1% de nascer manifesto.
- Se **um dos pais** carrega o traço (latente ou manifesto): chance bem maior —
  35% de nascer latente, 15% de nascer manifesto.

Esses valores são estimativas de design, ajustáveis no balanceamento sem mudar a
arquitetura — a regra fixa é: sem um pai portador, é mutação rara; com um pai
portador, a linhagem favorece fortemente manter o traço.

Um traço latente não ativa nenhum comportamento no cavalo que o carrega — ele só
existe para ser potencialmente passado adiante e para ser revelado pela Lente do
Criador (seção 6).

## 6. Lente do Criador (Breeder's Spyglass)

Shift + clique direito num cavalo com uma Luneta equipada abre uma tela somente
leitura (sem consumir a Luneta, sem durabilidade extra, repetível à vontade)
mostrando:

- A nota (E a S) de Velocidade, Capacidade de Salto e Vida Máxima.
- A lista de traços de linhagem presentes, com os latentes marcados
  distintamente dos manifestos (ex.: "Dorso Largo (latente)").

## 7. Arquitetura de Dados

- **Atributos numéricos**: continuam vivendo exclusivamente nos atributos
  nativos do Forge/Minecraft no cavalo — nenhuma duplicação de dado. O teto é
  elevado via o mesmo mecanismo de evento de modificação de atributo já usado na
  Fase A (modificador de velocidade da Ferradura de Ouro). A nota (E–S) é sempre
  calculada na hora a partir do valor real do atributo, nunca armazenada
  separadamente.
- **Traços**: um novo data attachment `HorseGenetics` no `AbstractHorse`
  (seguindo exatamente o padrão de attachment das Fases A/B), guardando o estado
  dos 3 traços (ausente/latente/manifesto) — já que não existe equivalente
  nativo para isso.
- **Hook de cruzamento**: no evento de nascimento do filhote, aplica as regras
  das seções 4 e 5, verificando se um item de dieta foi usado para iniciar o
  acasalamento.
- **Utilitário de nota**: uma função `attributeGrade(valor, min, maxExpandido) -> Grade`
  consultada pela Lente do Criador e por qualquer debug/HUD futuro.
- A Lente do Criador lê o atributo nativo (via `attributeGrade`) e o attachment
  `HorseGenetics` para montar sua tela.

## 8. Plano de Testes

GameTests cobrindo:
- Cruzamento sem item de dieta nunca produz atributo acima de 100% (nota nunca
  sai de E–B).
- Cruzamento com cada um dos 3 itens de dieta consegue produzir valores na
  faixa 100–120% (A/S) no atributo favorecido, com frequência maior que nos
  outros dois atributos no mesmo cruzamento.
- Herança de traço: um pai com o traço manifesto aumenta a chance do filhote
  nascer com o traço (latente ou manifesto), comparado a pais sem o traço.
- Um cavalo selvagem (spawn natural, sem intervenção do jogador) nunca
  ultrapassa 100% em nenhum atributo.
- A Lente do Criador lê corretamente os 3 atributos (nota correta pro valor) e
  lista traços latentes com a marcação distinta dos manifestos.
- Um cavalo com um traço latente (não manifesto) não ativa o comportamento do
  traço (ex.: ainda empina com dano repentino mesmo carregando Sangue Frio
  latente).

## 9. Fora de Escopo / Itens Abertos

- Genética de cor de pelagem — Fase D, que reaproveita o vocabulário
  latente/manifesto (dominante/recessivo) introduzido aqui.
- Os valores exatos de chance de mutação/herança de traço (2%, 35%, etc.) e a
  chance de 20% de herança do melhor pai são estimativas de design, ajustáveis
  no balanceamento/playtesting sem alterar a arquitetura.
- A UI exata da Lente do Criador (layout, se mostra barra numérica além da
  letra) fica para decisão na implementação.
