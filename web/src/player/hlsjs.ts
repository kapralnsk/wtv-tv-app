import Hls from 'hls.js';
import type { PlayerListener, VideoPlayer } from './index.ts';

export class HlsJsPlayer implements VideoPlayer {
    private hls: Hls | null = null;
    private listener: PlayerListener | null = null;
    private qualities: string[] = ['Auto'];

    constructor(private readonly videoEl: HTMLVideoElement) {}

    async open(url: string): Promise<void> {
        this.hls?.destroy();

        if (!Hls.isSupported()) {
            // Safari / native HLS fallback
            this.videoEl.src = url;
            await this.videoEl.play().catch(() => {});
            this.qualities = ['Auto'];
            this.listener?.onReady();
            return;
        }

        return new Promise<void>((resolve, reject) => {
            const hls = new Hls();
            this.hls = hls;

            hls.on(Hls.Events.MANIFEST_PARSED, (_, data) => {
                const labels = data.levels
                    .map(l => `${l.height}p`)
                    .filter(l => l !== '0p');
                this.qualities = ['Auto', ...labels];
                this.listener?.onReady();
                this.videoEl.play().catch(() => {});
                resolve();
            });

            hls.on(Hls.Events.ERROR, (_, data) => {
                if (data.fatal) {
                    const err = new Error(`HLS ${data.type}: ${data.details}`);
                    this.listener?.onError(err);
                    reject(err);
                }
            });

            hls.loadSource(url);
            hls.attachMedia(this.videoEl);
        });
    }

    getQualities(): string[] {
        return this.qualities;
    }

    selectQuality(index: number): void {
        if (!this.hls) return;
        // index 0 = Auto (-1 in hls.js), index n = levels[n-1]
        this.hls.currentLevel = index === 0 ? -1 : index - 1;
    }

    setListener(cb: PlayerListener): void {
        this.listener = cb;
    }

    destroy(): void {
        this.hls?.destroy();
        this.hls = null;
    }
}
