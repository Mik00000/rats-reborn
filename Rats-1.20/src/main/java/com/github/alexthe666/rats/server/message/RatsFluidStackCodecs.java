package com.github.alexthe666.rats.server.message;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * Кодеки сетевой сериализации FluidStack, допускающие пустой стек.
 * В 1.21.1 стандартный FluidStack.STREAM_CODEC не кодирует EMPTY.
 */
public final class RatsFluidStackCodecs {

	private RatsFluidStackCodecs() {
	}

	public static final StreamCodec<RegistryFriendlyByteBuf, FluidStack> STREAM_CODEC = new StreamCodec<>() {
		@Override
		public FluidStack decode(RegistryFriendlyByteBuf buf) {
			if (!buf.readBoolean()) {
				return FluidStack.EMPTY;
			}
			return FluidStack.STREAM_CODEC.decode(buf);
		}

		@Override
		public void encode(RegistryFriendlyByteBuf buf, FluidStack stack) {
			if (stack.isEmpty()) {
				buf.writeBoolean(false);
				return;
			}
			buf.writeBoolean(true);
			FluidStack.STREAM_CODEC.encode(buf, stack);
		}
	};
}