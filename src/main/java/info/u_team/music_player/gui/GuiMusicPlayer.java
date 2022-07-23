package info.u_team.music_player.gui;

import info.u_team.music_player.gui.settings.GuiMusicPlayerSettings;
import info.u_team.music_player.init.MusicPlayerColors;
import info.u_team.music_player.init.MusicPlayerNetworkHandler;
import info.u_team.music_player.musicplayer.MusicPlayerManager;
import info.u_team.music_player.musicplayer.settings.Repeat;
import info.u_team.music_player.musicplayer.settings.Settings;
import info.u_team.u_team_core.gui.elements.ScalableActivatableButton;
import net.minecraft.client.gui.screens.PauseScreen;
import org.apache.commons.lang3.StringUtils;

import com.mojang.blaze3d.vertex.PoseStack;

import info.u_team.music_player.gui.controls.GuiControls;
import info.u_team.music_player.init.MusicPlayerResources;
import info.u_team.u_team_core.gui.elements.ImageButton;
import info.u_team.u_team_core.gui.elements.ScrollingText;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;

import static info.u_team.music_player.init.MusicPlayerLocalization.*;

public class GuiMusicPlayer extends BetterScreen {
	
	private EditBox namePlaylistField;
	
	private GuiMusicPlayerList playlistsList;
	
	private GuiControls controls;
	
	public GuiMusicPlayer() {
		super(new TextComponent("musicplayer"));
	}
	
	@Override
	protected void init() {
		addRenderableWidget(new ImageButton(1, 1, 15, 15, MusicPlayerResources.TEXTURE_BACK, button -> minecraft.setScreen(null)));

		final boolean isSettings = minecraft.screen instanceof GuiMusicPlayerSettings;
		final Settings settings = MusicPlayerManager.getSettingsManager().getSettings();
		final int modeY = width - (70 + (!isSettings ? 15 + 2 : 1));
		final ScalableActivatableButton multiplayerModeInGuiButton = addRenderableWidget(new ScalableActivatableButton(modeY, isSettings ? 1 : 18, 70, 15, Component.nullToEmpty(getTranslation(GUI_MULTIPLAYER_MODE)), 0.7F, settings.isMultiplayerMode(), MusicPlayerColors.LIGHT_GREEN));
		multiplayerModeInGuiButton.setPressable(() -> {
			settings.setMultiplayerMode(!settings.isMultiplayerMode());
			multiplayerModeInGuiButton.setActivated(settings.isMultiplayerMode());
			settings.setShowIngameMenueOverlay(false);
			settings.setRepeat(Repeat.NO);
			MusicPlayerNetworkHandler.stopTrackInPlaylists();
			minecraft.setScreen(new GuiMusicPlayerMulti());
		});

		namePlaylistField = new EditBox(font, 100, 60, width - 150, 20, Component.nullToEmpty(null));
		namePlaylistField.setMaxLength(500);
		addWidget(namePlaylistField);
		
		final ImageButton addPlaylistButton = addRenderableWidget(new ImageButton(width - 41, 59, 22, 22, MusicPlayerResources.TEXTURE_CREATE));
		addPlaylistButton.setPressable(() -> {
			final String name = namePlaylistField.getValue();
			if (StringUtils.isBlank(name) || name.equals(getTranslation(GUI_CREATE_PLAYLIST_INSERT_NAME))) {
				namePlaylistField.setValue(getTranslation(GUI_CREATE_PLAYLIST_INSERT_NAME));
				return;
			}
			playlistsList.addPlaylist(name);
			namePlaylistField.setValue("");
		});
		
		playlistsList = new GuiMusicPlayerList(width - 24, height, 90, height - 10, 12, width - 12);
		addWidget(playlistsList);
		
		controls = new GuiControls(this, 5, width);
		addWidget(controls);
	}
	
	@Override
	public void resize(Minecraft minecraft, int width, int height) {
		final String text = namePlaylistField.getValue();
		final ScrollingText titleRender = controls.getTitleRender();
		final ScrollingText authorRender = controls.getAuthorRender();
		this.init(minecraft, width, height);
		namePlaylistField.setValue(text);
		controls.copyTitleRendererState(titleRender);
		controls.copyAuthorRendererState(authorRender);
	}
	
	@Override
	public void tick() {
		namePlaylistField.tick();
		controls.tick();
	}
	
	@Override
	public void render(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks) {
		renderDirtBackground(0);
		playlistsList.render(matrixStack, mouseX, mouseY, partialTicks);
		font.draw(matrixStack, getTranslation(GUI_CREATE_PLAYLIST_ADD_LIST), 20, 65, 0xFFFFFF);
		namePlaylistField.render(matrixStack, mouseX, mouseY, partialTicks);
		controls.render(matrixStack, mouseX, mouseY, partialTicks);
		super.render(matrixStack, mouseX, mouseY, partialTicks);
	}
	
	public GuiMusicPlayerList getPlaylistsList() {
		return playlistsList;
	}
	
}
