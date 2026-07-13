package com.scis.ui;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 * DocumentAdapter — convenience base class that implements all three
 * {@link DocumentListener} methods as no-ops, so subclasses only override
 * what they need.
 */
public abstract class DocumentAdapter implements DocumentListener {

    @Override public void insertUpdate(DocumentEvent e)  {}
    @Override public void removeUpdate(DocumentEvent e)  {}
    @Override public void changedUpdate(DocumentEvent e) {}
}
