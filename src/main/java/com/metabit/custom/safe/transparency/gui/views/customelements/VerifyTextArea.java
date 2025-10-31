package com.metabit.custom.safe.transparency.gui.views.customelements;

import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.*;

import com.metabit.custom.safe.transparency.gui.listeners.TextareaChangedListeners;
import com.metabit.custom.safe.transparency.gui.views.MainView;

public class VerifyTextArea extends JTextArea {

	public VerifyTextArea(MainView mainView, AtomicBoolean eventsEnabled) {
        this.setLineWrap(true);
        this.setWrapStyleWord(true);
        this.getDocument().addDocumentListener(new TextareaChangedListeners(mainView, eventsEnabled));
    }
}
