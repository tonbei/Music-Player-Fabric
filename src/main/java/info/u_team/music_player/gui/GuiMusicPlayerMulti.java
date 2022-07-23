package info.u_team.music_player.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import info.u_team.music_player.gui.controls.GuiVolumeSlider;
import info.u_team.music_player.gui.playlist.search.GuiMusicSearchList;
import info.u_team.music_player.gui.playlist.search.GuiMusicSearchListEntryMusicTrack;
import info.u_team.music_player.gui.playlist.search.GuiMusicSearchListEntryPlaylist;
import info.u_team.music_player.gui.playlist.search.IGuiMusicSearch;
import info.u_team.music_player.gui.playlist.search.SearchProvider;
import info.u_team.music_player.gui.settings.GuiMusicPlayerSettings;
import info.u_team.music_player.init.MusicPlayerColors;
import info.u_team.music_player.init.MusicPlayerNetworkHandler;
import info.u_team.music_player.init.MusicPlayerResources;
import info.u_team.music_player.lavaplayer.api.audio.IAudioTrack;
import info.u_team.music_player.lavaplayer.api.audio.IAudioTrackList;
import info.u_team.music_player.musicplayer.MusicPlayerManager;
import info.u_team.music_player.musicplayer.playlist.Playlist;
import info.u_team.music_player.musicplayer.settings.Settings;
import info.u_team.u_team_core.gui.elements.ImageButton;
import info.u_team.u_team_core.gui.elements.ScalableActivatableButton;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import org.lwjgl.glfw.GLFW;

import static info.u_team.music_player.init.MusicPlayerLocalization.*;

public class GuiMusicPlayerMulti extends BetterScreen implements IGuiMusicSearch {

    private final Playlist playlist;

    private EditBox searchField;

    private final GuiMusicSearchList searchList;

    private SearchProvider searchProvider;

    private String information;
    private int informationTicks;
    private int maxTicksInformation;

    public GuiMusicPlayerMulti() {
        super(new TextComponent("musicplayer"));

        this.playlist = null;
        searchList = new GuiMusicSearchList();
        searchProvider = SearchProvider.YOUTUBE;
    }

    @Override
    protected void init() {
        addRenderableWidget(new ImageButton(1, 1, 15, 15, MusicPlayerResources.TEXTURE_BACK, button -> minecraft.setScreen(null)));

        final boolean isSettings = minecraft.screen instanceof GuiMusicPlayerSettings;
        final boolean isIngame = minecraft.screen instanceof PauseScreen;
        final Settings settings = MusicPlayerManager.getSettingsManager().getSettings();
        final int modeY = width - (70 + (!isSettings ? 15 + 2 : 1));
        final ScalableActivatableButton multiplayerModeInGuiButton = addRenderableWidget(new ScalableActivatableButton(modeY, isSettings ? 1 : 18, 70, 15, Component.nullToEmpty(getTranslation(GUI_MULTIPLAYER_MODE)), 0.7F, settings.isMultiplayerMode(), MusicPlayerColors.LIGHT_GREEN));
        multiplayerModeInGuiButton.setPressable(() -> {
            settings.setMultiplayerMode(!settings.isMultiplayerMode());
            multiplayerModeInGuiButton.setActivated(settings.isMultiplayerMode());
            MusicPlayerNetworkHandler.stopMultiTrack();
            minecraft.setScreen(new GuiMusicPlayer());
        });

        final ImageButton settingsButton = addRenderableWidget(new ImageButton(width - (15 + 1), 1, 15, 15, MusicPlayerResources.TEXTURE_SETTINGS));
        settingsButton.setPressable(() -> minecraft.setScreen(new GuiMusicPlayerSettings(minecraft.screen)));

        final int volumeY = width - (70 + (isIngame ? 15 * 2 + 3 : (!isSettings ? 15 + 2 : 1)));
        addRenderableWidget(new GuiVolumeSlider(volumeY, 1, 70, 15, Component.nullToEmpty(getTranslation(GUI_CONTROLS_VOLUME) + ": "), Component.nullToEmpty("%"), 0, 100, settings.getVolume(), false, true, false, 0.7F, slider -> {
            settings.setVolume(slider.getValueInt());
            MusicPlayerManager.getPlayer().setVolume(settings.getVolume());
        }));

        final ImageButton searchButton = addRenderableWidget(new ImageButton(10, 76, 24, 24, searchProvider.getLogo()));
        searchButton.setPressable(() -> {
            searchProvider = SearchProvider.toggle(searchProvider);
            searchButton.setImage(searchProvider.getLogo());
        });

        searchField = new EditBox(font, 40, 78, width - 51, 20, Component.nullToEmpty("")) {

            @Override
            public boolean keyPressed(int key, int p_keyPressed_2_, int p_keyPressed_3_) {
                keyFromTextField(this, searchProvider.getPrefix() + getValue(), key);
                return super.keyPressed(key, p_keyPressed_2_, p_keyPressed_3_);
            }

            @Override
            public boolean changeFocus(boolean p_changeFocus_1_) {
                System.out.println("CHANGEED FOR Search FIELD to " + p_changeFocus_1_);
                return super.changeFocus(p_changeFocus_1_);
            }

        };
        searchField.setMaxLength(1000);
        searchField.setFocus(true);
        setFocused(searchField);
        addWidget(searchField);

        searchList.updateSettings(width - 24, height, 130, height - 10, 12, width - 12);
        addWidget(searchList);
    }

