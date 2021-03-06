package net.sf.anathema.character.equipment.character;

import net.sf.anathema.character.equipment.item.model.IEquipmentTemplateProvider;
import net.sf.anathema.character.equipment.template.IEquipmentTemplate;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JComponent;
import javax.swing.JList;
import java.awt.Component;

public class EquipmentObjectCellRenderer extends DefaultListCellRenderer {

	private final IEquipmentTemplateProvider templateProvider;
	private final EquipmentTemplateTooltipBuilder tooltipBuilder;
	
	public EquipmentObjectCellRenderer(IEquipmentTemplateProvider provider) {
		templateProvider = provider;
		tooltipBuilder = new EquipmentTemplateTooltipBuilder();
	}
	
@Override
  public Component getListCellRendererComponent(
      JList list,
      Object value,
      int index,
      boolean isSelected,
      boolean cellHasFocus) {
	 
    String templateId = (String) value;
    JComponent component = (JComponent) super.getListCellRendererComponent(list, templateId, index, isSelected, cellHasFocus);
	IEquipmentTemplate template = templateProvider.loadTemplate(templateId);
	if (template != null) {
		component.setToolTipText(tooltipBuilder.getTooltipDescription(template));
	}
    return component;
  }

  @Override
  public boolean equals(Object obj) {
    return obj instanceof EquipmentObjectCellRenderer;
  }

  @Override
  public int hashCode() {
    return 1;
  }
}