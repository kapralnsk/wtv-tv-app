import { getProfile, getChannel, joinStream, joinChat, getChatBacklog } from './api.ts';
import { ChatWebSocket } from './chat.ts';
import { StateManager } from './state.ts';
import { createPlayer } from './player/index.ts';
import { initLayout } from './ui/layout.ts';
import { initChat } from './ui/chat.ts';
import { initOverlays } from './ui/overlays.ts';
import { initNav } from './nav.ts';

const CHANNEL_SLUG = 'dunduk';

async function main(): Promise<void> {
    const state = new StateManager();
    const container = document.getElementById('video-container')!;
    const player = createPlayer(container);

    player.setListener({
        onReady() {
            state.setAvailableQualities(player.getQualities());
        },
        onError(e) {
            state.setError(`Playback error: ${String(e)}`);
        },
    });

    initLayout(state);
    initChat(state);
    initOverlays(state, {
        onQualityPickerToggle: () => state.toggleQualityPicker(),
        onQualitySelect(index) {
            const s = state.getState();
            if (s.type !== 'Playing') return;
            const quality = s.availableQualities[index];
            if (!quality) return;
            state.selectQuality(quality);
            player.selectQuality(index);
        },
    });
    initNav(state, {
        onQualitySelect(index) {
            const s = state.getState();
            if (s.type !== 'Playing') return;
            const quality = s.availableQualities[index];
            if (!quality) return;
            state.selectQuality(quality);
            player.selectQuality(index);
        },
    });

    state.setLoading();

    try {
        const profile = await getProfile(CHANNEL_SLUG);
        const channel = await getChannel(profile.userId);

        if (!channel.liveStream) {
            state.setError(`${CHANNEL_SLUG} is not live`);
            return;
        }

        const stream = channel.liveStream;
        const startedAtMs = new Date(stream.startedAt).getTime() || Date.now();

        state.setPlaying({
            channelId: profile.userId,
            streamId: stream.streamId,
            playbackUrl: stream.playbackUrl,
            title: stream.title,
            startedAtMs,
            channelName: channel.name,
        });

        // Fire-and-forget: viewer join counter
        joinStream(stream.streamId).catch(() => {});

        // Start video — qualities arrive via player.onReady
        player.open(stream.playbackUrl).catch(e => {
            state.setError(`Failed to open stream: ${String(e)}`);
        });

        // Chat — non-fatal: failure must not interrupt video
        loadChat(profile.userId, state).catch(() => {});
    } catch (e) {
        state.setError(e instanceof Error ? e.message : String(e));
    }
}

async function loadChat(channelId: string, state: StateManager): Promise<void> {
    const [backlog, token] = await Promise.all([
        getChatBacklog(channelId),
        joinChat(channelId),
    ]);

    state.addBacklogMessages(
        backlog.map(m => ({ id: m.messageId, nickname: m.sender.nickname, content: m.content })),
    );

    const ws = new ChatWebSocket(
        token,
        msg => state.addNewMessage(msg),
        () => {}, // chat errors are silent
    );
    ws.connect();
}

main();
