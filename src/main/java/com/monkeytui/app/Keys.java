package com.monkeytui.app;

import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;

final class Keys {
    // Prevents instantiation of this key helper.
    private Keys() {
    }

    // Reports whether the key is the application quit shortcut.
    static boolean isQuit(KeyStroke key) {
        if (isCtrl(key, 'q')) return true;
        return key.getKeyType() == KeyType.Character && key.getCharacter() != null && key.getCharacter() == 17;
    }

    // Reports whether the key is a Ctrl+letter shortcut.
    static boolean isCtrl(KeyStroke key, char letter) {
        Character character = key.getCharacter();
        return key.isCtrlDown() && character != null && Character.toLowerCase(character) == letter;
    }

    // Reports whether the key is the requested character.
    static boolean isCharacter(KeyStroke key, char expected) {
        return key.getKeyType() == KeyType.Character && key.getCharacter() != null && key.getCharacter() == expected;
    }

    // Reports whether the key is an unmodified printable character.
    static boolean isPlainCharacter(KeyStroke key) {
        return key.getKeyType() == KeyType.Character && key.getCharacter() != null
                && !key.isCtrlDown() && !key.isAltDown();
    }

    // Reports whether the key moves the selection upward.
    static boolean isUp(KeyStroke key) {
        return key.getKeyType() == KeyType.ArrowUp || isCharacter(key, 'k') || isCharacter(key, 'K');
    }

    // Reports whether the key moves the selection downward.
    static boolean isDown(KeyStroke key) {
        return key.getKeyType() == KeyType.ArrowDown || isCharacter(key, 'j') || isCharacter(key, 'J');
    }
}
