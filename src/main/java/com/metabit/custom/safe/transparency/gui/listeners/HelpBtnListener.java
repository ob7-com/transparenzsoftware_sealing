package com.metabit.custom.safe.transparency.gui.listeners;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import com.metabit.custom.safe.transparency.gui.views.MainView;

/**
 * Help button listener, which opens the help view
 */
public class HelpBtnListener implements ActionListener {

    private MainView mainView;

    public HelpBtnListener(MainView mainView) {
        this.mainView = mainView;
    }

    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        mainView.onHelpOpen();
    }
}
