/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.faforever.client.fxml.infrastructure;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.logging.Logger;

/**
 * @author Ciprian
 */
public class Utf8String {

    private final static Logger logger;
    public static Utf8String Empty = new Utf8String("");

    static {
        logger = Logger.getLogger(Utf8String.class.getName());
    }

    final int _hash;
    byte[] _textData;

    public Utf8String(String text) {
        byte[] textData;
        textData = text.getBytes(StandardCharsets.UTF_8);
        _textData = textData;
        _hash = text.hashCode();
    }

    @Override
    public String toString() {
        return new String(_textData, StandardCharsets.UTF_8);
    }

    @Override
    public boolean equals(Object other) {
        if (other == null) {
            return false;
        }
        if (this == other) {
            return true;
        }
        if (!(other instanceof Utf8String)) {
            return false;
        }
        Utf8String otherText = (Utf8String) other;
        if (_textData == otherText._textData) {
            return true;
        }
        if (_hash != otherText._hash) {
            return false;
        }
        boolean result = Arrays.equals(_textData, otherText._textData);
        if (result) {
            otherText._textData = _textData;
        }
        return result;
    }

    @Override
    public int hashCode() {
        return _hash;
    }

}
