package it.hurts.sskirillss.relics.network.packets.research;

import io.netty.buffer.ByteBuf;
import it.hurts.sskirillss.relics.client.screen.description.misc.DescriptionUtils;
import it.hurts.sskirillss.relics.items.relics.base.IRelicItem;
import it.hurts.sskirillss.relics.utils.Reference;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ByIdMap;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.function.IntFunction;

@Data
@AllArgsConstructor
public class PacketManageLink implements CustomPacketPayload {
    private final int container;
    private final int slot;
    private final String ability;
    private final Operation operation;
    private final int from;
    private final int to;

    public static final Type<PacketManageLink> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Reference.MODID, "manage_link"));

    public static final StreamCodec<ByteBuf, PacketManageLink> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, PacketManageLink::getContainer,
            ByteBufCodecs.INT, PacketManageLink::getSlot,
            ByteBufCodecs.STRING_UTF8, PacketManageLink::getAbility,
            ByteBufCodecs.idMapper(Operation.BY_ID, Operation::getId), PacketManageLink::getOperation,
            ByteBufCodecs.INT, PacketManageLink::getFrom,
            ByteBufCodecs.INT, PacketManageLink::getTo,
            PacketManageLink::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            Player player = ctx.player();

            if (player.containerMenu.containerId != container) {
                causeError(player);

                return;
            }

            ItemStack stack = DescriptionUtils.gatherRelicStack(player, slot);

            if (!(stack.getItem() instanceof IRelicItem relic)) {
                causeError(player);

                return;
            }

            switch (operation) {
                case ADD -> relic.addResearchLink(stack, ability, from, to);
                case REMOVE -> relic.removeResearchLink(stack, ability, from, to);
            }

            try {
                player.containerMenu.getSlot(slot).set(stack);
            } catch (Exception e) {
                e.printStackTrace();

                causeError(player);
            }
        });
    }

    private static void causeError(Player player) {
        player.displayClientMessage(Component.translatable("info.relics.researching.wrong_container").withStyle(ChatFormatting.RED), false);

        player.closeContainer();
    }

    @Getter
    @AllArgsConstructor
    public enum Operation {
        ADD(0),
        REMOVE(1);

        public static final IntFunction<Operation> BY_ID = ByIdMap.continuous(Operation::getId, Operation.values(), ByIdMap.OutOfBoundsStrategy.ZERO);

        private final int id;
    }
}