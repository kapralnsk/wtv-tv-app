import type { StateManager } from './state.ts';

// Key codes used across platforms
const KEY = {
    ENTER: 13,
    UP: 38,
    DOWN: 40,
    LEFT: 37,
    RIGHT: 39,
    BACK_TIZEN: 10009,
    BACK_WEBOS: 461,
} as const;

let qualityFocusIdx = 0;

function setQualityFocus(idx: number): void {
    const items = document.querySelectorAll<HTMLElement>('#quality-picker .quality-item');
    items.forEach((el, i) => el.classList.toggle('focused', i === idx));
}

export function initNav(
    state: StateManager,
    callbacks: { onQualitySelect(index: number): void },
): void {
    // Register Tizen remote keys so they fire keydown events
    try {
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        const tizen = (window as any).tizen;
        if (tizen?.tvinputdevice) {
            tizen.tvinputdevice.registerKey('Back');
            tizen.tvinputdevice.registerKey('MediaPlayPause');
        }
    } catch { /* non-Tizen platform */ }

    // Reset quality focus index when the picker opens
    let prevPickerVisible = false;
    state.subscribe(s => {
        if (s.type !== 'Playing') return;
        if (s.isQualityPickerVisible && !prevPickerVisible) {
            qualityFocusIdx = Math.max(
                0,
                s.availableQualities.indexOf(s.selectedQuality),
            );
            // Let overlays.ts render first, then set focus class
            setTimeout(() => setQualityFocus(qualityFocusIdx), 0);
        }
        prevPickerVisible = s.isQualityPickerVisible;
    });

    document.addEventListener('keydown', (e: KeyboardEvent) => {
        const s = state.getState();
        if (s.type !== 'Playing') return;

        if (s.isQualityPickerVisible) {
            switch (e.keyCode) {
                case KEY.UP:
                    qualityFocusIdx = Math.max(0, qualityFocusIdx - 1);
                    setQualityFocus(qualityFocusIdx);
                    e.preventDefault();
                    break;
                case KEY.DOWN:
                    qualityFocusIdx = Math.min(
                        s.availableQualities.length - 1,
                        qualityFocusIdx + 1,
                    );
                    setQualityFocus(qualityFocusIdx);
                    e.preventDefault();
                    break;
                case KEY.ENTER:
                    callbacks.onQualitySelect(qualityFocusIdx);
                    e.preventDefault();
                    break;
                case KEY.BACK_TIZEN:
                case KEY.BACK_WEBOS:
                    state.toggleQualityPicker();
                    e.preventDefault();
                    break;
            }
            return;
        }

        switch (e.keyCode) {
            case KEY.ENTER:
                if (s.isMetadataVisible) {
                    state.toggleQualityPicker();
                } else {
                    state.showMetadata();
                }
                e.preventDefault();
                break;
        }
    });
}
