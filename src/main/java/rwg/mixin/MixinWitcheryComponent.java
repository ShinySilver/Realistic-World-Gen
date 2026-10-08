package rwg.mixin;

import net.minecraft.world.World;
import net.minecraft.world.gen.structure.StructureBoundingBox;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.emoniph.witchery.worldgen.WitcheryComponent;
import rwg.support.WitcheryWorldgenCompat;

/** Rejects Witchery structures whose sampled footprint is unsafe in an RWG world. */
@Mixin(value = WitcheryComponent.class, remap = false)
public abstract class MixinWitcheryComponent {

    @Inject(method = "calcGroundHeight", at = @At("HEAD"), cancellable = true, remap = false)
    private void rwg$rejectUnsafeTerrain(World world, StructureBoundingBox bounds,
            CallbackInfoReturnable<Integer> callback) {
        if (WitcheryWorldgenCompat.hasUnsuitableFoundation(world, bounds)) callback.setReturnValue(-1);
    }
}
