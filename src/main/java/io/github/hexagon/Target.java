package io.github.hexagon;

public interface Target {

    static Target loc(int locId) {
        return null;
    }

    static Target npc(int... npcIds) {
        return null;
    }

    static Target coordGrid(int x, int y, int floor) {
        return null;
    }
}
