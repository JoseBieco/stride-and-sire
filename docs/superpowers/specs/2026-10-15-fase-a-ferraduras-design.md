# Fase A — Sistema de Ferraduras (Horseshoes)

Status: aprovado para plano de implementação
Mod: Vanilla Plus Horses (Forge, Minecraft 26.3, Java 25)
Esta é a Fase 1 de 4 do roadmap do mod. Fases seguintes (fora de escopo aqui):
B — Alforjes Modulares, C — Genética Transparente (Lente do Criador, Influência da Dieta, Traços de Linhagem), D — Genética de Pelagem Mendeliana.

## 1. Objetivo

Adicionar um slot de equipamento extra e independente da armadura/barding nos equídeos,
para ferraduras que oferecem utilidades situacionais de exploração (dano de queda,
velocidade/diplomacia com Piglins, andar sobre água congelada, andar sobre lava),
sem alterar o slot de armadura existente.

## 2. Escopo

- Mobs afetados: todos os subtipos de `AbstractHorse` — Cavalo, Burro, Mula,
  Cavalo-Esqueleto, Cavalo-Zumbi. Camelo e Lhama ficam **fora de escopo** (não são
  equídeos e têm sistemas de equipamento próprios).
- O slot de ferradura é adicional ao slot de sela e ao slot de armadura/barding
  já existentes — não os substitui nem interage com eles.
- Alforjes, genética e pelagem são fases futuras; esta spec não cobre integração
  com eles (mas a arquitetura de attachment deve ser extensível para isso depois).

## 3. Itens e Progressão de Materiais

| Tier | Durabilidade base | Efeito passivo (sempre ativo) | Encantamentos exclusivos elegíveis | Observações |
|---|---|---|---|---|
| Cobre | 100 | — | Passos de Pena, Passos Congelantes | Tier de entrada, acesso cedo |
| Ferro | 200 | — | Passos de Pena, Passos Congelantes | Workhorse; item-base para upgrade de Netherite |
| Ouro | 80 | +velocidade base do cavalo; Piglins neutros ao jogador montado (igual armadura de ouro) | Nenhum dos 3 exclusivos | Build "diplomata/veloz"; não combina com encantamentos de terreno |
| Netherite | 400 | Imunidade total a dano de fogo/lava (sempre ativa) | Caminhante de Lava (exclusivo deste tier) + Passos de Pena | Não elegível para Passos Congelantes (incoerente termicamente) |

Regra de design: Ouro é deliberadamente excluído dos 3 encantamentos de terreno para
evitar que uma única ferradura acumule todos os benefícios ("melhor em tudo").

## 4. Encantamentos Customizados

Três encantamentos novos, exclusivos de item da classe/tag "Horseshoe":

1. **Passos de Pena** — remove/reduz dano de queda do cavalo (e do jogador montado).
   Elegível em: Cobre, Ferro, Netherite. Não elegível em: Ouro.
2. **Passos Congelantes** — replica o efeito do encantamento vanilla Frost Walker
   (nível equivalente a Frost Walker II), congelando água ao redor dos cascos
   enquanto o cavalo se move; o gelo derrete após alguns segundos como no vanilla.
   Elegível em: Cobre, Ferro. Não elegível em: Ouro, Netherite.
3. **Caminhante de Lava** — permite que o cavalo caminhe sobre lava sem afundar
   (comportamento equivalente ao Strider). Elegível exclusivamente em: Netherite.

Obtenção: mesa de encantamento, bigorna (combinação com livro encantado), e
elegível a aparecer em loot/trocas de aldeão como qualquer encantamento (a definir
na fase de implementação/balanceamento). Encantamentos genéricos relevantes também
se aplicam normalmente: **Unbreaking** (reduz chance de perda de durabilidade) e
**Mending** (repara com XP). Não há reparo via bigorna com lingotes — ao esgotar a
durabilidade, a ferradura quebra (é destruída), só Mending evita isso.

