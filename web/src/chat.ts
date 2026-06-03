const IVS_CHAT_URL = 'wss://edge.ivschat.eu-central-1.amazonaws.com/';

export interface ChatMessage {
    id: string;
    nickname: string;
    content: string;
}

/**
 * Port of ChatWebSocket.kt:parseIvsFrame.
 * IVS live frames wrap a JSON-encoded string in Attributes.data.
 */
export function parseIvsFrame(text: string): ChatMessage | null {
    try {
        const obj = JSON.parse(text) as Record<string, unknown>;
        if (obj['Type'] !== 'EVENT') return null;
        if (obj['EventName'] !== 'MESSAGE') return null;

        const attrs = obj['Attributes'] as Record<string, unknown> | undefined;
        if (!attrs) return null;
        const dataStr = attrs['data'];
        if (typeof dataStr !== 'string') return null;

        const inner = JSON.parse(dataStr) as Record<string, unknown>;
        const sender = inner['sender'] as Record<string, unknown> | undefined;
        if (!sender) return null;

        const id = inner['messageId'];
        const nickname = sender['nickname'];
        const content = inner['content'];

        if (typeof id !== 'string' || !id) return null;
        if (typeof nickname !== 'string' || !nickname) return null;
        if (typeof content !== 'string') return null;

        return { id, nickname, content };
    } catch {
        return null;
    }
}

export class ChatWebSocket {
    private ws: WebSocket | null = null;
    private pingTimer: ReturnType<typeof setInterval> | null = null;

    constructor(
        private readonly token: string,
        private readonly onMessage: (msg: ChatMessage) => void,
        private readonly onError: (e: Event) => void,
    ) {}

    connect(): void {
        // IVS auth: token sent as Sec-WebSocket-Protocol during the HTTP upgrade.
        this.ws = new WebSocket(IVS_CHAT_URL, [this.token]);

        this.ws.onmessage = (e: MessageEvent<string>) => {
            const msg = parseIvsFrame(e.data);
            if (msg) this.onMessage(msg);
        };

        this.ws.onerror = this.onError;

        // 30 s keepalive
        this.pingTimer = setInterval(() => {
            if (this.ws?.readyState === WebSocket.OPEN) {
                this.ws.send('{}');
            }
        }, 30_000);
    }

    close(): void {
        if (this.pingTimer !== null) {
            clearInterval(this.pingTimer);
            this.pingTimer = null;
        }
        this.ws?.close(1000);
        this.ws = null;
    }
}
