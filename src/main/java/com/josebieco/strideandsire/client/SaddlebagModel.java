package com.josebieco.strideandsire.client;

import com.josebieco.strideandsire.StrideAndSire;
import com.josebieco.strideandsire.render.SaddlebagRendering;
import java.util.Set;
import net.minecraft.client.model.animal.equine.AbstractEquineModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.EquineRenderState;
import net.minecraft.resources.Identifier;

/**
 * Two leather side bags hanging on the flanks just behind the saddle.
 *
 * <p>Built like vanilla's {@code EquineSaddleModel}: the adult equine part tree ({@link AbstractEquineModel#createBodyMesh})
 * with every vanilla cube stripped, so {@link AbstractEquineModel#setupAnim} poses the {@code body} part exactly like the
 * horse's own body (rearing tilt included) and the bags, children of {@code body}, follow it. The per-type mesh scale
 * (horse 1.1, donkey 0.87, mule 0.92, undead horses 1.0) lives on the root part's pose; {@link SaddlebagLayer} copies
 * the parent model's root pose onto this model's root, so a single layer definition serves every equine renderer.
 *
 * <p>Geometry in body-local model pixels (body box x -5..5, y -8..2, z -17..5; saddle box x ±5.5, z -9.5..0.5;
 * donkey/mule chests x ±5..±8, y -8..0, z -4..4). Per side:
 * <ul>
 *   <li>bag: 2 (out) x 5 (tall) x 6 (long), x 5.1..7.1, y -6..-1, z 0..6 (front edge tucked half a pixel under the
 *       saddle's back edge, rear edge one pixel past the rump);</li>
 *   <li>flap: nominal 1 x 3 x 6, deformed to 0.5 thick and grown 0.25 in z so it overhangs the bag's front and back
 *       edges; it starts a quarter pixel above the bag's top: x 7.1..7.6, y -6.25..-3.25, z -0.25..6.25 (the lower
 *       2.25 px of the bag's outer face show below it).</li>
 * </ul>
 * Total outward extent 7.6 stays inside a chest's 8, and no bag face is coplanar with a body, saddle or chest face, so
 * on a chested donkey/mule the chest simply hides the front of each bag (the rear 2 px still show) without z-fighting.
 * The right bag uses {@code mirror()} so both outer faces sample the same texture region.
 *
 * <p>UV layout (texture {@value SaddlebagRendering#MODEL_TEXTURE_WIDTH}x{@value SaddlebagRendering#MODEL_TEXTURE_HEIGHT},
 * standard box unwrap with d = z-length, w = x-thickness, h = height): bag at {@code texOffs(0, 0)} uses u 0..16, v 0..11
 * (outer face u 8..14, v 6..11); flap at {@code texOffs(16, 0)} uses u 16..30, v 0..9 (outer face u 23..29, v 6..9).
 * {@code tools/pixelart/generate.py} paints that layout.
 */
public class SaddlebagModel extends AbstractEquineModel<EquineRenderState> {
    public static final ModelLayerLocation LAYER =
        new ModelLayerLocation(Identifier.fromNamespaceAndPath(StrideAndSire.MODID, "saddlebag"), "main");

    static final String LEFT_BAG = "left_saddlebag";
    static final String RIGHT_BAG = "right_saddlebag";

    public SaddlebagModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = createBodyMesh(CubeDeformation.NONE);
        PartDefinition root = mesh.getRoot();
        // Keep the part tree and poses (setupAnim needs body, head_parts, legs and tail) but draw none of the horse.
        root.retainExactParts(Set.of());
        PartDefinition body = root.getChild("body");
        CubeDeformation flap = new CubeDeformation(-0.25F, 0.0F, 0.25F);
        body.addOrReplaceChild(LEFT_BAG,
            CubeListBuilder.create()
                .texOffs(0, 0).addBox(5.1F, -6.0F, 0.0F, 2.0F, 5.0F, 6.0F)
                .texOffs(16, 0).addBox(6.85F, -6.25F, 0.0F, 1.0F, 3.0F, 6.0F, flap),
            PartPose.ZERO);
        body.addOrReplaceChild(RIGHT_BAG,
            CubeListBuilder.create().mirror()
                .texOffs(0, 0).addBox(-7.1F, -6.0F, 0.0F, 2.0F, 5.0F, 6.0F)
                .texOffs(16, 0).addBox(-7.85F, -6.25F, 0.0F, 1.0F, 3.0F, 6.0F, flap),
            PartPose.ZERO);
        return LayerDefinition.create(mesh, SaddlebagRendering.MODEL_TEXTURE_WIDTH, SaddlebagRendering.MODEL_TEXTURE_HEIGHT);
    }
}
