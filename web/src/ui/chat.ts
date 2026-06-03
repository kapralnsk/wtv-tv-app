import type { ChatMessage } from '../chat.ts';
import type { StateManager } from '../state.ts';

// 12-colour palette matching PlayerScreen.kt
const CHAT_COLORS = [
    '#FF4500', '#2E8B57', '#1E90FF', '#DAA520',
    '#FF69B4', '#9ACD32', '#D2691E', '#5F9EA0',
    '#B22222', '#00FA9A', '#9B59B6', '#FF7F50',
];

/** Java-compatible String.hashCode() using 32-bit signed integer arithmetic. */
export function javaHashCode(str: string): number {
    let hash = 0;
    for (let i = 0; i < str.length; i++) {
        hash = (Math.imul(31, hash) + str.charCodeAt(i)) | 0;
    }
    return hash;
}

export function nicknameColor(nickname: string): string {
    const idx = (javaHashCode(nickname) & 0x7fffffff) % CHAT_COLORS.length;
    return CHAT_COLORS[idx];
}

function renderMessage(msg: ChatMessage): HTMLLIElement {
    const li = document.createElement('li');
    const nick = document.createElement('strong');
    nick.style.color = nicknameColor(msg.nickname);
    nick.textContent = msg.nickname;
    li.appendChild(nick);
    li.appendChild(document.createTextNode(': ' + msg.content));
    return li;
}

export function initChat(state: StateManager): void {
    const list = document.getElementById('chat-messages')!;
    // Tracks which message IDs are currently in the DOM.
    const rendered = new Map<string, HTMLLIElement>();

    state.subscribe(s => {
        if (s.type !== 'Playing') return;

        const messages = s.chatMessages;
        const currentIds = new Set(messages.map(m => m.id));

        // Remove messages that were trimmed from the list (oldest evicted at cap)
        for (const [id, el] of rendered) {
            if (!currentIds.has(id)) {
                el.remove();
                rendered.delete(id);
            }
        }

        // Append messages not yet in the DOM (in order, so newest lands at bottom)
        for (const msg of messages) {
            if (!rendered.has(msg.id)) {
                const li = renderMessage(msg);
                rendered.set(msg.id, li);
                list.appendChild(li);
            }
        }

        // Auto-scroll to bottom so newest message is visible
        list.scrollTop = list.scrollHeight;
    });
}
