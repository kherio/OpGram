package com.my.televip.Configs;

import com.my.televip.Clients.ClientManager;
import com.my.televip.Clients.Telegraph;
import com.my.televip.features.ui.DisableChannelSwipeBack;
import com.my.televip.features.ui.DisableNumberRounding;
import com.my.televip.features.ui.ExactLastSeen;
import com.my.televip.features.extra.ConfirmSending;
import com.my.televip.features.extra.UndoSend;
import com.my.televip.features.extra.VoicePlaybackSpeed;
import com.my.televip.features.extra.BatterySaver;
import com.my.televip.features.extra.ChatLock;
import com.my.televip.features.extra.HideSponsoredMessages;
import com.my.televip.features.ui.DisableProfileSwipeBack;
import com.my.televip.features.stories.DisableStories;
import com.my.televip.features.connections.DownloadSpeed;
import com.my.televip.features.media.EnableSavingStories;
import com.my.televip.features.other.FixTLError;
import com.my.televip.features.ghostMode.GhostMode;
import com.my.televip.features.ghostMode.HidePhone;
import com.my.televip.features.ui.HidePinnedMessages;
import com.my.televip.features.ui.HideProxySponsor;
import com.my.televip.features.other.HideUpdateApp;
import com.my.televip.features.ui.HijriDate;
import com.my.televip.features.media.PreventMedia;
import com.my.televip.features.other.RemovesContentSaving;
import com.my.televip.features.messages.SaveEditsHistory;
import com.my.televip.features.media.SecretMediaSave;
import com.my.televip.features.other.TelePremium;
import com.my.televip.features.media.VoiceToMusicHook;
import com.my.televip.features.messages.MessageTimeModifier;
import com.my.televip.features.messages.ShowDeletedMessages;
import com.my.televip.features.otherFeatures.AlwaysSaveMedia;
import com.my.televip.features.otherFeatures.CopyNameHook;
import com.my.televip.features.otherFeatures.EditOnlineTextView;
import com.my.televip.features.otherFeatures.FeatureInitializer;
import com.my.televip.language.Keys;
import com.my.televip.language.Translator;
import com.my.televip.logging.Logger;

import java.util.ArrayList;
import java.util.List;


public class ConfigManager {
    

    private static final List<ConfigItem> items = new ArrayList<>();


    // GhostMode
    public static ConfigItem ghostModeSettings;
    public static ConfigItem hideSeenPrivateChat;
    public static ConfigItem hideSeenChannel;
    public static ConfigItem hideSeen;
    public static ConfigItem markReadAfterSend;
    public static ConfigItem hideTyping;
    public static ConfigItem hideStoryView;
    public static ConfigItem hidePhone;
    public static ConfigItem hideOnline;
    public static ConfigItem onlineInfo;

    public static ConfigItem shadows;

    // Stories
    public static ConfigItem stories;
    public static ConfigItem disableStories;

    // Messages
    public static ConfigItem messages;
    public static ConfigItem showDeletedMessages;
    public static ConfigItem showMessageId;
    public static ConfigItem saveEditsHistory;

    // Connections
    public static ConfigItem connections;
    public static ConfigItem downloadSpeed;

    // Media
    public static ConfigItem media;
    public static ConfigItem secretMediaSave;
    public static ConfigItem preventMedia;
    public static ConfigItem enableSavingStories;
    public static ConfigItem enableVoiceMessageSaving;

    //UI
    public static ConfigItem ui;
    public static ConfigItem hidePinnedMessages;
    public static ConfigItem disableChannelSwipeBack;
    public static ConfigItem disableProfileSwipeBack;
    public static ConfigItem hideProxySponsor;
    public static ConfigItem showUserID;
    public static ConfigItem customCalendar;

    // Other Features
    public static ConfigItem otherFeatures;
    public static ConfigItem removesContentSaving;
    public static ConfigItem telegramPremium;
    public static ConfigItem disableNumberRounding;
    public static ConfigItem exactLastSeen;
    public static ConfigItem hideListened;
    public static ConfigItem hideRecording;
    public static ConfigItem showSecondsInTime;
    public static ConfigItem hideSponsoredMessages;
    public static ConfigItem confirmSending;
    public static ConfigItem undoSend;
    public static ConfigItem reminders;
    public static ConfigItem showDeletedTime;
    public static ConfigItem alertDeleted;
    public static ConfigItem alertEdited;
    public static ConfigItem voicePlaybackSpeed;
    public static ConfigItem noStoriesPreload;
    public static ConfigItem hideStoriesBar;
    public static ConfigItem chatLock;
    public static ConfigItem backupHeader;
    public static ConfigItem btnExportSettings;
    public static ConfigItem btnImportSettings;
    public static ConfigItem btnCheckUpdate;
    public static ConfigItem btnViewEditsHistory;
    public static ConfigItem btnQuickReplies;
    public static ConfigItem btnClearEditsHistory;
    public static ConfigItem hideUpdateApp;
    public static ConfigItem fixTLError;