    @Override
    public void resize(Minecraft minecraft, int width, int height) {
        final String searchFieldText = searchField.getValue();
        final boolean searchFieldFocus = searchField.isFocused() && getFocused() == searchField;
        this.init(minecraft, width, height);
        searchField.setValue(searchFieldText);
        searchField.setFocus(searchFieldFocus);
        if (searchFieldFocus) {
            setFocused(searchField);
        }
    }

    @Override
    public void tick() {
        searchField.tick();
        informationTicks++;
    }

    @Override
    public void render(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        renderDirtBackground(0);
        searchList.render(matrixStack, mouseX, mouseY, partialTicks);

        drawString(matrixStack, minecraft.font, getTranslation(GUI_SEARCH_SEARCH_SEARCH), 10, 63, 0xFFFFFF);

        if (information != null && informationTicks <= maxTicksInformation) {
            drawString(matrixStack, minecraft.font, information, 15, 110, 0xFFFFFF);
        }

        searchField.render(matrixStack, mouseX, mouseY, partialTicks);

        super.render(matrixStack, mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (searchField.mouseClicked(mouseX, mouseY, button)) {
            setFocused(searchField);
            searchField.setFocus(true);
            //urlField.setFocus(false);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void setInformation(String information, int maxTicksInformation) {
        this.information = information;
        this.maxTicksInformation = maxTicksInformation;
        informationTicks = 0;
    }

    private void keyFromTextField(EditBox field, String text, int key) {
        if (field.isVisible() && field.isFocused() && (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER)) {
            searchList.clear();
            addTrack(text);
            field.setValue("");
        }
    }

    private void addTrack(String uri) {
        MusicPlayerManager.getPlayer().getTrackSearch().getTracks(uri, result -> {
            minecraft.execute(() -> {
                if (result.hasError()) {
                    setInformation(ChatFormatting.RED + result.getErrorMessage(), 150);
                } else if (result.isList()) {
                    final IAudioTrackList list = result.getTrackList();
                    if (!list.isSearch()) {
                        searchList.add(new GuiMusicSearchListEntryPlaylist(this, playlist, list));
                    }
                    list.getTracks().forEach(track -> searchList.add(new GuiMusicSearchListEntryMusicTrack(this, playlist, track, !list.isSearch())));
                } else {
                    final IAudioTrack track = result.getTrack();
                    searchList.add(new GuiMusicSearchListEntryMusicTrack(this, playlist, track, false));
                }
            });
        });
    }
}
