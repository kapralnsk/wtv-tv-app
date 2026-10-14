import type { ChatMessage } from './chat.ts';

export interface StreamInfo {
    channelId: string;
    streamId: string;
    playbackUrl: string;
    title: string;
    startedAtMs: number;
    channelName: string;
}

export type PlayingState = {
    type: 'Playing';
    streamInfo: StreamInfo;
    availableQualities: string[];
    selectedQuality: string;
    isChatVisible: boolean;
    isQualityPickerVisible: boolean;
    isMetadataVisible: boolean;
    chatMessages: ChatMessage[];
};

export type AppState =
    | { type: 'Loading' }
    | PlayingState
    | { type: 'Error'; message: string };

type Listener = (state: AppState) => void;

const CHAT_CAP = 200;

export class StateManager {
    private _state: AppState = { type: 'Loading' };
    private listeners: Listener[] = [];
    private chatMessages: ChatMessage[] = [];
    private metadataHideTimer: ReturnType<typeof setTimeout> | null = null;

    getState(): AppState {
        return this._state;
    }

    subscribe(fn: Listener): () => void {
        this.listeners.push(fn);
        return () => {
            this.listeners = this.listeners.filter(l => l !== fn);
        };
    }

    private emit(): void {
        for (const l of this.listeners) l(this._state);
    }

    setLoading(): void {
        this._state = { type: 'Loading' };
        this.emit();
    }

    setPlaying(info: StreamInfo): void {
        this.chatMessages = [];
        this._state = {
            type: 'Playing',
            streamInfo: info,
            availableQualities: ['Auto'],
            selectedQuality: 'Auto',
            isChatVisible: true,
            isQualityPickerVisible: false,
            isMetadataVisible: true,
            chatMessages: [],
        };
        this.emit();
        this.scheduleMetadataHide();
    }

    setError(message: string): void {
        this._state = { type: 'Error', message };
        this.emit();
    }

    private playing(): PlayingState | null {
        return this._state.type === 'Playing' ? this._state : null;
    }

    private update(patch: Partial<PlayingState>): void {
        const s = this.playing();
        if (!s) return;
        this._state = { ...s, ...patch };
        this.emit();
    }

    setAvailableQualities(qualities: string[]): void {
        const s = this.playing();
        if (!s) return;
        const selected = qualities.includes(s.selectedQuality)
            ? s.selectedQuality
            : (qualities[0] ?? 'Auto');
        this.update({ availableQualities: qualities, selectedQuality: selected });
    }

    selectQuality(quality: string): void {
        this.update({ selectedQuality: quality, isQualityPickerVisible: false });
    }

    toggleChat(): void {
        const s = this.playing();
        if (!s) return;
        this.update({ isChatVisible: !s.isChatVisible });
    }

    toggleQualityPicker(): void {
        const s = this.playing();
        if (!s) return;
        this.update({ isQualityPickerVisible: !s.isQualityPickerVisible });
    }

    showMetadata(): void {
        this.update({ isMetadataVisible: true });
        this.scheduleMetadataHide();
    }

    private scheduleMetadataHide(): void {
        if (this.metadataHideTimer !== null) clearTimeout(this.metadataHideTimer);
        this.metadataHideTimer = setTimeout(() => {
            this.update({ isMetadataVisible: false });
        }, 4_000);
    }

    /** Add backlog messages (oldest-first from API). Appended to end. */
    addBacklogMessages(messages: ChatMessage[]): void {
        this.chatMessages.push(...messages);
        this.update({ chatMessages: [...this.chatMessages] });
    }

    /** Add a new live message. Appended at end (newest at bottom). */
    addNewMessage(msg: ChatMessage): void {
        this.chatMessages.push(msg);
        if (this.chatMessages.length > CHAT_CAP) this.chatMessages.shift();
        this.update({ chatMessages: [...this.chatMessages] });
    }
}