    // Button
    public static ConfigItem btnChannel;
    public static ConfigItem btnRestartApp;

    public static void loadAndRead(){
        ConfigPreferences.init();
        load();
    }

    public static void load() {
        items.clear();

        // GhostMode
        ghostModeSettings = new ConfigItem(ConfigItem.HEADER, Keys.GhostModeSettings);
        items.add(ghostModeSettings);

        List<ConfigItem> childrenHideSeen = new ArrayList<>();

        hideSeenPrivateChat = new ConfigItem(ConfigItem.SWITCH, Keys.HideSeenPrivateChat, ConfigPreferences.getBoolean(Keys.HideSeenPrivateChat), GhostMode::init);
        childrenHideSeen.add(hideSeenPrivateChat);

        hideSeenChannel = new ConfigItem(ConfigItem.SWITCH, Keys.HideSeenChannel, ConfigPreferences.getBoolean(Keys.HideSeenChannel), GhostMode::init);
        childrenHideSeen.add(hideSeenChannel);

        hideSeen = new ConfigItem(ConfigItem.EXPANDABLE_SWITCH, Keys.HideSeen, childrenHideSeen);
        items.add(hideSeen);

        markReadAfterSend = new ConfigItem(ConfigItem.SWITCH, Keys.MarkReadAfterSend, ConfigPreferences.getBoolean(Keys.MarkReadAfterSend), GhostMode::init);
        items.add(markReadAfterSend);

        hideTyping = new ConfigItem(ConfigItem.SWITCH, Keys.HideTyping, ConfigPreferences.getBoolean(Keys.HideTyping), GhostMode::init);
        items.add(hideTyping);

        hideRecording = new ConfigItem(ConfigItem.SWITCH, Keys.HideRecording, ConfigPreferences.getBoolean(Keys.HideRecording), GhostMode::init);
        items.add(hideRecording);

        hideListened = new ConfigItem(ConfigItem.SWITCH, Keys.HideListened, ConfigPreferences.getBoolean(Keys.HideListened), GhostMode::init);
        items.add(hideListened);

        hideStoryView = new ConfigItem(ConfigItem.SWITCH, Keys.HideStoryView, ConfigPreferences.getBoolean(Keys.HideStoryView), GhostMode::init);
        items.add(hideStoryView);

        hidePhone = new ConfigItem(ConfigItem.SWITCH, Keys.HidePhone, true, ConfigPreferences.getBoolean(Keys.HidePhone), HidePhone::init);
        items.add(hidePhone);

        hideOnline = new ConfigItem(ConfigItem.SWITCH, Keys.HideOnline, true, ConfigPreferences.getBoolean(Keys.HideOnline), GhostMode::init);
        items.add(hideOnline);

        onlineInfo = new ConfigItem(ConfigItem.INFO, Keys.OfflineVisibilityInfo);
        items.add(onlineInfo);

        shadows = new ConfigItem(ConfigItem.DIVIDER);
        if (!ClientManager.is(ClientManager.Client.Nekogram) && !ClientManager.is(ClientManager.Client.Cherrygram)) {
            exactLastSeen = new ConfigItem(ConfigItem.SWITCH, Keys.ExactLastSeen, "14:32 -> 23/09/2026 14:32:07", ConfigPreferences.getBoolean(Keys.ExactLastSeen), ExactLastSeen::init);
            items.add(exactLastSeen);
        }

        items.add(shadows);

        // Stories
        stories = new ConfigItem(ConfigItem.HEADER, Keys.StoriesSettings);
        items.add(stories);

        disableStories = new ConfigItem(ConfigItem.SWITCH, Keys.DisableStories, true, ConfigPreferences.getBoolean(Keys.DisableStories), DisableStories::init);
        items.add(disableStories);

        items.add(shadows);

        // Messages
        messages = new ConfigItem(ConfigItem.HEADER, Keys.MessagesSettings);
        items.add(messages);

        showDeletedMessages = new ConfigItem(ConfigItem.SWITCH, Keys.ShowDeletedMessages, ConfigPreferences.getBoolean(Keys.ShowDeletedMessages), ShowDeletedMessages::init);
        items.add(showDeletedMessages);

        if (!ClientManager.is(ClientManager.Client.NagramX)) {
            showMessageId = new ConfigItem(ConfigItem.SWITCH, Keys.ShowMessageID, ConfigPreferences.getBoolean(Keys.ShowMessageID), MessageTimeModifier::init);
            items.add(showMessageId);
        }

        saveEditsHistory = new ConfigItem(ConfigItem.SWITCH, Keys.SaveEditsHistory, ConfigPreferences.getBoolean(Keys.SaveEditsHistory), SaveEditsHistory::init);
        items.add(saveEditsHistory);

        showSecondsInTime = new ConfigItem(ConfigItem.SWITCH, Keys.ShowSecondsInTime, "14:32 -> 14:32:07", ConfigPreferences.getBoolean(Keys.ShowSecondsInTime), MessageTimeModifier::init);
        items.add(showSecondsInTime);

        showDeletedTime = new ConfigItem(ConfigItem.SWITCH, Keys.ShowDeletedTime, ConfigPreferences.getBoolean(Keys.ShowDeletedTime), MessageTimeModifier::init);
        items.add(showDeletedTime);

        alertDeleted = new ConfigItem(ConfigItem.SWITCH, Keys.AlertDeleted, ConfigPreferences.getBoolean(Keys.AlertDeleted), null);
        items.add(alertDeleted);

        alertEdited = new ConfigItem(ConfigItem.SWITCH, Keys.AlertEdited, ConfigPreferences.getBoolean(Keys.AlertEdited), null);
        items.add(alertEdited);

        voicePlaybackSpeed = new ConfigItem(ConfigItem.SWITCH, Keys.VoicePlaybackSpeed, "1.5x", ConfigPreferences.getBoolean(Keys.VoicePlaybackSpeed), VoicePlaybackSpeed::init);
        items.add(voicePlaybackSpeed);

        noStoriesPreload = new ConfigItem(ConfigItem.SWITCH, Keys.NoStoriesPreload, ConfigPreferences.getBoolean(Keys.NoStoriesPreload), BatterySaver::init);
        items.add(noStoriesPreload);

        hideStoriesBar = new ConfigItem(ConfigItem.SWITCH, Keys.HideStoriesBar, ConfigPreferences.getBoolean(Keys.HideStoriesBar), BatterySaver::init);
        items.add(hideStoriesBar);

        chatLock = new ConfigItem(ConfigItem.SWITCH, Keys.ChatLock, ConfigPreferences.getBoolean(Keys.ChatLock), ChatLock::init);
        items.add(chatLock);

        confirmSending = new ConfigItem(ConfigItem.SWITCH, Keys.ConfirmSending, ConfigPreferences.getBoolean(Keys.ConfirmSending), ConfirmSending::init);
        items.add(confirmSending);

        undoSend = new ConfigItem(ConfigItem.SWITCH, Keys.UndoSend, ConfigPreferences.getBoolean(Keys.UndoSend), UndoSend::init);
        items.add(undoSend);

        reminders = new ConfigItem(ConfigItem.SWITCH, Keys.Reminders, ConfigPreferences.getBoolean(Keys.Reminders), null);
        items.add(reminders);

        hideSponsoredMessages = new ConfigItem(ConfigItem.SWITCH, Keys.HideSponsoredMessages, ConfigPreferences.getBoolean(Keys.HideSponsoredMessages), HideSponsoredMessages::init);
        items.add(hideSponsoredMessages);

        items.add(shadows);

        // Connections
        connections = new ConfigItem(ConfigItem.HEADER, Keys.ConnectionsSettings);
        items.add(connections);

        downloadSpeed = new ConfigItem(ConfigItem.SWITCH, Keys.DownloadSpeed, ConfigPreferences.getBoolean(Keys.DownloadSpeed), DownloadSpeed::init);
        items.add(downloadSpeed);

        items.add(shadows);

        // Media
        media = new ConfigItem(ConfigItem.HEADER, Keys.MediaSettings);
        items.add(media);

        if (!ClientManager.is(ClientManager.Client.Nekogram) && !ClientManager.is(ClientManager.Client.Cherrygram)) {
            secretMediaSave = new ConfigItem(ConfigItem.SWITCH, Keys.SecretMediaSave, ConfigPreferences.getBoolean(Keys.SecretMediaSave), SecretMediaSave::init);
            items.add(secretMediaSave);
        }

        preventMedia = new ConfigItem(ConfigItem.SWITCH, Keys.PreventMedia, ConfigPreferences.getBoolean(Keys.PreventMedia), PreventMedia::init);
        items.add(preventMedia);

        enableSavingStories = new ConfigItem(ConfigItem.SWITCH, Keys.EnableSavingStories, ConfigPreferences.getBoolean(Keys.EnableSavingStories), EnableSavingStories::init);
        items.add(enableSavingStories);

        enableVoiceMessageSaving = new ConfigItem(ConfigItem.SWITCH, Keys.EnableVoiceMessageSaving, ConfigPreferences.getBoolean(Keys.EnableVoiceMessageSaving), VoiceToMusicHook::init);
        items.add(enableVoiceMessageSaving);

        items.add(shadows);

        // UI
        ui = new ConfigItem(ConfigItem.HEADER, Keys.UiSettings);
        items.add(ui);

        hidePinnedMessages = new ConfigItem(ConfigItem.SWITCH, Keys.HidePinnedMessages, ConfigPreferences.getBoolean(Keys.HidePinnedMessages), HidePinnedMessages::init);
        items.add(hidePinnedMessages);

        disableChannelSwipeBack = new ConfigItem(ConfigItem.SWITCH, Keys.DisableChannelSwipeBack, ConfigPreferences.getBoolean(Keys.DisableChannelSwipeBack), DisableChannelSwipeBack::init);
        items.add(disableChannelSwipeBack);

        disableProfileSwipeBack = new ConfigItem(ConfigItem.SWITCH, Keys.DisableProfileSwipeBack, ConfigPreferences.getBoolean(Keys.DisableProfileSwipeBack), DisableProfileSwipeBack::init);
        items.add(disableProfileSwipeBack);

        hideProxySponsor = new ConfigItem(ConfigItem.SWITCH, Keys.HideProxySponsor, true, ConfigPreferences.getBoolean(Keys.HideProxySponsor), HideProxySponsor::init);
        items.add(hideProxySponsor);

        if (!ClientManager.is(ClientManager.Client.Telegraph) && !ClientManager.is(ClientManager.Client.Nekogram) && !ClientManager.is(ClientManager.Client.Cherrygram)) {
            showUserID = new ConfigItem(ConfigItem.SWITCH, Keys.ShowUserID, ConfigPreferences.getBoolean(Keys.ShowUserID), EditOnlineTextView::init);
            items.add(showUserID);
            customCalendar = new ConfigItem(ConfigItem.TEXT, Keys.Calendar, true, HijriDate::init);
            items.add(customCalendar);
        }

        items.add(shadows);

        // Other Features
        otherFeatures = new ConfigItem(ConfigItem.HEADER, Keys.OtherFeaturesSettings);
        items.add(otherFeatures);

        removesContentSaving = new ConfigItem(ConfigItem.SWITCH, Keys.RemovesContentSaving, ConfigPreferences.getBoolean(Keys.RemovesContentSaving), RemovesContentSaving::init);
        items.add(removesContentSaving);

        telegramPremium = new ConfigItem(ConfigItem.SWITCH, Keys.TelegramPremium, ConfigPreferences.getBoolean(Keys.TelegramPremium), TelePremium::init);
        items.add(telegramPremium);

        if (!ClientManager.is(ClientManager.Client.Telegraph)) {
            disableNumberRounding = new ConfigItem(ConfigItem.SWITCH, Keys.DisableNumberRounding, "5.3K -> 5300", ConfigPreferences.getBoolean(Keys.DisableNumberRounding), DisableNumberRounding::init);
            hideUpdateApp = new ConfigItem(ConfigItem.SWITCH, Keys.HideUpdateApp, true, ConfigPreferences.getBoolean(Keys.HideUpdateApp), HideUpdateApp::init);
            fixTLError = new ConfigItem(ConfigItem.SWITCH, Keys.FixTLError, ConfigPreferences.getBoolean(Keys.FixTLError), FixTLError::init);
            items.add(disableNumberRounding);
            items.add(hideUpdateApp);
            items.add(fixTLError);
        }

        items.add(shadows);

        btnChannel = new ConfigItem(ConfigItem.TEXT, Keys.DeveloperChannel);
        items.add(btnChannel);

        items.add(shadows);

        backupHeader = new ConfigItem(ConfigItem.HEADER, Keys.BackupSettings);
        items.add(backupHeader);
        btnCheckUpdate = new ConfigItem(ConfigItem.TEXT, Keys.CheckForUpdates);
        items.add(btnCheckUpdate);
        btnExportSettings = new ConfigItem(ConfigItem.TEXT, Keys.ExportSettings);
        items.add(btnExportSettings);
        btnImportSettings = new ConfigItem(ConfigItem.TEXT, Keys.ImportSettings);
        items.add(btnImportSettings);
        btnViewEditsHistory = new ConfigItem(ConfigItem.TEXT, Keys.ViewEditsHistory);
        items.add(btnViewEditsHistory);
        btnQuickReplies = new ConfigItem(ConfigItem.TEXT, Keys.QuickReplies);
        items.add(btnQuickReplies);
        btnClearEditsHistory = new ConfigItem(ConfigItem.TEXT, Keys.ClearEditsHistory);
        items.add(btnClearEditsHistory);

        items.add(shadows);

        btnRestartApp = new ConfigItem(ConfigItem.TEXT, Keys.RestartApp);
        items.add(btnRestartApp);

        items.add(shadows);

        readFeature();

    }

