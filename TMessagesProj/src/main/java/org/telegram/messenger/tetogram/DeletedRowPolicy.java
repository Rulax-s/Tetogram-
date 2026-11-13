/* Tetogram, 2026. GNU GPL v2 or later. */
package org.telegram.messenger.tetogram;

import java.util.function.IntSupplier;

/** A notification payload is not a RecyclerView row. Each fragment owns a fresh row instance. */
public final class DeletedRowPolicy {
    private DeletedRowPolicy() {}
    public interface Row<T> {
        int account(T value);
        long dialog(T value);
        int id(T value);
        int stableId(T value);
        void stableId(T value, int id);
        T clonePayload(T value);
        void copyStableParams(T value, T original);
    }
    public static <T> T replace(T original, T payload, Row<T> row, IntSupplier allocate) {
        if (original == null || payload == null || !MessageTimelinePolicy.sameMessage(
                row.account(original), row.dialog(original), row.id(original),
                row.account(payload), row.dialog(payload), row.id(payload))) return null;
        T result = row.clonePayload(payload);
        if (result == payload || result == original) throw new IllegalStateException("Shared deleted row");
        row.copyStableParams(result, original);
        row.stableId(result, row.stableId(original) == 0 ? allocate.getAsInt() : row.stableId(original));
        return result;
    }
}
