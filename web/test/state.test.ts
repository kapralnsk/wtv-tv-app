import { describe, it, expect, vi, beforeEach } from 'vitest';
import { StateManager } from '../src/state.ts';
import type { StreamInfo, PlayingState } from '../src/state.ts';

vi.useFakeTimers();

function makeStreamInfo(overrides: Partial<StreamInfo> = {}): StreamInfo {
    return {
        channelId: 'ch-id',
        streamId: 'stream-id',
        playbackUrl: 'https://example.com/stream.m3u8',
        title: 'Test Stream',
        startedAtMs: 1_767_225_600_000,
        channelName: 'TestChannel',
        ...overrides,
    };
}

function playing(sm: StateManager): PlayingState {
    const s = sm.getState();
    if (s.type !== 'Playing') throw new Error(`Expected Playing, got ${s.type}`);
    return s;
}

describe('StateManager', () => {
    let sm: StateManager;

    beforeEach(() => {
        sm = new StateManager();
    });

    it('initial state is Loading', () => {
        expect(sm.getState().type).toBe('Loading');
    });

    it('setPlaying transitions to Playing with defaults', () => {
        sm.setPlaying(makeStreamInfo());
        const s = playing(sm);
        expect(s.availableQualities).toEqual(['Auto']);
        expect(s.selectedQuality).toBe('Auto');
        expect(s.isChatVisible).toBe(true);
        expect(s.isQualityPickerVisible).toBe(false);
        expect(s.isMetadataVisible).toBe(true);
        expect(s.chatMessages).toEqual([]);
    });

    it('setError transitions to Error', () => {
        sm.setError('oops');
        const s = sm.getState();
        expect(s.type).toBe('Error');
        if (s.type === 'Error') expect(s.message).toBe('oops');
    });

    it('setAvailableQualities retains selected when still present', () => {
        sm.setPlaying(makeStreamInfo());
        sm.setAvailableQualities(['Auto', '1080p60', '720p60']);
        sm.selectQuality('1080p60');
        sm.setAvailableQualities(['Auto', '1080p60', '480p30']);
        expect(playing(sm).selectedQuality).toBe('1080p60');
    });

    it('setAvailableQualities falls back to first when selected is gone', () => {
        sm.setPlaying(makeStreamInfo());
        sm.setAvailableQualities(['Auto', '1080p60']);
        sm.selectQuality('1080p60');
        sm.setAvailableQualities(['Auto', '720p60']);
        expect(playing(sm).selectedQuality).toBe('Auto');
    });

    it('selectQuality updates quality and closes picker', () => {
        sm.setPlaying(makeStreamInfo());
        sm.setAvailableQualities(['Auto', '1080p60', '720p60']);
        sm.toggleQualityPicker();
        sm.selectQuality('720p60');
        const s = playing(sm);
        expect(s.selectedQuality).toBe('720p60');
        expect(s.isQualityPickerVisible).toBe(false);
    });

    it('selectQuality is a no-op when not Playing', () => {
        sm.selectQuality('720p60');
        expect(sm.getState().type).toBe('Loading');
    });

    it('toggleChat flips isChatVisible', () => {
        sm.setPlaying(makeStreamInfo());
        sm.toggleChat();
        expect(playing(sm).isChatVisible).toBe(false);
        sm.toggleChat();
        expect(playing(sm).isChatVisible).toBe(true);
    });

    it('toggleQualityPicker flips isQualityPickerVisible', () => {
        sm.setPlaying(makeStreamInfo());
        sm.toggleQualityPicker();
        expect(playing(sm).isQualityPickerVisible).toBe(true);
        sm.toggleQualityPicker();
        expect(playing(sm).isQualityPickerVisible).toBe(false);
    });

    it('toggleQualityPicker is a no-op when not Playing', () => {
        sm.toggleQualityPicker();
        expect(sm.getState().type).toBe('Loading');
    });

    it('metadata auto-hides after 4 s', () => {
        sm.setPlaying(makeStreamInfo());
        expect(playing(sm).isMetadataVisible).toBe(true);
        vi.advanceTimersByTime(4_000);
        expect(playing(sm).isMetadataVisible).toBe(false);
    });

    it('showMetadata re-shows and restarts the 4 s timer', () => {
        sm.setPlaying(makeStreamInfo());
        vi.advanceTimersByTime(4_000);
        expect(playing(sm).isMetadataVisible).toBe(false);

        sm.showMetadata();
        expect(playing(sm).isMetadataVisible).toBe(true);
        vi.advanceTimersByTime(3_000);
        expect(playing(sm).isMetadataVisible).toBe(true);
        vi.advanceTimersByTime(1_000);
        expect(playing(sm).isMetadataVisible).toBe(false);
    });

    it('addBacklogMessages appends in order', () => {
        sm.setPlaying(makeStreamInfo());
        sm.addBacklogMessages([
            { id: 'a', nickname: 'Alice', content: 'first' },
            { id: 'b', nickname: 'Bob', content: 'second' },
        ]);
        const msgs = playing(sm).chatMessages;
        expect(msgs.map(m => m.id)).toEqual(['a', 'b']);
    });

    it('addNewMessage appends at end (newest at bottom)', () => {
        sm.setPlaying(makeStreamInfo());
        sm.addBacklogMessages([{ id: 'old', nickname: 'A', content: 'old' }]);
        sm.addNewMessage({ id: 'new', nickname: 'B', content: 'new' });
        const msgs = playing(sm).chatMessages;
        expect(msgs[0].id).toBe('old');
        expect(msgs[1].id).toBe('new');
    });

    it('addNewMessage enforces 200 message cap by removing oldest', () => {
        sm.setPlaying(makeStreamInfo());
        const backlog = Array.from({ length: 200 }, (_, i) => ({
            id: `m${i}`,
            nickname: 'U',
            content: `msg ${i}`,
        }));
        sm.addBacklogMessages(backlog);
        sm.addNewMessage({ id: 'overflow', nickname: 'V', content: 'overflow' });
        const msgs = playing(sm).chatMessages;
        expect(msgs.length).toBe(200);
        expect(msgs[0].id).toBe('m1');
        expect(msgs[199].id).toBe('overflow');
    });

    it('subscribe delivers state to listener', () => {
        const states: string[] = [];
        sm.subscribe(s => states.push(s.type));
        sm.setPlaying(makeStreamInfo());
        sm.setError('boom');
        expect(states).toEqual(['Playing', 'Error']);
    });

    it('unsubscribe stops delivery', () => {
        const calls: number[] = [];
        const unsub = sm.subscribe(() => calls.push(1));
        sm.setPlaying(makeStreamInfo());
        unsub();
        sm.setError('x');
        expect(calls.length).toBe(1);
    });
});
