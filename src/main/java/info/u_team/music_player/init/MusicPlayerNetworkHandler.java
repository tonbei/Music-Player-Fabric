package info.u_team.music_player.init;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import info.u_team.music_player.MusicPlayerMod;
import info.u_team.music_player.lavaplayer.api.audio.IAudioTrack;
import info.u_team.music_player.lavaplayer.api.audio.IAudioTrackList;
import info.u_team.music_player.lavaplayer.api.queue.ITrackManager;
import info.u_team.music_player.musicplayer.MusicPlayerManager;
import info.u_team.music_player.musicplayer.playlist.LoadedTracks;
import info.u_team.music_player.musicplayer.playlist.Playlist;
import info.u_team.music_player.musicplayer.playlist.Skip;
import info.u_team.music_player.musicplayer.settings.Repeat;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import org.apache.commons.lang3.tuple.Pair;

import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class MusicPlayerNetworkHandler {

    private static final Gson GSON_NON_PRETTY = new GsonBuilder().enableComplexMapKeySerialization().disableHtmlEscaping().create();

    private static final ResourceLocation CHANNEL = new ResourceLocation("musicplayer:multi");

    private static Playlist MULTI_PLAYLIST;

    private static boolean isPlaying = false;
    private static String previousTrack = "";

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (MusicPlayerManager.getSettingsManager().getSettings().isMultiplayerMode()) {
                boolean current = MusicPlayerManager.getPlayer().getTrackManager().getCurrentTrack() != null;

                if (client.getConnection() != null && isPlaying && !current) {
                    Map<String, String> packetData = new HashMap<>();
                    packetData.put("playInfo", "STOP");
                    packetData.put("endTrack", previousTrack);
                    byte[] byteData = GSON_NON_PRETTY.toJson(packetData).getBytes(StandardCharsets.UTF_8);
                    ClientPlayNetworking.send(CHANNEL, new FriendlyByteBuf(Unpooled.copiedBuffer(byteData)));
                }
                isPlaying = current;
                //previousTrack = isPlaying ? previousTrack : "";
            }
        });

        ClientPlayNetworking.registerGlobalReceiver(CHANNEL, (client, handler, buf, responseSender) -> {
            if (!MusicPlayerManager.getSettingsManager().getSettings().isMultiplayerMode()) return;

            Type mapTokenType = new TypeToken<Map<String, String>>(){}.getType();
            Map<String, String> packetData = GSON_NON_PRETTY.fromJson(new String(ByteBufUtil.getBytes(buf), StandardCharsets.UTF_8), mapTokenType);

            MusicPlayerMod.LOGGER.warn(packetData);

            //final ITrackManager manager = MusicPlayerManager.getPlayer().getTrackManager();

            switch (packetData.getOrDefault("playStatus", "NONE")) {
                case "PLAY" -> {
                    if (previousTrack.equalsIgnoreCase(packetData.get("timeStamp"))) {
                        if (!isPlaying){
                            client.execute(() -> {
                                Map<String, String> sendData = new HashMap<>();
                                sendData.put("playInfo", "STOP");
                                sendData.put("endTrack", previousTrack);
                                byte[] byteData = GSON_NON_PRETTY.toJson(sendData).getBytes(StandardCharsets.UTF_8);
                                ClientPlayNetworking.send(CHANNEL, new FriendlyByteBuf(Unpooled.copiedBuffer(byteData)));
                            });
                        }
                        return;
                    }
                    playTrack(client, packetData.getOrDefault("URL", "null"));
                }
                case "STOP" -> stopMultiTrack();
                default -> MusicPlayerMod.LOGGER.warn("Unknown Packet");
            }

            if (packetData.containsKey("LOOP"))
                MusicPlayerManager.getSettingsManager().getSettings().setRepeat(Repeat.SINGLE);
            else
                MusicPlayerManager.getSettingsManager().getSettings().setRepeat(Repeat.NO);

            if (packetData.containsKey("timeStamp"))
                previousTrack = packetData.getOrDefault("timeStamp", "");

			/*client.execute(() -> {
				Player player = client.player;
				for (String string : strings)
					player.sendMessage(new TextComponent(string), player.getUUID());
			});*/
        });
    }

    public static void playTrack(Minecraft minecraft, String uri) {
        stopMultiTrack();

        MULTI_PLAYLIST = new Playlist(minecraft.getUser().getUuid());

        if (!MULTI_PLAYLIST.isLoaded())
            MULTI_PLAYLIST.load();

        MusicPlayerManager.getPlayer().getTrackSearch().getTracks(uri, result -> {
            minecraft.execute(() -> {

                if (result.hasError()) {
                    //setInformation(ChatFormatting.RED + result.getErrorMessage(), 150);
                    MusicPlayerMod.LOGGER.error(result.getErrorMessage());
                    return;
                } else if (result.isList()) {
                    final IAudioTrackList list = result.getTrackList();
                    MULTI_PLAYLIST.add(list.getTracks().get(0));
                } else {
                    final IAudioTrack track = result.getTrack();
                    MULTI_PLAYLIST.add(track);
                }

                final Runnable runnable = () -> {
                    final ITrackManager manager = MusicPlayerManager.getPlayer().getTrackManager();

                    // Start playlist
                    if (!MULTI_PLAYLIST.isEmpty()) {
                        final Pair<LoadedTracks, IAudioTrack> pair = MULTI_PLAYLIST.getFirstTrack();
                        MULTI_PLAYLIST.setPlayable(pair.getLeft(), pair.getRight());
                        if (pair.getLeft().hasError() || pair.getRight() == null) {
                            MULTI_PLAYLIST.skip(Skip.FORWARD);
                        }
                        manager.setTrackQueue(MULTI_PLAYLIST);
                        manager.start();
                    } else {
                        MULTI_PLAYLIST.setStopable();
                        manager.stop();
                    }
                };

                if (!MULTI_PLAYLIST.isLoaded()) {
                    MULTI_PLAYLIST.load(runnable);
                } else {
                    runnable.run();
                }
            });
        });
    }

    public static void stopMultiTrack() {
        if (MULTI_PLAYLIST != null)
            MULTI_PLAYLIST.setStopable();
        isPlaying = false;
        previousTrack = "";
        MusicPlayerManager.getPlayer().getTrackManager().stop();
    }

    public static void stopTrackInPlaylists() {
        if (MusicPlayerManager.getPlaylistManager().getPlaylists().getPlaying() != null)
            MusicPlayerManager.getPlaylistManager().getPlaylists().getPlaying().setStopable();

        MusicPlayerManager.getPlaylistManager().getPlaylists().setPlaying(null);
        MusicPlayerManager.getPlayer().getTrackManager().stop();
    }
}
