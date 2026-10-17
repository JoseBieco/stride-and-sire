<p align="center">
  <!-- TODO: substitua por um banner real (recomendado 1280x640, PNG ou JPG) -->
  <img src="docs/images/banner.png" alt="Banner do Stride & Sire" width="100%">
</p>

<h1 align="center">Stride & Sire</h1>

<p align="center"><i>Um mod Vanilla+ para Minecraft (Forge) que dá aos cavalos a atenção que eles merecem.</i></p>

<p align="center">
  <img alt="Minecraft" src="https://img.shields.io/badge/Minecraft-26.3-5C913B?logo=minecraft&logoColor=white">
  <img alt="Forge" src="https://img.shields.io/badge/Mod%20Loader-Forge-1B1B1B?logo=curseforge&logoColor=white">
  <img alt="Java" src="https://img.shields.io/badge/Java-25-ED8B00?logo=openjdk&logoColor=white">
  <img alt="Licença" src="https://img.shields.io/badge/Licença-MIT-blue">
  <img alt="Status" src="https://img.shields.io/badge/Status-Em%20Desenvolvimento-yellow">
</p>

<p align="center">
  <a href="#visão-geral">Visão geral</a> ·
  <a href="#fase-a--ferraduras">Ferraduras</a> ·
  <a href="#fase-b--alforjes-modulares">Alforjes</a> ·
  <a href="#fase-c--genética-transparente">Genética</a> ·
  <a href="#roadmap">Roadmap</a> ·
  <a href="#instalação">Instalação</a> ·
  <a href="README.md">English</a>
</p>

---

Stride & Sire expande todos os equídeos do jogo — Cavalo, Burro, Mula,
Cavalo-Esqueleto e Cavalo-Zumbi — com novos slots de equipamento, um sistema de
criação mais profundo e genética que o jogador consegue ver e planejar, sem fugir
do espírito do jogo base. Camelo e Lhama ficam deliberadamente fora de escopo —
este mod é sobre a família dos cavalos.

<p align="center">
  <img src="docs/images/showcase-overview.png" alt="Cavalo usando ferradura de ouro e alforje de prata." width="700">
</p>

## Visão geral

- **Ferraduras** — um slot de equipamento dedicado com 4 tiers de material e 3
  encantamentos customizados, que fazem dano de queda, oceanos e lava deixarem
  de ser obstáculos pra exploração a cavalo.
- **Alforjes Modulares** — um inventário preso à sela, dando capacidade de carga
  real pro Cavalo e ainda mais pra Burro/Mula, sem substituir o baú deles.
- **Genética Transparente** — veja os atributos reais de um cavalo com a Lente
  do Criador, crie de forma deliberada com itens de dieta, e desenvolva traços
  de linhagem passivos ao longo de gerações.
- **Genética Mendeliana de Pelagem** *(chegando na Fase D)* — pelagens raras se
  tornam um objetivo de criação pra colecionadores, não um sorteio de spawn.

## Status

| Fase | Funcionalidade | Status |
|---|---|---|
| A | Ferraduras | ✅ Implementada |
| B | Alforjes Modulares | ✅ Implementada |
| C | Genética Transparente (Lente do Criador, criação guiada por dieta, traços de linhagem) | 📐 Desenhada |
| D | Genética Mendeliana de Pelagem | ⬜ Não iniciada |

Plataforma: Minecraft Forge, Minecraft 26.3, Java 25.

## Fase A — Ferraduras

<p align="center">
  <!-- TODO: screenshot da tela de inventário do cavalo com o slot de ferradura preenchido -->
  <img src="docs/images/horseshoe-inventory.png" alt="Tela de inventário do cavalo mostrando o slot de ferradura" width="600">
</p>

Um slot de equipamento totalmente separado do slot de armadura/barding do cavalo.
Clique com o botão direito (mão vazia) num cavalo elegível para abrir seu
inventário e encaixar uma ferradura junto da sela e da armadura.

### Tiers de material

| Tier | Durabilidade | Efeito passivo (sempre ativo) | Encantamentos exclusivos elegíveis |
|---|---|---|---|
| Cobre | 100 | — | Passos de Pena, Passos Congelantes |
| Ferro | 200 | — | Passos de Pena, Passos Congelantes |
| Diamante | 300 | — | Passos de Pena, Passos Congelantes |
| Ouro | 80 | +velocidade de movimento; Piglins ficam neutros enquanto o cavalo está montado (mesma regra da armadura de ouro) | nenhum |
| Netherite | 400 | Imunidade total a dano de fogo/lava | Caminhante de Lava, Passos de Pena |

