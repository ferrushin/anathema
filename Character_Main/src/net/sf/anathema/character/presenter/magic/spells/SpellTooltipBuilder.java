package net.sf.anathema.character.presenter.magic.spells;

import net.sf.anathema.character.generic.framework.magic.stringbuilder.MagicDescriptionStringBuilder;
import net.sf.anathema.character.generic.framework.magic.stringbuilder.ScreenDisplayInfoStringBuilder;
import net.sf.anathema.character.generic.framework.magic.stringbuilder.source.MagicSourceStringBuilder;
import net.sf.anathema.character.generic.magic.ISpell;
import net.sf.anathema.character.generic.magic.description.MagicDescriptionProvider;
import net.sf.anathema.lib.gui.TooltipBuilder;
import net.sf.anathema.lib.resources.IResources;

public class SpellTooltipBuilder {

  private final IResources resources;
  private final SpellViewProperties properties;
  private MagicDescriptionProvider magicDescriptionProvider;

  public SpellTooltipBuilder(IResources resources, SpellViewProperties properties, MagicDescriptionProvider magicDescriptionProvider) {
    this.resources = resources;
    this.properties = properties;
    this.magicDescriptionProvider = magicDescriptionProvider;
  }

  public String createTooltip(ISpell spell) {
    TooltipBuilder builder = new TooltipBuilder();
    builder.appendTitleLine(resources.getString(spell.getId()));
    builder.appendLine(properties.getCircleLabel(), getCircleValue(spell));
    builder.appendLine(getCostLabel(), getCostValue(spell));
    builder.appendLine(getTargetLabel(), getTargetValue(spell));
    builder.appendLine(getSourceLabel(), getSourceValue(spell));
    builder.appendParagraphs(getDescriptionParagraphs(spell));
    return builder.build();
  }

  private String[] getDescriptionParagraphs(ISpell spell) {
    return new MagicDescriptionStringBuilder(resources, magicDescriptionProvider).getDisplayParagraphs(spell);
  }

  private String getCircleValue(ISpell spell) {
    return resources.getString(spell.getCircleType().getId());
  }

  private String getCostValue(ISpell spell) {
    return new ScreenDisplayInfoStringBuilder(resources).createCostString(spell);
  }

  private String getTargetValue(ISpell spell) {
    if (spell.getTarget() == null) {
      return getUndefinedString();
    }
    return resources.getString("Spells.Target." + spell.getTarget());
  }

  private String getSourceValue(ISpell spell) {
    return new MagicSourceStringBuilder<ISpell>(resources).createSourceString(spell);
  }

  public String getUndefinedString() {
    return resources.getString("CardView.CharmConfiguration.Spells.Target.Undefined"); //$NON-NLS-1$
  }

  private String getTargetLabel() {
    return resources.getString("CardView.CharmConfiguration.Spells.Target"); //$NON-NLS-1$
  }

  private String getSourceLabel() {
    return resources.getString("CardView.CharmConfiguration.Spells.Source"); //$NON-NLS-1$
  }

  private String getCostLabel() {
    return resources.getString("CardView.CharmConfiguration.Spells.Cost"); //$NON-NLS-1$
  }
}
