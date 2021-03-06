package net.sf.anathema.character.library.trait.view;

import net.miginfocom.layout.CC;
import net.miginfocom.swing.MigLayout;
import net.sf.anathema.character.library.intvalue.IIconToggleButtonProperties;
import net.sf.anathema.framework.value.IconToggleButton;
import net.sf.anathema.lib.control.IBooleanValueChangedListener;

import javax.swing.JPanel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import static net.sf.anathema.lib.gui.layout.LayoutUtils.fillWithoutInsets;

public abstract class AbstractToggleButtonTraitViewWrapper<K extends ITraitView< ? >> extends
    AbstractTraitViewWrapper<K> {

  private final IconToggleButton button;
  private final IIconToggleButtonProperties properties;
  private JPanel traitViewPanel;
  private JPanel innerViewPanel;

  public AbstractToggleButtonTraitViewWrapper(K innerView, IIconToggleButtonProperties properties, boolean selected) {
    super(innerView);
    this.properties = properties;
    this.button = new IconToggleButton(properties.createStandardIcon(), properties.createUnselectedIcon());
    setButtonState(selected, true);
    button.setToolTipText(properties.getToolTipText());
  }

  public void setButtonState(boolean selected, boolean enabled) {
    button.setIconSet(properties.createStandardIcon(), properties.createUnselectedIcon());
    button.setEnabled(enabled);
    button.setSelected(selected);
  }

  @Override
  public void addComponents(JPanel viewPanel) {
    this.traitViewPanel = viewPanel;
  }

  @Override
  public void delete() {
    getInnerView().delete();
    traitViewPanel.remove(innerViewPanel);
    traitViewPanel.remove(button.getComponent());
    traitViewPanel.revalidate();
  }

  public void addButtonSelectedListener(final IBooleanValueChangedListener listener) {
    button.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        listener.valueChanged(!button.isSelected());
      }
    });
  }

  protected IconToggleButton getButton() {
    return button;
  }

  protected void addInnerView(JPanel panel) {
    this.innerViewPanel = new JPanel(new MigLayout(fillWithoutInsets().wrapAfter(2)));
    getInnerView().addComponents(innerViewPanel);
    panel.add(innerViewPanel, new CC().growX().pushX());
  }
}