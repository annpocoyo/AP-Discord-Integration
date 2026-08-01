package com.annpocoyo.apdiscordintegration.mixin;

import com.annpocoyo.apdiscordintegration.minecraft.GlobalTellRawEvent;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.ComponentArgument;
import net.minecraft.server.commands.TellRawCommand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TellRawCommand.class)
public class TellRawMixin {
	@Inject(at = @At("TAIL"), method = "lambda$register$0")
	private static void tellRawHead(CommandContext<CommandSourceStack> c, CallbackInfoReturnable<Integer> cir) throws CommandSyntaxException {
		if (cir.getReturnValue() == 0)
			return;

		// Detect if the message was sent to all players or all entities, if so, send message to event
		net.minecraft.commands.CommandSourceStack source = c.getSource();
		// noinspection SpellCheckingInspection
		String command = c.getInput().replace("tellraw ", "");
		if (command.startsWith("@a "))
			GlobalTellRawEvent.invoke(ComponentArgument.getResolvedComponent(c, "message"), source);
	}
}