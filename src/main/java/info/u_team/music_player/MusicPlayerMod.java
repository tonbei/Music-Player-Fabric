package info.u_team.music_player;

import info.u_team.music_player.init.MusicPlayerClientConstruct;
import net.fabricmc.api.ClientModInitializer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MusicPlayerMod implements ClientModInitializer {
	
	public static final String MODID = "musicplayer";
	public static Logger LOGGER = LogManager.getLogger("MusicPlayer");
	
	@Override
	public void onInitializeClient() {
		MusicPlayerClientConstruct.construct();
	}
}
