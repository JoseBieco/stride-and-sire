# Fase B — Alforjes Modulares (Saddlebags)

Status: aprovado para plano de implementação
Mod: Stride & Sire (Forge, Minecraft 26.3, Java 25)
Esta é a Fase 2 de 4 do roadmap do mod. Depende da arquitetura de data attachment,
menu customizado e camada de renderização já implementada na Fase A (Ferraduras).
Fases seguintes (fora de escopo aqui): C — Genética Transparente (Lente do Criador,
Influência da Dieta, Traços de Linhagem — inclui o traço "Dorso Largo", que no
futuro desbloqueará o Alforje Duplo de 18 slots), D — Genética de Pelagem Mendeliana.

## 1. Objetivo

Adicionar um slot de equipamento extra, dependente da sela, para alforjes com
inventário próprio em todos os equídeos — dando alívio de carga para quem explora
montado em Cavalo, Cavalo-Esqueleto ou Cavalo-Zumbi, e um complemento (não
substituto) ao baú já existente de Burro e Mula.

## 2. Escopo

- Mobs afetados: todos os subtipos de `AbstractHorse` (Cavalo, Burro, Mula,
  Cavalo-Esqueleto, Cavalo-Zumbi). Camelo e Lhama ficam fora de escopo, como na
  Fase A.
- Para Burro/Mula, o alforje **soma** ao baú nativo (18 slots): total de 27 slots
  quando ambos estão equipados. O alforje não substitui o baú.
- O "Alforje Duplo" (18 slots), mencionado na ideia original do jogador, é
  **explicitamente fora de escopo** nesta fase — ele será desbloqueado apenas pelo
  traço genético "Dorso Largo" na Fase C. Nesta fase, todo alforje tem exatamente
  9 slots, independente de material.
- Sem durabilidade: o alforje é permanente até o jogador removê-lo manualmente
  (ou até a remoção automática por dependência de sela, ver seção 6).

## 3. Itens e Progressão de Materiais

| Tier | Slots | Bônus especial | Observação |
|---|---|---|---|
| Couro | 9 | — | Tier base, sem bônus |
| Ferro | 9 | Itens guardados protegidos contra destruição por dano de explosão (TNT, Creeper) se o cavalo morrer | |
| Ouro | 9 | Reaproveita o efeito de neutralidade com Piglins das ferraduras de ouro (reforça a identidade "diplomacia" do material no mod) | |
| Netherite | 9 | Itens guardados são imunes a fogo/lava se o cavalo morrer em chamas | Só obtido via upgrade (ver seção 5) |

Nenhum tier altera a contagem de slots — essa é fixa em 9, deliberadamente, para
não haver dois caminhos (material vs. traço genético) levando ao mesmo resultado
de capacidade.

## 4. Pré-requisito de Sela

- O slot de alforje só aceita um item se o cavalo já tiver uma sela equipada.
- Se a sela for removida enquanto um alforje estiver equipado, o alforje é
  automaticamente desequipado (preservando seu conteúdo, ver seção 5) e derrubado
  como item no mundo, na posição do cavalo — nunca derrama os itens soltos, nunca
  bloqueia a remoção da sela.

## 5. Arquitetura de Dados

Diferente da ferradura (cujo estado vive só no attachment do cavalo), o alforje
usa um padrão mais próximo do shulker box vanilla:

- O **inventário de 9 slots do alforje vive no próprio `ItemStack`**, como um
  componente de dados tipo "container" (o mesmo mecanismo vanilla usado por
  shulker boxes). Isso garante que, ao cair no chão (seção 4) ou ser movido entre
  inventários, o conteúdo viaja junto automaticamente, sem lógica extra de
  sincronização de itens.
- O **cavalo** guarda, via data attachment (reaproveitando `ModAttachments` /
  o padrão de `HorseshoeAttachment` da Fase A), apenas uma referência ao slot do
  alforje equipado — não uma cópia dos itens.
- Isso significa que craft, upgrade de netherite (seção 6) e os bônus especiais
  (seção 3) todos operam sobre o container-component do item, não sobre um estado
  paralelo no cavalo.

## 6. Receitas de Craft

Segue exatamente o padrão da Fase A: Couro, Ferro e Ouro têm receita própria em
mesa de trabalho (couro + lingotes do material correspondente). Netherite **não
tem receita própria** — só é obtido via Mesa de Ferraria, upgradando um Alforje de
Ferro (Alforje de Ferro + Lingote de Netherite + Molde de Upgrade de Netherite),
preservando o conteúdo do container-component durante o upgrade (igual ao
comportamento vanilla ao upgradar um shulker box não é possível, mas aqui o alforje
precisa preservar itens guardados — tratar como requisito explícito de teste).

## 7. Integração com o Menu do Cavalo

- A tela de inventário do cavalo (já estendida na Fase A com o slot de ferradura)
  ganha mais um slot: o slot de alforje, condicionado a já haver sela (seção 4).
- Quando o alforje está equipado, a tela exibe as 9 fileiras extras de slots do
  seu container-component abaixo dos slots de equipamento.
- Para Burro/Mula, essas 9 fileiras aparecem somadas às 18 do baú nativo (27 no
  total) — ambos os inventários continuam sendo containers independentes
  (o baú no cavalo, o alforje no item), só exibidos na mesma tela.

## 8. Integração com Eventos / Bônus Especiais

| Bônus | Gatilho |
|---|---|
| Proteção contra explosão (Ferro) | No evento de morte do cavalo por dano de explosão, se o alforje equipado é tier Ferro (ou superior, a definir no plano), o container-component do alforje não perde seus itens ao ser dropado |
| Neutralidade com Piglins (Ouro) | Estende a mesma checagem já usada pela ferradura de ouro (Fase A) para também considerar "cavalo montado tem alforje de ouro" |
| Imunidade a fogo/lava para o conteúdo (Netherite) | No evento de morte do cavalo por dano de fogo/lava, o container-component do alforje Netherite não é destruído/queimado junto com o restante do drop |

## 9. Renderização

Nova camada no renderer do cavalo (seguindo o padrão de `HorseshoeLayer` da Fase
A), desenhando pequenas bolsas nas laterais da sela, com textura variando por
material, visível somente quando o attachment aponta para um alforje equipado.

## 10. Plano de Testes

GameTests cobrindo:
- Slot de alforje rejeita item sem sela equipada; aceita com sela equipada.
- Remover a sela com alforje equipado (com itens dentro) dropa o alforje como
  item no mundo com o conteúdo intacto, sem bloquear a remoção da sela.
- Burro/Mula com baú + alforje expõem 27 slots simultâneos, cada inventário
  preservando sua própria identidade (mover item do baú não afeta o alforje).
- Cada bônus especial (Ferro/Ouro/Netherite) disparando corretamente, e ausente
  no tier Couro.
- Upgrade de Ferro para Netherite via Mesa de Ferraria preserva o conteúdo do
  container-component (itens guardados não são perdidos nem duplicados).
- Cavalo morre por explosão/fogo sem alforje equipado — comportamento vanilla
  inalterado (sem regressão nos drops normais do cavalo).

## 11. Fora de Escopo / Itens Abertos

- Alforje Duplo (18 slots) — Fase C, via traço "Dorso Largo".
- Genética, pelagem — Fases C e D.
- Definição exata de "quais explosões/fontes de fogo contam" para os bônus de
  Ferro/Netherite fica para decisão na implementação (deve reaproveitar as mesmas
  categorias de dano já usadas pelos handlers de evento da Fase A).
