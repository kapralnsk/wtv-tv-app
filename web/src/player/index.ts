import { AvPlayPlayer } from './avplay.ts';
import { HlsJsPlayer } from './hlsjs.ts';

export interface PlayerListener {
    onReady(): void;
    onError(e: unknown): void;
}

export interface VideoPlayer {
    open(url: string): Promise<void>;
    getQualities(): string[];
    /** @param index 0 = Auto, 1..n = specific quality in getQualities() order */
    selectQuality(index: number): void;
    setListener(cb: PlayerListener): void;
    destroy(): void;
}

export function createPlayer(container: HTMLElement): VideoPlayer {
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    if (typeof (window as any).webapis?.avplay !== 'undefined') {
        // Tizen: use native AVPlay (webapis.avplay calls are inside class methods only)
        return new AvPlayPlayer(container);
    }
    const videoEl = container.querySelector<HTMLVideoElement>('video')!;
    return new HlsJsPlayer(videoEl);
}
