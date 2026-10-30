import java.util.ArrayDeque;
import java.util.Deque;

/** Memento caretaker: stores snapshots without inspecting their contents. */
public final class HomeHistory {
    private static final int MAX_UNDO = 50;
    private final Deque<HomeModel.Snapshot> undo = new ArrayDeque<>();
    private HomeModel.Snapshot saved;

    public void remember(HomeModel.Snapshot snapshot) {
        undo.push(snapshot);
        if (undo.size() > MAX_UNDO) {
            undo.removeLast();
        }
    }

    public boolean undo(HomeModel model) {
        if (undo.isEmpty()) { return false; }
        model.restore(undo.peek());
        undo.pop();
        return true;
    }

    public void save(HomeModel model) {
        saved = model.save();
    }

    public boolean restoreSaved(HomeModel model) {
        if (saved == null) { return false; }
        HomeModel.Snapshot previous = model.save();
        model.restore(saved);
        remember(previous);
        return true;
    }
}
