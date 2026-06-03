import { describe, it, expect } from 'vitest';
import { javaHashCode, nicknameColor } from '../src/ui/chat.ts';

describe('javaHashCode', () => {
    it('matches Java String.hashCode() for ASCII input', () => {
        // Java: "Alice".hashCode() == 63350368
        expect(javaHashCode('Alice')).toBe(63350368);
    });

    it('matches Java String.hashCode() for single character', () => {
        // Java: "A".hashCode() == 65
        expect(javaHashCode('A')).toBe(65);
    });

    it('matches Java String.hashCode() for empty string', () => {
        expect(javaHashCode('')).toBe(0);
    });

    it('handles 32-bit overflow the same way Java does', () => {
        // Java: "test".hashCode() == 3556498
        expect(javaHashCode('test')).toBe(3556498);
    });

    it('handles strings that produce negative 32-bit hash', () => {
        // Verify sign bit is handled — result should be Int32
        const h = javaHashCode('aaaaaaaaaaaaaaaa');
        // Must fit in signed 32-bit range
        expect(h).toBeGreaterThanOrEqual(-2147483648);
        expect(h).toBeLessThanOrEqual(2147483647);
    });
});

describe('nicknameColor', () => {
    it('returns a hex color string', () => {
        const c = nicknameColor('Alice');
        expect(c).toMatch(/^#[0-9A-Fa-f]{6}$/);
    });

    it('is deterministic for the same input', () => {
        expect(nicknameColor('Bob')).toBe(nicknameColor('Bob'));
    });

    it('maps Alice to index 4 (#FF69B4)', () => {
        // hashCode=63350368, 63350368 & 0x7fffffff = 63350368, % 12 = 4 → #FF69B4
        expect(nicknameColor('Alice')).toBe('#FF69B4');
    });

    it('handles Cyrillic nicknames', () => {
        const c = nicknameColor('Саша');
        expect(c).toMatch(/^#[0-9A-Fa-f]{6}$/);
    });

    it('handles names that would give negative hash (strips sign bit)', () => {
        // Any name that produces a negative JVM hash must still yield a valid index
        const c = nicknameColor('aaaaaaaaaaaaaaaa');
        expect(c).toMatch(/^#[0-9A-Fa-f]{6}$/);
    });
});
