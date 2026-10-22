package com.lastbroadcast.registry;

import com.lastbroadcast.LastBroadcast;
import com.lastbroadcast.entity.StaffNpc;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> TYPES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, LastBroadcast.MODID);

    public static final RegistryObject<EntityType<StaffNpc>> STAFF = TYPES.register("staff",
            () -> EntityType.Builder.of(StaffNpc::new, MobCategory.MISC).sized(0.6f, 1.8f).eyeHeight(1.62f)
                    .clientTrackingRange(10).build("staff"));

    private ModEntities() {}
}
