import { describe, it, expect } from 'vitest';
import { parseIvsFrame } from '../src/chat.ts';

// IVS frames wrap a JSON-encoded string inside Attributes.data
function makeFrame({
    type = 'EVENT',
    eventName = 'MESSAGE',
    messageId = 'msg-1',
    content = 'hello',
    senderUserId = 'user-1',
    senderNickname = 'Alice',
}: {
    type?: string;
    eventName?: string;
    messageId?: string;
    content?: string;
    senderUserId?: string;
    senderNickname?: string;
} = {}): string {
    const data = JSON.stringify({
        messageId,
        content,
        sender: { userId: senderUserId, nickname: senderNickname, tags: [] },
    });
    return JSON.stringify({
        Type: type,
        EventName: eventName,
        Attributes: { data },
        SendTime: '2026-01-01T00:00:00Z',
    });
}

describe('parseIvsFrame', () => {
    it('returns ChatMessage for a valid MESSAGE event', () => {
        expect(parseIvsFrame(makeFrame())).toEqual({
            id: 'msg-1',
            nickname: 'Alice',
            content: 'hello',
        });
    });

    it('returns null when Type is not EVENT', () => {
        expect(parseIvsFrame(makeFrame({ type: 'UNKNOWN' }))).toBeNull();
    });

    it('returns null when EventName is not MESSAGE', () => {
        expect(parseIvsFrame(makeFrame({ eventName: 'VIEWER_JOIN' }))).toBeNull();
    });

    it('returns null when Attributes.data is missing', () => {
        const frame = JSON.stringify({ Type: 'EVENT', EventName: 'MESSAGE', Attributes: {} });
        expect(parseIvsFrame(frame)).toBeNull();
    });

    it('returns null when messageId is missing from data', () => {
        const data = JSON.stringify({ content: 'hi', sender: { userId: 'u1', nickname: 'Bob' } });
        const frame = JSON.stringify({ Type: 'EVENT', EventName: 'MESSAGE', Attributes: { data } });
        expect(parseIvsFrame(frame)).toBeNull();
    });

    it('returns null when sender is missing from data', () => {
        const data = JSON.stringify({ messageId: 'm1', content: 'hi' });
        const frame = JSON.stringify({ Type: 'EVENT', EventName: 'MESSAGE', Attributes: { data } });
        expect(parseIvsFrame(frame)).toBeNull();
    });

    it('returns null when nickname is missing from sender', () => {
        const data = JSON.stringify({ messageId: 'm1', content: 'hi', sender: { userId: 'u1' } });
        const frame = JSON.stringify({ Type: 'EVENT', EventName: 'MESSAGE', Attributes: { data } });
        expect(parseIvsFrame(frame)).toBeNull();
    });

    it('returns null for invalid JSON', () => {
        expect(parseIvsFrame('not-json')).toBeNull();
    });

    it('returns null for empty string', () => {
        expect(parseIvsFrame('')).toBeNull();
    });

    it('preserves content with special characters', () => {
        const result = parseIvsFrame(makeFrame({ content: 'PogChamp LUL Kappa' }));
        expect(result?.content).toBe('PogChamp LUL Kappa');
    });

    it('preserves Cyrillic content', () => {
        const result = parseIvsFrame(makeFrame({ content: 'вау' }));
        expect(result?.content).toBe('вау');
    });
});