    // ---- Search & collapsible sections (view over the full item list) ----
    private static String searchQuery = "";
    private static final java.util.Set<String> collapsed = new java.util.HashSet<>();
    private static final List<ConfigItem> viewItems = new ArrayList<>();

    public static void setSearchQuery(String q) {
        searchQuery = q == null ? "" : q.trim().toLowerCase();
        rebuildView();
    }

    public static String getSearchQuery() { return searchQuery; }

    public static boolean isCollapsed(String headerKey) { return collapsed.contains(headerKey); }

    public static void toggleCollapseByKey(String headerKey) {
        if (headerKey == null) return;
        if (!collapsed.remove(headerKey)) collapsed.add(headerKey);
        rebuildView();
    }

    /** Rebuilds the visible list from the full one, applying the search filter and collapsed sections. */
    public static void rebuildView() {
        viewItems.clear();
        boolean searching = !searchQuery.isEmpty();
        String currentHeader = null;
        boolean currentHidden = false;
        for (ConfigItem it : items) {
            int type = it.getType();
            if (type == ConfigItem.HEADER) {
                currentHeader = it.getKey();
                currentHidden = !searching && collapsed.contains(currentHeader);
                if (searching) {
                    // headers are added lazily only if a child matches (handled below)
                    continue;
                }
                viewItems.add(it);
                continue;
            }
            if (searching) {
                if (!matches(it)) continue;
                viewItems.add(it);
            } else {
                if (currentHidden) continue;
                viewItems.add(it);
            }
        }
    }

