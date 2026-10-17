# Polimento Visual e Tier de Diamante

Status: aguardando revisão do usuário
Mod: Stride & Sire (Forge 66, Minecraft 26.3, Java 25)
Complementa as Fases A (Ferraduras) e B (Alforjes). Não é uma fase nova do roadmap;
é polimento + um tier novo de material para ambos os equipamentos.

## 1. Objetivo

1. Substituir a mancha plana atual do alforje no flanco do cavalo por um modelo 3D.
2. Mostrar ícones-silhueta nos slots vazios de ferradura e de alforje no menu do cavalo.
3. Dar aos alforjes ícones de item detalhados, com cara de bolsa de couro com fivela.
4. Adicionar o tier Diamante para ferraduras e alforjes, alinhando a progressão de
   Netherite ao vanilla (Netherite sai do upgrade do Diamante).

## 2. Escopo

- Mobs: os mesmos de sempre (Cavalo, Burro, Mula, Cavalo-Esqueleto, Cavalo-Zumbi).
- Fora de escopo: redesenhar os ícones das ferraduras já existentes; animações
  próprias das bolsas (além de seguirem o corpo); qualquer mudança em genética.

## 3. Geração da Arte

Toda textura nova é desenhada por um script Python versionado em
`tools/pixelart/` (Pillow), que escreve os PNGs diretamente nos caminhos de assets
do mod. Os PNGs gerados também são versionados (o build não roda Python). Rodar o
script de novo regenera tudo de forma determinística (mesma entrada → mesmos
bytes). Paleta no estilo vanilla: couro marrom com 3–4 tons, metais com 3 tons
(sombra, base, brilho) e contorno escuro.

Arte coberta pelo script:
- Ícones de item dos 5 alforjes (Couro, Ferro, Ouro, Diamante, Netherite).
- Ícone de item da Ferradura de Diamante.
- Textura dos cascos para a Ferradura de Diamante (mesmo layout de UV das outras).
- Texturas das bolsas 3D, uma por tier de alforje.
- Os 2 sprites de slot vazio (ferradura e alforje).

## 4. Alforje 3D (bolsas laterais)

- Uma camada de modelo própria (`ModelLayerLocation` do mod, registrada no evento
  de definições de camada do cliente) contendo duas bolsas: uma em cada flanco,
  atrás da sela, cada uma uma caixa de aproximadamente 6×5×3 pixels de modelo, mais
  uma aba fina sobre a face externa.
- As bolsas copiam a pose da parte "body" do modelo do cavalo a cada quadro, então
  acompanham o movimento e a inclinação do corpo.
- Textura por tier de alforje (seção 3): couro marrom com fivela/costura no metal
  do tier.
- Regras de visibilidade inalteradas: só desenha em adulto, selado, com alforje
  equipado e visível. O seam comum `SaddlebagRendering.materialToRender` continua
  sendo a fonte de verdade; só a forma de desenhar muda.
- Burro/Mula com baú nativo: as bolsas ficam atrás dos baús, sem sobrepor.
  Implementado: as bolsas pendem *sob* os baús (colocá-las "atrás" não é
  geometricamente possível: baú z −4..4, corpo termina em z 5).
- A mancha plana atual (overlay de textura da Fase B) é removida.

## 5. Ícones de Slot Vazio

- Os slots de ferradura e de alforje exibem um ícone-silhueta quando estão vazios,
  como o vanilla faz nos slots de sela e armadura.
- São dois sprites de GUI novos (`container/slot/horseshoe` e
  `container/slot/saddlebag` no namespace do mod), ligados pelo mecanismo vanilla de
  ícone de slot vazio em `HorseshoeSlot` e `SaddlebagSlot`.
- O ícone some quando o slot está ocupado. Sobre o slot de alforje: a silhueta
  aparece sempre que o slot está vazio, inclusive quando o cavalo não tem sela.
  O slot continua recusando o alforje até haver sela.

## 6. Ícones dos Alforjes

- 16×16, todos com a mesma base: bolsa de couro marrom com aba, fivela, costura e
  cantoneiras.
- O tier aparece no metal da fivela e das cantoneiras: Couro com costura simples e
  fivela de couro escuro; Ferro cinza; Ouro amarelo; Diamante ciano; Netherite
  cinza-escuro com reflexo arroxeado.