## 5. Receitas de Craft

Cobre / Ferro / Ouro — receita em mesa de trabalho, formato em "U" (5 lingotes do material):

```
I . I
I . I
. I .
```

Netherite — **sem receita própria**. Só via Mesa de Ferraria (Smithing Table),
upgrade padrão igual a armaduras/ferramentas vanilla:
Ferradura de Ferro + Lingote de Netherite + Molde de Upgrade de Netherite.

## 6. Arquitetura de Dados e Slot

O Forge não possui um slot nativo para isso. Abordagem:

- **Data attachment** no próprio `AbstractHorse` guardando o `ItemStack` da
  ferradura equipada e sua durabilidade restante (API de attachments do
  Forge/NeoForge moderno).
- **Tela de inventário customizada**, estendendo a tela vanilla do cavalo,
  adicionando um slot extra que só aceita itens da tag/classe "Horseshoe" —
  os slots de sela e armadura continuam inalterados.
- **Sincronização servidor-cliente** do attachment (necessário para
  renderização visual e HUD de durabilidade).
- Accessor exposto pelo cavalo (ex. `getEquippedHorseshoe()`) consultado pelos
  handlers de evento que implementam os efeitos (ver seção 7).
- Esta camada de attachment deve ser projetada de forma extensível, já que a
  Fase B (Alforjes) provavelmente vai precisar de um padrão semelhante.

## 7. Integração com Eventos / Lógica de Efeitos

| Efeito | Gatilho |
|---|---|
| Passos de Pena / redução de queda | Evento de dano de queda — cancela/reduz se a ferradura equipada tiver o encantamento |
| Velocidade (Ouro) | `AttributeModifier` no atributo de movimento, aplicado ao equipar e removido ao desequipar |
| Piglins neutros (Ouro) | Estende a checagem existente de "jogador vestindo ouro" para também considerar "cavalo montado tem ferradura de ouro" |
| Passos Congelantes | Reaproveita a lógica do Frost Walker vanilla, disparada a cada tick de movimento do cavalo em vez das botas do jogador |
| Caminhante de Lava + imunidade a fogo (Netherite) | Cancela dano de fogo/lava em evento de dano; sobrescreve comportamento de flutuação em lava (igual ao Strider) |
| Quebra ao esgotar durabilidade | Ao decrementar durabilidade a zero (sem Mending ativo), remove o item do attachment/slot e destrói a ferradura |

## 8. Renderização

Camada visual extra no renderer do cavalo, desenhando a ferradura nos 4 cascos
(textura variando por tier/material), visível somente quando o attachment tem uma
ferradura equipada.

## 9. Plano de Testes

GameTests do Forge cobrindo:
- Equipar/desequipar no slot novo (e que sela/armadura não são afetadas).
- Quebra da ferradura ao zerar durabilidade (com e sem Mending).
- Cada efeito passivo (Ouro: velocidade, Piglin) disparando corretamente.
- Cada encantamento exclusivo disparando corretamente, e bloqueado nos tiers
  não elegíveis (ex.: tentar aplicar Caminhante de Lava em Ferro deve falhar).
- Upgrade para Netherite via Mesa de Ferraria preservando encantamentos existentes
  (igual ao comportamento vanilla de armaduras/ferramentas).
- Comportamento de neutralidade com Piglins com e sem ferradura de ouro.

## 10. Fora de Escopo / Itens Abertos

- Alforjes, genética transparente, influência de dieta, traços de linhagem e
  pelagem mendeliana — fases futuras (B, C, D).
- Valores numéricos exatos de durabilidade e bônus de velocidade (Ouro) são
  estimativas de design; podem ser ajustados durante balanceamento/playtesting
  sem alterar a arquitetura.
- Disponibilidade dos 3 encantamentos customizados em loot tables e trocas de
  aldeão fica para decisão na implementação/balanceamento.
