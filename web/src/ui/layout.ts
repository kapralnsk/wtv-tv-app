import type { StateManager } from '../state.ts';

export function initLayout(state: StateManager): void {
    const statusOverlay = document.getElementById('status-overlay')!;
    const statusText = document.getElementById('status-text')!;
    const app = document.getElementById('app')!;
    const videoContainer = document.getElementById('video-container')!;
    const chatPanel = document.getElementById('chat-panel')!;

    state.subscribe(s => {
        switch (s.type) {
            case 'Loading':
                statusOverlay.removeAttribute('hidden');
                statusText.textContent = 'Loading…';
                app.setAttribute('hidden', '');
                break;

            case 'Error':
                statusOverlay.removeAttribute('hidden');
                statusText.textContent = s.message;
                app.setAttribute('hidden', '');
                break;

            case 'Playing':
                statusOverlay.setAttribute('hidden', '');
                app.removeAttribute('hidden');

                if (s.isChatVisible) {
                    chatPanel.removeAttribute('hidden');
                    videoContainer.style.flex = '7';
                } else {
                    chatPanel.setAttribute('hidden', '');
                    videoContainer.style.flex = '10';
                }
                break;
        }
    });
}
