package com.vibe.vuties.client;

import com.vibe.vuties.item.ShockBootsItem;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.renderer.GeoArmorRenderer;
import software.bernie.geckolib.util.RenderUtil;

public class ShockBootsGeoRenderer extends GeoArmorRenderer<ShockBootsItem> {

    public ShockBootsGeoRenderer() {
        super(new ShockBootsGeoModel());
    }

    @Override
    public RenderType getRenderType(ShockBootsItem animatable, ResourceLocation texture, @Nullable MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityCutoutNoCull(texture);
    }

    @Override
    protected void applyBoneVisibilityBySlot(EquipmentSlot currentSlot) {
        super.applyBoneVisibilityBySlot(currentSlot);

        if (currentSlot == EquipmentSlot.FEET) {
            if (this.rightLeg != null) setBoneVisible(this.rightLeg, true);
            if (this.leftLeg != null) setBoneVisible(this.leftLeg, true);
            if (this.rightBoot != null) setBoneVisible(this.rightBoot, true);
            if (this.leftBoot != null) setBoneVisible(this.leftBoot, true);
        }
    }

    @Override
    protected void applyBaseTransformations(HumanoidModel<?> baseModel) {
        super.applyBaseTransformations(baseModel);

        ModelPart rightPart = baseModel.rightLeg;
        ModelPart leftPart = baseModel.leftLeg;

        // Надежная синхронизация костей ботинок с моделью ног игрока
        if (this.rightLeg != null) {
            RenderUtil.matchModelPartRot(rightPart, this.rightLeg);
            this.rightLeg.updatePosition(rightPart.x + 2, 12 - rightPart.y, rightPart.z);
        }
        if (this.rightBoot != null) {
            RenderUtil.matchModelPartRot(rightPart, this.rightBoot);
            this.rightBoot.updatePosition(rightPart.x + 2, 12 - rightPart.y, rightPart.z);
        }

        if (this.leftLeg != null) {
            RenderUtil.matchModelPartRot(leftPart, this.leftLeg);
            this.leftLeg.updatePosition(leftPart.x - 2, 12 - leftPart.y, leftPart.z);
        }
        if (this.leftBoot != null) {
            RenderUtil.matchModelPartRot(leftPart, this.leftBoot);
            this.leftBoot.updatePosition(leftPart.x - 2, 12 - leftPart.y, leftPart.z);
        }
    }
}
