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
import net.minecraft.client.renderer.entity.state.DonkeyRenderState;
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
 * On a chested Donkey/Mule ({@link DonkeyRenderState#hasChest}, rule {@link SaddlebagRendering#placementFor}) that
 * volume would sit inside the chest box, so {@link #setupAnim} swaps to an identical pair dropped by 6.3 px: bag
 * y 0.3..5.3, flap y 0.05..3.05, entirely below the chest's bottom face (y 0) and never intersecting it. The bags then
 * hang under the chests against the belly/upper hind legs (hind legs sit at x within ±5, y from 2, z 1..5), with the
 * whole outer face, flap and buckle visible. No bag face is coplanar with a body, saddle or chest face.
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
    static final String LEFT_BAG_UNDER_CHEST = "left_saddlebag_under_chest";
    static final String RIGHT_BAG_UNDER_CHEST = "right_saddlebag_under_chest";

    /** Vertical drop of the {@link SaddlebagRendering.Placement#UNDER_CHEST} variant: bag top from y -6 to y 0.3. */
    private static final float UNDER_CHEST_DROP = 6.3F;

    private final ModelPart[] flankBags;
    private final ModelPart[] underChestBags;

    public SaddlebagModel(ModelPart root) {
        super(root);
        this.flankBags = new ModelPart[] {this.body.getChild(LEFT_BAG), this.body.getChild(RIGHT_BAG)};
        this.underChestBags = new ModelPart[] {this.body.getChild(LEFT_BAG_UNDER_CHEST), this.body.getChild(RIGHT_BAG_UNDER_CHEST)};
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = createBodyMesh(CubeDeformation.NONE);
        PartDefinition root = mesh.getRoot();
        // Keep the part tree and poses (setupAnim needs body, head_parts, legs and tail) but draw none of the horse.
        root.retainExactParts(Set.of());
        PartDefinition body = root.getChild("body");
        body.addOrReplaceChild(LEFT_BAG, bag(false), PartPose.ZERO);
        body.addOrReplaceChild(RIGHT_BAG, bag(true), PartPose.ZERO);
        // Same boxes (same UVs), dropped below the chest's bottom face (y 0) instead of inside it.
        body.addOrReplaceChild(LEFT_BAG_UNDER_CHEST, bag(false), PartPose.offset(0.0F, UNDER_CHEST_DROP, 0.0F));
        body.addOrReplaceChild(RIGHT_BAG_UNDER_CHEST, bag(true), PartPose.offset(0.0F, UNDER_CHEST_DROP, 0.0F));
        return LayerDefinition.create(mesh, SaddlebagRendering.MODEL_TEXTURE_WIDTH, SaddlebagRendering.MODEL_TEXTURE_HEIGHT);
    }

    /** One bag + flap in flank position; {@code right} mirrors it to the -x side. */
    private static CubeListBuilder bag(boolean right) {
        CubeDeformation flap = new CubeDeformation(-0.25F, 0.0F, 0.25F);
        return CubeListBuilder.create().mirror(right)
            .texOffs(0, 0).addBox(right ? -7.1F : 5.1F, -6.0F, 0.0F, 2.0F, 5.0F, 6.0F)
            .texOffs(16, 0).addBox(right ? -7.85F : 6.85F, -6.25F, 0.0F, 1.0F, 3.0F, 6.0F, flap);
    }

    @Override
    public void setupAnim(EquineRenderState state) {
        super.setupAnim(state);
        boolean hasChest = state instanceof DonkeyRenderState donkey && donkey.hasChest;
        boolean underChest = SaddlebagRendering.placementFor(hasChest) == SaddlebagRendering.Placement.UNDER_CHEST;
        for (ModelPart part : this.flankBags) {
            part.visible = !underChest;
        }
        for (ModelPart part : this.underChestBags) {
            part.visible = underChest;
        }
    }
}