    private static boolean matches(ConfigItem it) {
        String label = Translator.get(it.getKey());
        if (label != null && label.toLowerCase().contains(searchQuery)) return true;
        String key = it.getKey();
        return key != null && key.toLowerCase().contains(searchQuery);
    }

    public static List<ConfigItem> getItems() {
        if (viewItems.isEmpty() && searchQuery.isEmpty() && collapsed.isEmpty()) return items;
        return viewItems;
    }

    public static List<ConfigItem> getAllItems() { return items; }

    public static void readFeature() {
        try {
            for (ConfigItem item : items) {
                if (item == null) continue;
                if (item.getType() != ConfigItem.SWITCH && item.getCustomCalendar() == 0) continue;
                if (item.isEnable()) item.run();
            }

            if (!ClientManager.is(ClientManager.Client.Telegraph) && !ClientManager.is(ClientManager.Client.Nekogram) && !ClientManager.is(ClientManager.Client.Cherrygram)) {
                FeatureInitializer.init();
                CopyNameHook.init();
                EditOnlineTextView.init();
            }
            AlwaysSaveMedia.init();

            if (ClientManager.is(ClientManager.Client.Telegraph)) Telegraph.removeAd();

        } catch (Throwable e) {
            Logger.e(e);
        }
    }

    public static boolean isGhostMode(){
        return hideSeen.isEnable() ||
                (hideListened != null && hideListened.isEnable()) ||
                (hideRecording != null && hideRecording.isEnable()) ||
                hideStoryView.isEnable() ||
                hideTyping.isEnable() ||
                hideOnline.isEnable() ||
                markReadAfterSend.isEnable();
    }

}
