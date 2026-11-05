package dev.deltavisuals.feature;

import dev.deltavisuals.core.Category;
import dev.deltavisuals.core.Module;
import dev.deltavisuals.core.Setting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;

/**
 * Night Vision: the world is fully lit, like with the potion, but only on your screen.
 *
 * It adds an endless Night Vision effect to the local player entity (client side only, the server is never told
 * and nothing is sent). An endless effect has no flicker near the end and no timer. The effect is re-applied if
 * the server removes it, and it is taken away again when the module is switched off or the world is left.
 * A real Night Vision effect from the server that still has plenty of time left is left untouched.
 */
public final class NightVision extends Module {
    /** Vanilla starts to flicker at 200 ticks left; a real effect with more than this is already steady. */
    private static final int STEADY_TICKS = 220;

    private final Setting.Bool hideIcon;
    /** True while the effect on the player is the one this module added. */
    private boolean applied;

    public NightVision() {
        super("Night Vision", "See in the dark: the world is fully lit (only on your screen, nothing is sent to the server)",
            Category.VISUALS, false);
        hideIcon = add(new Setting.Bool("Hide Effect Icon", true));
    }

    @Override
    public void onTick(MinecraftClient mc) {
        ClientPlayerEntity p = mc.player;
        if (p == null) { applied = false; return; }

        StatusEffectInstance cur = p.getStatusEffect(StatusEffects.NIGHT_VISION);
        boolean ours = applied && cur != null && cur.isInfinite();
        if (ours) {
            // keep the icon setting live
            if (cur.shouldShowIcon() == hideIcon.get()) {
                p.removeStatusEffect(StatusEffects.NIGHT_VISION);
                applied = false;
                apply(p);
            }
            return;
        }
        applied = false;
        if (cur != null && (cur.isInfinite() || cur.getDuration() > STEADY_TICKS)) return; // a real, steady effect
        if (cur != null) p.removeStatusEffect(StatusEffects.NIGHT_VISION);                // a real one about to flicker out
        apply(p);
    }

    private void apply(ClientPlayerEntity p) {
        p.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, StatusEffectInstance.INFINITE,
            0, true, false, !hideIcon.get()));
        applied = true;
    }

    @Override
    public void onDisable() {
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientPlayerEntity p = mc == null ? null : mc.player;
        if (p != null && applied) {
            StatusEffectInstance cur = p.getStatusEffect(StatusEffects.NIGHT_VISION);
            if (cur != null && cur.isInfinite()) p.removeStatusEffect(StatusEffects.NIGHT_VISION);
        }
        applied = false;
    }
}