A durabilidade se desgasta com o uso; ao chegar a zero, a ferradura quebra (sem
reparo via bigorna com material bruto — só **Mending** evita isso).
**Unbreaking** reduz a chance de desgaste normalmente.

### Encantamentos customizados

- **Passos de Pena** — anula o dano de queda do cavalo (e do montador). Disponível
  em Cobre, Ferro, Diamante e Netherite — não em Ouro.
- **Passos Congelantes** — o cavalo congela a água sob os cascos enquanto se move,
  exatamente como o Frost Walker II vanilla; o gelo derrete após alguns segundos.
  Disponível só em Cobre, Ferro e Diamante.
- **Caminhante de Lava** — o cavalo caminha sobre lava sem afundar, como um
  Strider. Exclusivo de Netherite.

Obtido via mesa de encantamento, bigorna + livro encantado, ou loot, como
qualquer encantamento vanilla.

<details>
<summary><strong>Receitas de craft</strong></summary>

Ferraduras de Cobre, Ferro, Diamante e Ouro compartilham uma receita em formato
"U" (5 lingotes do material do tier):

```
I . I
I . I
. I .
```

Netherite **não tem receita própria**. É um upgrade na Mesa de Ferraria, igual às
ferramentas e armaduras de Netherite vanilla:

```
Ferradura de Diamante + Lingote de Netherite + Molde de Upgrade de Netherite
```

O upgrade preserva encantamentos existentes e a proporção de dano do item.
Ferraduras de Ferro não servem mais de base para esse upgrade, e uma ferradura
com Passos Congelantes não pode ser upada (a Netherite não aceita esse
encantamento).

</details>

## Fase B — Alforjes Modulares

<p align="center">
  <!-- TODO: screenshot mostrando as bolsas renderizadas nas laterais da sela -->
  <img src="docs/images/saddlebag-render.png" alt="Cavalo usando um alforje visível nas laterais" width="600">
</p>

Um segundo slot de equipamento, preso à sela, dando ao cavalo seu próprio
inventário pequeno — sem substituir o baú que Burro e Mula já carregam.

### Tiers de material

| Tier | Slots | Bônus especial |
|---|---|---|
| Couro | 9 | — |
| Ferro | 9 | O conteúdo sobrevive se o cavalo morrer por dano de explosão |
| Diamante | 9 | Mesma proteção contra explosão do Ferro; não protegido contra fogo/lava |
| Ouro | 9 | Piglins ficam neutros enquanto o cavalo está montado (mesmo mecanismo da Ferradura de Ouro) |
| Netherite | 9 | O conteúdo é imune a fogo/lava se o cavalo morrer em chamas |

A contagem de slots é 9 por padrão em todo tier — a única forma de ter mais
capacidade é o traço de linhagem Dorso Largo da Fase C (veja abaixo), que expõe
18 slots em qualquer tier de material.

Para Burro e Mula, os slots do alforje são **somados** ao baú nativo de 18 slots
— 27 no total (36 com Dorso Largo) quando ambos estão equipados, e os dois
inventários nunca se misturam.

Sem durabilidade — o alforje é permanente até o jogador removê-lo manualmente.

### Dependência de sela

Um alforje só pode ser encaixado se o cavalo já estiver com sela. Se a sela for
removida enquanto um alforje (com conteúdo) estiver equipado, o alforje é
automaticamente desprendido e cai no chão como um item normal — seu conteúdo viaja
intacto junto com ele. A remoção da sela nunca é bloqueada.

<details>
<summary><strong>Receitas de craft</strong></summary>

Mesmo padrão das ferraduras: Couro, Ferro, Diamante e Ouro têm receita própria
(Diamante: 2 diamantes + 5 couros); Netherite é um upgrade na Mesa de Ferraria a
partir de um Alforje de Diamante (+ Lingote de Netherite + Molde de Upgrade de
Netherite), preservando os itens guardados sem alteração. Alforjes de Ferro não
servem mais de base para esse upgrade.

</details>

## Fase C — Genética Transparente

<p align="center">
  <img src="docs/images/breeder-spyglass.png" alt="Tela da Lente do Criador mostrando notas de atributo e traços de um cavalo" width="600">
</p>