## 7. Tier Diamante

### 7.1 Ferradura de Diamante

| Campo | Valor |
|---|---|
| Durabilidade base | 300 |
| Encantamentos exclusivos elegíveis | Passos de Pena, Passos Congelantes (como Cobre/Ferro) |
| Passivo | nenhum |
| Encantabilidade | 10 |
| Receita | formato "U" de 5 diamantes (`I I` / `I I` / ` I `) |

- Entra nas tags de encantamento `enchantable/feather_step`,
  `enchantable/frost_step` e `minecraft:enchantable/durability` (Unbreaking/Mending).
- Tem textura de item, textura de casco e nomes en_us/pt_br
  ("Diamond Horseshoe" / "Ferradura de Diamante"). Vai para a creative tab, entre a
  de Ferro e a de Ouro.
- A camada de casco, o desgaste e as regras de menu tratam a Diamante como qualquer
  outro tier, sem casos especiais.

### 7.2 Alforje de Diamante

| Campo | Valor |
|---|---|
| Slots | 9 (18 com Dorso Largo), como todo alforje |
| Proteção contra explosão | sim |
| Proteção contra fogo/lava | não (exclusiva da Netherite) |
| Neutralidade com Piglins | não (exclusiva do Ouro) |
| Receita | `D D` / `L L` / `LLL` (2 diamantes + 5 couros) |

- Tem tooltip de bônus ("Contents survive explosions" / pt_br equivalente), ícone,
  textura 3D, nomes en_us/pt_br ("Diamond Saddlebag" / "Alforje de Diamante") e
  entrada na creative tab, entre Ferro e Ouro.

### 7.3 Mudança de progressão (Netherite)

- Ferradura de Netherite: o upgrade na Mesa de Ferraria passa a ser
  **Ferradura de Diamante** + Lingote de Netherite + Molde de Upgrade de Netherite.
  O upgrade a partir da Ferro deixa de existir.
  Continua conservando encantamentos e a proporção de dano (ex.: 150/300 → 200/400)
  e continua recusando bases com encantamentos que a Netherite não aceita
  (Passos Congelantes).
- Alforje de Netherite: o upgrade passa a ser **Alforje de Diamante** + Lingote
  de Netherite + Molde. O upgrade a partir do de Ferro deixa de existir. O
  conteúdo continua preservado exatamente (itens, quantidades, slots).
- Ferradura e Alforje de Ferro continuam com receita normal; só deixam de ser base
  do upgrade.

## 8. Plano de Testes

GameTests (o resto da suíte continua verde):
- Ferradura de Diamante registrada com o material, a durabilidade e a
  encantabilidade certos; matriz de encantamentos (Passos de Pena e Congelantes
  sim, Caminhante de Lava não) concordando com as tags.
- Receita em U de diamantes; o upgrade Diamante → Netherite conserva encantamentos
  e proporção de dano; Ferro não serve mais de base; Diamante com Passos
  Congelantes é recusada.
- Alforje de Diamante registrado com o material certo; receita; protege conteúdo
  em morte por explosão e perde o conteúdo em morte por fogo; upgrade Diamante →
  Netherite preserva o conteúdo; Ferro não serve mais de base.
- `HorseshoeSlot` e `SaddlebagSlot` informam o sprite certo de slot vazio.
- `SaddlebagRendering.materialToRender` cobre o tier Diamante; a seleção de textura
  3D cobre os 5 tiers.
- O script de arte roda e gera todos os PNGs esperados com o tamanho certo
  (checagem do script; não é um GameTest).

Checagem manual no cliente (fora dos testes automatizados): aparência das bolsas
3D em Cavalo e em Burro com baú, ícones dos slots vazios, ícones dos alforjes e
textura da Ferradura de Diamante nos cascos.

## 9. Riscos / Itens Abertos

- A API de camadas de modelo do 26.3 (registro de `LayerDefinition`, cópia de pose
  do body) precisa ser confirmada nos sources do Forge 66.
- Mundos existentes: itens de upgrade antigos (Ferro → Netherite) simplesmente
  deixam de ter receita; nenhum item é perdido nem convertido.
