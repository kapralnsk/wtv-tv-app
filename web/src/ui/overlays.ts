import type { StateManager } from '../state.ts';

function formatUptime(startedAtMs: number): string {
    const totalSecs = Math.floor((Date.now() - startedAtMs) / 1000);
    const h = Math.floor(totalSecs / 3600);
    const m = Math.floor((totalSecs % 3600) / 60);
    const s = totalSecs % 60;
    return h > 0
        ? `${h}:${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`
        : `${m}:${String(s).padStart(2, '0')}`;
}

export function initOverlays(
    state: StateManager,
    callbacks: {
        onQualityPickerToggle(): void;
        onQualitySelect(index: number): void;
    },
): void {
    const infoBar = document.getElementById('stream-info-bar')!;
    const picker = document.getElementById('quality-picker')!;

    let uptimeTimer: ReturnType<typeof setInterval> | null = null;
    let infoBarRenderedForStream = '';

    function stopUptime(): void {
        if (uptimeTimer !== null) {
            clearInterval(uptimeTimer);
            uptimeTimer = null;
        }
    }

    state.subscribe(s => {
        if (s.type !== 'Playing') {
            infoBar.setAttribute('hidden', '');
            picker.setAttribute('hidden', '');
            stopUptime();
            infoBarRenderedForStream = '';
            return;
        }

        // ── Stream info bar ──────────────────────────────────────────────
        if (s.isMetadataVisible) {
            infoBar.removeAttribute('hidden');

            // Only rebuild DOM when stream changes or quality label changes.
            const key = `${s.streamInfo.streamId}:${s.selectedQuality}`;
            if (infoBarRenderedForStream !== key) {
                infoBarRenderedForStream = key;
                stopUptime();

                infoBar.innerHTML = '';

                const nameEl = document.createElement('div');
                nameEl.className = 'info-channel-name';
                nameEl.textContent = s.streamInfo.channelName;
                infoBar.appendChild(nameEl);

                if (s.streamInfo.title) {
                    const titleEl = document.createElement('div');
                    titleEl.className = 'info-stream-title';
                    titleEl.textContent = s.streamInfo.title;
                    infoBar.appendChild(titleEl);
                }

                const metaEl = document.createElement('div');
                metaEl.className = 'info-meta';

                const liveEl = document.createElement('span');
                liveEl.className = 'info-live';
                liveEl.textContent = `● LIVE ${formatUptime(s.streamInfo.startedAtMs)}`;
                metaEl.appendChild(liveEl);

                const qualityBtn = document.createElement('button');
                qualityBtn.className = 'quality-btn';
                qualityBtn.textContent = s.selectedQuality;
                qualityBtn.addEventListener('click', callbacks.onQualityPickerToggle);
                metaEl.appendChild(qualityBtn);

                infoBar.appendChild(metaEl);

                uptimeTimer = setInterval(() => {
                    liveEl.textContent = `● LIVE ${formatUptime(s.streamInfo.startedAtMs)}`;
                }, 1_000);
            }
        } else {
            infoBar.setAttribute('hidden', '');
            stopUptime();
            infoBarRenderedForStream = '';
        }

        // ── Quality picker ───────────────────────────────────────────────
        if (s.isQualityPickerVisible) {
            picker.removeAttribute('hidden');
            picker.innerHTML = '';

            s.availableQualities.forEach((q, i) => {
                const item = document.createElement('div');
                item.className = 'quality-item';
                item.dataset['index'] = String(i);
                item.dataset['selected'] = q === s.selectedQuality ? 'true' : 'false';
                item.textContent = q;
                item.addEventListener('click', () => callbacks.onQualitySelect(i));
                picker.appendChild(item);
            });

            // Initial D-pad focus: highlight the currently playing quality
            const selectedEl = picker.querySelector<HTMLElement>('[data-selected="true"]');
            selectedEl?.classList.add('focused');
        } else {
            picker.setAttribute('hidden', '');
        }
    });
}
