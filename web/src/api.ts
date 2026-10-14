const DEFAULT_HEADERS: HeadersInit = {
    'User-Agent': 'Mozilla/5.0 (Linux; Android 12; Chromecast with Google TV) AppleWebKit/537.36',
    'Origin': 'https://w.tv',
    'Content-Type': 'application/json',
};

async function apiFetch<T>(url: string, init?: RequestInit): Promise<T> {
    const res = await fetch(url, {
        ...init,
        headers: { ...DEFAULT_HEADERS, ...init?.headers },
    });
    if (!res.ok) throw new Error(`HTTP ${res.status} ${res.statusText} — ${url}`);
    return res.json() as Promise<T>;
}

export interface ProfileData {
    userId: string;
    nickname: string;
}

export interface LiveStreamData {
    streamId: string;
    title: string;
    startedAt: string;
    playbackUrl: string;
    viewers: number;
}

export interface ChannelDetailData {
    channelId: string;
    name: string;
    live: boolean;
    liveStream?: LiveStreamData;
}

export interface BacklogMessage {
    messageId: string;
    type: string;
    content: string;
    sender: { userId: string; nickname: string };
}

export async function getProfile(slug: string): Promise<ProfileData> {
    const data = await apiFetch<{ profile: ProfileData }>(
        `https://profiles-service.w.tv/api/v1/profiles/by-nickname/${encodeURIComponent(slug)}`,
    );
    return data.profile;
}

export async function getChannel(channelId: string): Promise<ChannelDetailData> {
    const data = await apiFetch<{ channel: ChannelDetailData }>(
        `https://streams-search-service.w.tv/api/v1/channels/${encodeURIComponent(channelId)}`,
    );
    return data.channel;
}

export async function joinStream(streamId: string): Promise<void> {
    await apiFetch<unknown>(
        `https://streams-service.w.tv/api/v1/streams/${encodeURIComponent(streamId)}/join`,
        { method: 'POST', body: '{}' },
    );
}

export async function joinChat(chatId: string): Promise<string> {
    const data = await apiFetch<{ token: string }>(
        `https://chats-service.w.tv/api/v1/chats/${encodeURIComponent(chatId)}/join`,
        { method: 'POST', body: '{}' },
    );
    return data.token;
}

export async function getChatBacklog(chatId: string): Promise<BacklogMessage[]> {
    const data = await apiFetch<{ messages: BacklogMessage[] }>(
        `https://chats-service.w.tv/api/v1/chats/${encodeURIComponent(chatId)}/messages`,
    );
    return data.messages;
}
