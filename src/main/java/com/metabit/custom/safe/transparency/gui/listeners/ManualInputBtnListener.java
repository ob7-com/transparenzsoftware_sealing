package com.metabit.custom.safe.transparency.gui.listeners;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.metabit.custom.safe.transparency.gui.views.DataInputDialog;
import com.metabit.custom.safe.transparency.gui.views.MainView;
import com.metabit.custom.safe.transparency.i18n.Translator;

import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Listener which is called when user presses "Paste"
 */
public class ManualInputBtnListener implements ActionListener {

    private final MainView mainView;
    
    public ManualInputBtnListener(MainView mainView) {
        this.mainView = mainView;
    }

    @Override
    public void actionPerformed(ActionEvent event) {
    	DataInputDialog d = new DataInputDialog(mainView);
    }
}
