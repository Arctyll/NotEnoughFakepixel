package org.ginafro.notenoughfakepixel.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.RenderPlayer;
import org.ginafro.notenoughfakepixel.NotEnoughFakepixel;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.HashMap;
import java.util.Map;

@Mixin(RenderPlayer.class)
public class MixinRenderPlayer {
    private static final Map<String, float[]> userSizes = new HashMap<>();
    private float scaleFactor = 1.0f;

    static {
        userSizes.put("GoatMG", new float[]{1.5f, 0.7f, 1.5f});
    }

    @Inject(method = "preRenderCallback", at = @At("HEAD"))
    private void onPreRenderCallback(AbstractClientPlayer entity, float partialTickTime, CallbackInfo ci) {
        float x = 1.0f, y = 1.0f, z = 1.0f;

        if (NotEnoughFakepixel.feature.qol.sizetoggle) {
            x = NotEnoughFakepixel.feature.qol.x;
            y = NotEnoughFakepixel.feature.qol.y;
            z = NotEnoughFakepixel.feature.qol.z;
        }

        if (userSizes.containsKey(entity.getName())) {
            float[] sizes = userSizes.get(entity.getName());
            x = sizes[0];
            y = sizes[1];
            z = sizes[2];
        }

        GL11.glScalef(x, y, z);

        if (NotEnoughFakepixel.feature.qol.spintoggle) {
            long currentTime = Minecraft.getSystemTime();
            float time = currentTime / 10f;
            float angle = (time * NotEnoughFakepixel.feature.qol.speed) % 360;
            GL11.glRotatef(180 - angle, NotEnoughFakepixel.feature.qol.value1, NotEnoughFakepixel.feature.qol.value2, NotEnoughFakepixel.feature.qol.value3);
        }
    }
}

