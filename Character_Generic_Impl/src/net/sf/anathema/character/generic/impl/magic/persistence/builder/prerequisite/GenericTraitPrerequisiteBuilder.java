package net.sf.anathema.character.generic.impl.magic.persistence.builder.prerequisite;

import static net.sf.anathema.character.generic.impl.magic.ICharmXMLConstants.ATTRIB_VALUE;

import net.sf.anathema.character.generic.traits.IGenericTrait;
import net.sf.anathema.character.generic.traits.ITraitType;
import net.sf.anathema.character.generic.traits.types.ValuedTraitType;
import net.sf.anathema.lib.exception.PersistenceException;
import net.sf.anathema.lib.xml.ElementUtilities;

import org.dom4j.Element;

public class GenericTraitPrerequisiteBuilder implements ITraitPrerequisiteBuilder {

  private final ITraitType type;

  public GenericTraitPrerequisiteBuilder(ITraitType type) {
    this.type = type;
  }

  public IGenericTrait build(Element element) throws PersistenceException {
    int minValue = ElementUtilities.getRequiredIntAttrib(element, ATTRIB_VALUE);
    return new ValuedTraitType(type, minValue);
  }
}