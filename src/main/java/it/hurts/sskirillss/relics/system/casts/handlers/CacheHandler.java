package it.hurts.sskirillss.relics.system.casts.handlers;

import it.hurts.sskirillss.relics.items.relics.base.IRelicItem;
import it.hurts.sskirillss.relics.items.relics.base.data.cast.misc.RelicContainer;
import it.hurts.sskirillss.relics.system.casts.abilities.AbilityCache;
import it.hurts.sskirillss.relics.system.casts.abilities.AbilityReference;
import it.hurts.sskirillss.relics.utils.Reference;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiFunction;

@Mod.EventBusSubscriber(modid = Reference.MODID, value = Dist.CLIENT)
public class CacheHandler {
    public static final LinkedHashMap<AbilityReference, AbilityCache> REFERENCES = new LinkedHashMap<>();

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        Player player = event.player;

        if (!(player instanceof LocalPlayer))
            return;

        LinkedHashMap<AbilityReference, AbilityCache> references = new LinkedHashMap<>();

        for (RelicContainer source : RelicContainer.values()) {
            for (AbilityReference reference : source.gatherAbilities().apply(player)) {
                ItemStack stack = reference.getSlot().gatherStack(player);

                if (!(stack.getItem() instanceof IRelicItem relic))
                    continue;

                AbilityCache cache = REFERENCES.getOrDefault(reference, new AbilityCache());

                Map<String, Boolean> predicates = cache.getPredicates();

                for (Map.Entry<String, BiFunction<Player, ItemStack, Boolean>> predicate : relic.getAbilityCastData(reference.getId()).getCastPredicates().entrySet())
                    predicates.put(predicate.getKey(), predicate.getValue().apply(player, stack));

                cache.setPredicates(predicates);

                if (cache.getIconShakeDelta() > 0)
                    cache.setIconShakeDelta(cache.getIconShakeDelta() - 1);

                references.put(reference, cache);
            }
        }

        REFERENCES.clear();
        REFERENCES.putAll(references);
    }
}