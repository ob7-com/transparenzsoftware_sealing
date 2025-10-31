package com.metabit.custom.safe.transparency.gui.listeners;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import com.metabit.custom.safe.transparency.gui.views.MainView;

/**
 * Listener to close the whole gui and app.
 */
public class CloseAppListener implements ActionListener {

    private MainView mainView;

    public CloseAppListener(MainView mainView) {
        this.mainView = mainView;
    }

    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        mainView.onClose();
    }
}
