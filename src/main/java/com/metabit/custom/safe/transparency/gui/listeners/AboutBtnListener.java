package com.metabit.custom.safe.transparency.gui.listeners;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import com.metabit.custom.safe.transparency.gui.views.MainView;

/**
 * Listener to open the about page
 */
public class AboutBtnListener implements ActionListener {

    private MainView mainView;

    public AboutBtnListener(MainView mainView) {
        this.mainView = mainView;
    }

    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        mainView.onAboutOpen();
    }
}
