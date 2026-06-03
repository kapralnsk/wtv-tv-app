import type { PlayerListener, VideoPlayer } from './index.ts';

// Tizen webapis global — only available at runtime on Tizen devices/emulator.
declare const webapis: {
    avplay: {
        open(url: string): void;
        prepareAsync(onSuccess: () => void, onError: (err: unknown) => void): void;
        play(): void;
        stop(): void;
        close(): void;
        getTotalTrackInfo(): Array<{ type: string; index: number; extra_info: string }>;
        setCurrentTrack(type: string, index: number): void;
        setListener(listener: {
            onStreamInfoReady(): void;
            onError(type: string): void;
            onBufferingComplete(): void;
            onEvent(eventType: string, eventData: string): void;
        }): void;
    };
};

export class AvPlayPlayer implements VideoPlayer {
    private listener: PlayerListener | null = null;
    private qualities: string[] = ['Auto'];

    constructor(container: HTMLElement) {
        // Make the AVPlay object element visible (hidden by default via CSS)
        const obj = container.querySelector<HTMLElement>('object');
        if (obj) obj.style.display = 'block';
    }

    open(url: string): Promise<void> {
        return new Promise((resolve, reject) => {
            try {
                webapis.avplay.open(url);
            } catch (e) {
                reject(e);
                return;
            }

            webapis.avplay.setListener({
                onStreamInfoReady: () => {
                    try {
                        const tracks = webapis.avplay.getTotalTrackInfo();
                        const videoTracks = tracks.filter(t => t.type === 'VIDEO');
                        const labels = videoTracks.map(t => {
                            try {
                                const info = JSON.parse(t.extra_info) as Record<string, unknown>;
                                const h = info['Height'] ?? info['height'] ?? 0;
                                return `${h}p`;
                            } catch {
                                return `Track ${t.index}`;
                            }
                        });
                        this.qualities = ['Auto', ...labels];
                    } catch {
                        this.qualities = ['Auto'];
                    }
                    this.listener?.onReady();
                    resolve();
                },
                onError: (type: string) => {
                    const err = new Error(`AVPlay error: ${type}`);
                    this.listener?.onError(err);
                    reject(err);
                },
                onBufferingComplete: () => {},
                onEvent: () => {},
            });

            webapis.avplay.prepareAsync(
                () => { webapis.avplay.play(); },
                (err: unknown) => {
                    this.listener?.onError(err);
                    reject(err);
                },
            );
        });
    }

    getQualities(): string[] {
        return this.qualities;
    }

    selectQuality(index: number): void {
        if (index === 0) return; // Auto — let AVPlay ABR decide
        try {
            webapis.avplay.setCurrentTrack('VIDEO', index - 1);
        } catch {
            // non-fatal: track may not be available
        }
    }

    setListener(cb: PlayerListener): void {
        this.listener = cb;
    }

    destroy(): void {
        try {
            webapis.avplay.stop();
            webapis.avplay.close();
        } catch {
            // ignore errors on destroy
        }
    }
}