A criação de cavalos no vanilla escondia sua matemática do jogador. O Stride &
Sire torna isso visível e, com os itens certos, direcionável — sem transformar
em planilha.

### Lente do Criador

Shift + clique direito num cavalo elegível com uma Luneta equipada abre um
relatório somente leitura da genética dele: uma nota (E a S) de Velocidade,
Salto e Vida Máxima, além de todos os traços de linhagem que ele carrega —
incluindo os **latentes** (herdados, mas não expressos no momento, mostrados
separadamente dos manifestos). Não consome a Luneta nem gasta durabilidade;
confira quantas vezes quiser.

### Notas de atributo expandidas

O teto de Velocidade, Salto e Vida Máxima de todo cavalo é elevado 20% acima do
máximo vanilla, com nota de E a S:

| Nota | Faixa (% do máximo vanilla) | Como alcançar |
|---|---|---|
| E | 0–25% | Spawn selvagem ou qualquer cruzamento |
| D | 25–50% | Spawn selvagem ou qualquer cruzamento |
| C | 50–75% | Spawn selvagem ou qualquer cruzamento |
| B | 75–100% (o teto vanilla) | Spawn selvagem ou qualquer cruzamento |
| A | 100–110% (acima do vanilla) | **Só cruzamento guiado por dieta** |
| S | 110–120% (o teto expandido) | **Só cruzamento guiado por dieta** |

Um cavalo selvagem ou criado sem item de dieta nunca passa de B — A e S só se
alcançam cruzando com um dos itens de dieta abaixo.

### Criação guiada por dieta

O item usado para iniciar o acasalamento decide qual atributo tem chance de
herdar do pai mais forte em vez de uma média simples:

| Item | Atributo favorecido |
|---|---|
| Cenoura Dourada | Velocidade |
| Maçã Dourada | Vida Máxima |
| Melancia Reluzente | Salto *(passa a funcionar como item de criação de cavalo por este mod — no vanilla, não funciona)* |

Usar qualquer um dos três libera a faixa A/S para os **três** atributos nesse
cruzamento; o item só decide qual deles ganha adicionalmente 20% de chance de
herdar direto do pai melhor em vez da média dos pais.

### Traços de linhagem

O cruzamento pode passar um ou mais de três traços passivos. Cada um existe em
um de três estados por cavalo — ausente, **latente** (herdado, transmissível,
mas inativo) ou **manifesto** (ativo) — e um cavalo pode carregar mais de um ao
mesmo tempo:

| Traço | Efeito quando manifesto |
|---|---|
| Dorso Largo | Desbloqueia 18 slots de alforje em vez de 9 (em qualquer tier de material) |
| Casco Firme | Sobe automaticamente elevações de 1,5–2 blocos, como uma cabra |
| Sangue Frio | Não empina nem derruba o montador com dano repentino de zumbi/explosão de creeper (ainda toma o dano) |

Um filhote tem uma chance baixa (1–2%) de manifestar/carregar espontaneamente um
traço que nenhum dos pais tem, e uma chance bem maior (15–35%) se pelo menos um
dos pais já carrega — cruzar de forma seletiva mantém o traço na linhagem.

## Roadmap

### Fase D — Genética Mendeliana de Pelagem (não iniciada)

Pelagens raras (preto absoluto, branco albino) deixam de aparecer em spawns
naturais — tornam-se "mutações recessivas", só alcançáveis cruzando linhagens
específicas de cor/manchas por 3–4 gerações, criando um objetivo de endgame para
colecionadores. Reaproveita o vocabulário latente/manifesto (dominante/recessivo)
introduzido na Fase C.

## Instalação

<!-- TODO: preencher quando existir uma release/página no CurseForge -->
1. Instale o [Minecraft Forge](https://files.minecraftforge.net/) para o Minecraft 26.3.
2. Baixe o `stride-and-sire-*.jar` mais recente em [Releases](../../releases) (ou no CurseForge, quando publicado).
3. Coloque o jar na sua pasta `mods`.

## Galeria

<!-- TODO: adicionar mais screenshots/GIFs conforme as funcionalidades forem saindo -->
<p align="center">
  <img src="docs/images/gallery-1.png" alt="Imagem de galeria placeholder 1" width="280">
  <img src="docs/images/gallery-2.png" alt="Imagem de galeria placeholder 2" width="280">
  <img src="docs/images/gallery-3.png" alt="Imagem de galeria placeholder 3" width="280">
</p>

## Licença

[MIT](LICENSE.txt)
