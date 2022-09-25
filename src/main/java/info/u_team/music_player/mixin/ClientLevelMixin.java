package info.u_team.music_player.mixin;

import info.u_team.music_player.init.MusicPlayerNetworkHandler;
import info.u_team.music_player.musicplayer.MusicPlayerManager;
import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientLevel.class)
public class ClientLevelMixin {

    @Inject(at = @At("HEAD"), method = "disconnect()V")
    private void disconnect(CallbackInfo ci) {
        if (!MusicPlayerManager.getSettingsManager().getSettings().isMultiplayerMode()) return;

        MusicPlayerNetworkHandler.stopMultiTrack();
    }
}
