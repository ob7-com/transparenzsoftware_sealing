package com.metabit.custom.safe.transparency.gui.listeners;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import com.metabit.custom.safe.transparency.gui.views.MainView;

/**
 * Listener which catches changes on a select box and will
 * perform a resetting of the application state.
 */
public class SelectBoxChangedListener implements ActionListener {

    private MainView mainView;

    public SelectBoxChangedListener(MainView mainView) {
        this.mainView = mainView;
    }

    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        mainView.clearErrorMessages();
        mainView.setEnableVerifyButton(true);
    }
}
