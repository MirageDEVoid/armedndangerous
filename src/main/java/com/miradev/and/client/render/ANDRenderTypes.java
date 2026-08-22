package com.miradev.and.client.render;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.Util;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Function;

public class ANDRenderTypes extends RenderStateShard {

    public static final Function<ResourceLocation, RenderType> ITEM_GUI_NO_DEPTH =
            Util.memoize(ANDRenderTypes::buildItemGuiNoDepth);

    private ANDRenderTypes() {
        super("and_dummy", () -> {}, () -> {});
    }

    private static RenderType buildItemGuiNoDepth(ResourceLocation texture) {
        return RenderType.create(
                "and_item_gui_no_depth/" + texture,
                DefaultVertexFormat.NEW_ENTITY,
                VertexFormat.Mode.QUADS,
                256, true, false,
                RenderType.CompositeState.builder()
                        .setShaderState(RENDERTYPE_ENTITY_CUTOUT_NO_CULL_SHADER)
                        .setTextureState(new TextureStateShard(texture, false, false))
                        .setTransparencyState(NO_TRANSPARENCY)
                        .setLightmapState(LIGHTMAP)
                        .setOverlayState(OVERLAY)
                        .setDepthTestState(NO_DEPTH_TEST)
                        .createCompositeState(true)
        );
    }
}