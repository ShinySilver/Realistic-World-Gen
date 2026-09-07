package rwg.mixin;

import java.util.Random;

import net.minecraft.world.World;
import net.minecraft.world.gen.structure.StructureBoundingBox;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.emoniph.witchery.worldgen.ComponentClonedStructure;

import rwg.support.WitcheryWorldgenCompat;

/** Stops Witchery's cloned structures from bridging RWG junction skylights. */
@Mixin(value = ComponentClonedStructure.class, remap = false)
public abstract class MixinComponentClonedStructure {

    @Inject(method = "addComponentParts", at = @At("HEAD"), cancellable = true, remap = false)
    private void rwg$rejectRiverJunctions(World world, Random random, CallbackInfoReturnable<Boolean> callback) {
        StructureBoundingBox bounds = ((ComponentClonedStructure) (Object) this).getBoundingBox();
        if (WitcheryWorldgenCompat.intersectsRiverJunctionOpening(world, bounds)) {
            callback.setReturnValue(false);
        }
    }
}
