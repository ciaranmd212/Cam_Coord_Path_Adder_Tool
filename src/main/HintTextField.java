//Custom TextField class. Used to hide password when it is entered.
package main;

import javax.swing.*;
import java.awt.*;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

public class HintTextField extends JTextField implements FocusListener {

    private final String hint;
    private boolean showingHint;

    public HintTextField(final String hint) {
        super(hint);
        this.hint = hint;
        this.showingHint = true;
        this.setForeground(Color.GRAY);
        this.addFocusListener(this);
        this.setFont(this.getFont().deriveFont(Font.ITALIC));
    }

    @Override
    public void focusGained(FocusEvent e) {
        if (this.getText().isEmpty()) {
            this.setText("");
            this.setForeground(Color.BLACK);
            showingHint = false;
        }
    }

    @Override
    public void focusLost(FocusEvent e) {
        if (this.getText().isEmpty()) {
            this.setText(hint);
            this.setForeground(Color.GRAY);
            showingHint = true;
        }
    }

    @Override
    public String getText() {
        return showingHint ? "" : super.getText();
    }
}
