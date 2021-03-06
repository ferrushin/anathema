package net.sf.anathema.platform.tree.document.visualizer;

import net.sf.anathema.graph.graph.LayeredGraph;
import net.sf.anathema.graph.nodes.ISimpleNode;
import net.sf.anathema.platform.tree.document.components.ILayer;
import net.sf.anathema.platform.tree.document.components.IVisualizableNode;
import net.sf.anathema.platform.tree.document.components.VisualizableNodeLeftSideComparator;
import net.sf.anathema.platform.tree.document.util.BackwardsIterable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class BottomUpGraphPositioner extends AbstractCascadeVisualizer {

  // BASIC:
  // 1. Build Sugiyama layout
  // 1b. Create Metanodes for nodes who have only common children.
  // (Location conflicts arising from proximity (not identical location) should be resolved by the layer, not
  // the nodes.)
  // 2. Put lowest layer nodes in, left to right
  // 3. Add parent nodes, centered on leaf nodes.
  // 4. Add leaf nodes on the next layer, possibly shifting already placed nodes to the right.
  // Subtree-boundaries should not be broken.
  // 5. Draw Nodes

  // MULTIPLE PARENTS:
  // If a node has multiple parents, they should be placed next to each other
  // (This is implied by the crossing-reduction constraints and should happen automatically)
  // The parents are combined into a single "meta node", which connects to all of the
  // included node's parents and all of their children.
  // During calculation, meta nodes are treated as a single large node.
  // Their position is calculated as usual, with free space for placement
  // of the individual nodes surrounding them.
  // Once calculation is done, the nodes placed symmetrically around the calculated center location in
  // the order determined by the Sugiyama layout.

  // TWO NODES WITH TWO SHARED CHILDREN:
  // In cases such as L1={A,B}, L2={C}, L3={D}, with A=>C, A=>D, B=>C, B=>D, the nodes A and B are combined
  // into a meta node, as above. In addition, C and D are combined into a vertical meta node, which takes
  // up only a single slot, but extends over multiple layers.

  // SHIFTING:
  // When a node is shifted, all nodes to the right should also be shifted by the same amount.
  // Shifts should ALWAYS go right, to make things easier.
  // All children of the shifted node should be moved,too.
  // Maybe results get better (symmetry!) if only those children located to the right of the node are shifted.

  // DRAWING:
  // Once node placement is complete, node drawing occurs.
  // First all nodes are drawn, with empty space left for the dummy nodes.
  // Then, edges are created.
  // Edges between two regular nodes are drawn as ususal.
  // Edges between a regular node and a dummy node are drawn as a polyline.
  // Since dummy nodes always have a single child, it can be assumed that the edge from dummy node
  // to it's child is as straight as possible. Thus:
  // From the parent to the dummy node, a line is drawn.
  // These lines should always attach to the top-center spot of the dummy nodes.
  // Within the dummy node, the line is extended straight downwards, arriving at the bottom-center spot.
  // The line is then drawn to the next child node, either a regular node (edge may arrive anywhere
  // and needs to end in an arrow) or a dummy node (repeat above steps).

  // CHILDGROUPS:
  // For each leaf, calculate all of it's ancestors.
  // If two nodes on the same layer are reachable by traversing the tree from the same leaf,
  // they form a childgroup (often they will also be part of a horizontal metanode) and may be placed as close
  // to each other as possible without overlapping their edges
  // Instead of giving the entire map as argument, just give the relevant leaves?

  public BottomUpGraphPositioner(LayeredGraph graph, ITreePresentationProperties properties) {
    super(properties, graph);
  }

  @Override
  public ILayer[] buildTree() {
    SimplifiedBottomUpGraphVisualizer simplifiedVisualizer = new SimplifiedBottomUpGraphVisualizer(getGraph(),
            getProperties());
    if (simplifiedVisualizer.isApplicable()) {
      return simplifiedVisualizer.buildTree();
    }
    int layerCount = getGraph().getDeepestLayer();
    for (int layerIndex = layerCount - 1; layerIndex >= 0; layerIndex--) {
      createLeafGroups(layerIndex);
    }
    for (int layerIndex = layerCount - 1; layerIndex >= 0; layerIndex--) {
      createVisualizableNodes(layerIndex);
    }
    ILayer[] layers = createLayers(layerCount);
    for (ILayer layer : new BackwardsIterable<>(layers)) {
      layer.positionNodes();
      layer.unrollHorizontalMetanodes();
    }
    createSlimWaistSymmetrie(layers);
    centerSingleParents(layers);
    centerOnlyChildren(layers);
    rectifySingleParentRoots(layers);
    straightenLines(layers);
    separateOverlappingNodes(layers);
    removeWhiteSpace(layers);
    centerTrailingSingleNodePath(layers);
    return layers;
  }

  private void centerTrailingSingleNodePath(ILayer[] layers) {
    int lengthOfTail = getLengthOfTail(layers);
    for (int index = layers.length - lengthOfTail; index < layers.length; index++) {
      centerOnlyChild(layers[index].getNodes()[0]);
    }
  }

  private int getLengthOfTail(ILayer[] layers) {
    int lengthOfTail = 0;
    for (ILayer layer : new BackwardsIterable<>(layers)) {
      if (layer.getNodes().length == 1) {
        lengthOfTail++;
      } else {
        break;
      }
    }
    return lengthOfTail;
  }

  private void rectifySingleParentRoots(ILayer[] layers) {
    ILayer rootLayer = layers[0];
    for (IVisualizableNode node : rootLayer.getNodes()) {
      IVisualizableNode[] children = node.getChildren();
      if (children.length != 1) {
        continue;
      }
      IVisualizableNode[] peers = children[0].getParents();
      if (peers.length != 2) {
        continue;
      }
      for (IVisualizableNode peer : peers) {
        if (peer == node) {
          continue;
        }
        if (peer.getChildren().length != 1) {
          rootLayer.setNodePosition(node, children[0].getPosition());
        }
      }
    }
  }

  private void createSlimWaistSymmetrie(ILayer[] layers) {
    for (int layerIndex = 1; layerIndex < layers.length; layerIndex++) {
      ILayer layer = layers[layerIndex];
      IVisualizableNode[] layerNodes = layer.getNodes();
      if (layerNodes.length == 1) {
        boolean isStraigthLine = true;
        for (int lowerLayerIndex = layerIndex + 1; lowerLayerIndex < layers.length; lowerLayerIndex++) {
          isStraigthLine = isStraigthLine && layers[lowerLayerIndex].getNodes().length == 1;
        }
        if (isStraigthLine) {
          return;
        }
        int maximumWidth = 0;
        for (ILayer otherLayer : layers) {
          maximumWidth = Math.max(maximumWidth, otherLayer.getWidth());
        }
        int imageCenter = maximumWidth / 2;
        layer.getNodes()[0].setPosition(imageCenter);
        for (int lowerLayerIndex = layerIndex + 1; lowerLayerIndex < layers.length; lowerLayerIndex++) {
          shiftLayerWithoutChecking(layers[lowerLayerIndex], imageCenter);
        }
      }
    }
  }

  private void shiftLayerWithoutChecking(ILayer layer, int imageCenter) {
    IVisualizableNode[] layerNodes = layer.getNodes();
    int leftSide = layerNodes[0].getLeftSide();
    int rightSide = layerNodes[layerNodes.length - 1].getRightSide();
    int layerCenter = (rightSide + leftSide) / 2;
    int shift = imageCenter - layerCenter;
    for (IVisualizableNode node : layerNodes) {
      node.setPosition(node.getPosition() + shift);
    }
  }

  private void centerOnlyChildren(ILayer[] layers) {
    for (ILayer layer : layers) {
      if (layer.getPreviousLayer() == null) {
        continue;
      }
      for (IVisualizableNode node : layer.getNodes()) {
        if (node.getChildren().length == 0) {
          centerOnlyChild(node);
        }
      }
    }
  }

  private void centerOnlyChild(IVisualizableNode node) {
    IVisualizableNode[] parents = node.getParents();
    for (IVisualizableNode parent : parents) {
      if (parent.getChildren().length != 1) {
        return;
      }
    }
    Arrays.sort(parents, new VisualizableNodeLeftSideComparator());
    int leftSide = parents[0].getPosition();
    int rightSide = parents[parents.length - 1].getPosition();
    node.getLayer().moveNodeTo(node, (leftSide + rightSide) / 2);
  }

  private void centerSingleParents(ILayer[] layers) {
    for (ILayer layer : layers) {
      for (IVisualizableNode node : layer.getNodes()) {
        if (node.isRootNode() && isSingleParent(node)) {
          layer.positionNode(node);
        }
      }
    }
  }

  private boolean isSingleParent(IVisualizableNode node) {
    boolean singleParent = true;
    for (IVisualizableNode child : node.getChildren()) {
      singleParent = singleParent && child.getParents().length == 1;
    }
    return singleParent;
  }

  private void straightenLines(ILayer[] layers) {
    for (ILayer layer : layers) {
      int treeWidth = new TreeDimensionCalculator(getProperties()).getTreeWidth(layers);
      if (layer.getPreviousLayer() == null) {
        continue;
      }
      for (IVisualizableNode node : new BackwardsIterable<>(layer.getNodes())) {
        IVisualizableNode[] parents = node.getParents();
        IVisualizableNode[] children = node.getChildren();
        if (parents.length == 1 && children.length <= 1) {
          int parentSuggestedShift = parents[0].getPosition() - node.getPosition();
          if (children.length == 1) {
            int childSuggestedShift = children[0].getPosition() - node.getPosition();
            if (Math.signum(parentSuggestedShift) == Math.signum(childSuggestedShift)) {
              straightenLineToParentAndChild(layer, treeWidth, node,
                      Math.min(parentSuggestedShift, childSuggestedShift));
              continue;
            }
          }
          straightenLineToParent(layer, treeWidth, node, parentSuggestedShift);
        }
      }
    }
  }

  private void straightenLineToParentAndChild(ILayer layer, int treeWidth, IVisualizableNode node, int suggestedShift) {
    if (suggestedShift > 0) {
      IVisualizableNode nextNode = layer.getNextNode(node);
      Integer nextNodeLeftSide;
      if (nextNode == null) {
        nextNodeLeftSide = treeWidth + getProperties().getGapDimension().width;
      } else {
        nextNodeLeftSide = nextNode.getLeftSide();
      }
      int possibleShift = Math.min(suggestedShift,
              nextNodeLeftSide - getProperties().getGapDimension().width - node.getRightSide());
      if (possibleShift > 0) {
        node.shiftRight(possibleShift);
      }
    }

    if (suggestedShift < 0) {
      IVisualizableNode previousNode = layer.getPreviousNode(node);
      Integer previousNodeRightSide;
      if (previousNode == null) {
        previousNodeRightSide = -getProperties().getGapDimension().width;
      } else {
        previousNodeRightSide = previousNode.getRightSide();
      }
      int possibleShift = Math.min(Math.abs(suggestedShift),
              node.getLeftSide() - previousNodeRightSide - getProperties().getGapDimension().width);
      if (possibleShift > 0) {
        node.shiftRight(-possibleShift);
      }
    }
  }

  private void straightenLineToParent(ILayer layer, int treeWidth, IVisualizableNode node, int suggestedShift) {
    if (suggestedShift > 0) {
      Integer nextNodeLeftExtreme = layer.getNextNodeLeftExtreme(node);
      if (nextNodeLeftExtreme == null) {
        nextNodeLeftExtreme = treeWidth + getProperties().getGapDimension().width;
      }
      int possibleShift = Math.min(suggestedShift,
              nextNodeLeftExtreme - getProperties().getGapDimension().width - node.getRightSide());
      if (possibleShift > 0) {
        node.shiftRight(possibleShift);
      }
    }

    if (suggestedShift < 0) {
      Integer previousNodeRightExtreme = layer.getPreviousNodeRightExtreme(node);
      if (previousNodeRightExtreme == null) {
        previousNodeRightExtreme = -getProperties().getGapDimension().width;
      }
      int possibleShift = Math.min(Math.abs(suggestedShift),
              node.getLeftSide() - previousNodeRightExtreme - getProperties().getGapDimension().width);
      if (possibleShift > 0) {
        node.shiftRight(-possibleShift);
      }
    }
  }

  private void separateOverlappingNodes(ILayer[] layers) {
    NodeProjection nodeProjection = new NodeProjection(layers);
    for (ILayer layer : layers) {
      IVisualizableNode[] layerNodes = new NodeProjection(layer).getNodes();
      for (int nodeIndex = 0; nodeIndex < layerNodes.length - 1; nodeIndex++) {
        IVisualizableNode node = layerNodes[nodeIndex];
        IVisualizableNode nextNode = layerNodes[nodeIndex + 1];
        int missingSpace = node.getRightSide() + getProperties().getGapDimension().width - nextNode.getLeftSide();
        if (missingSpace > 0) {
          int projectionIndex = nodeProjection.indexOf(nextNode);
          while (nodeProjection.get(projectionIndex - 1).getLeftSide() == nodeProjection.get(
                  projectionIndex).getLeftSide()) {
            projectionIndex--;
          }
          nodeProjection.forceAllRemainingNodesLeft(projectionIndex, -missingSpace);
        }
      }
    }
  }

  private void removeWhiteSpace(ILayer[] layers) {
    NodeProjection nodeProjection = new NodeProjection(layers);
    int leftSide = nodeProjection.get(0).getLeftSide();
    if (leftSide > 0) {
      nodeProjection.forceAllRemainingNodesLeft(0, leftSide);
    }
    for (int nodeIndex = 0; nodeIndex < nodeProjection.size() - 1; nodeIndex++) {
      int distanceToPredecessor = nodeProjection.getDistanceToPredecessors(nodeIndex + 1);
      int whiteSpace = distanceToPredecessor - getProperties().getGapDimension().width;
      if (whiteSpace > 0) {
        nodeProjection.forceAllRemainingNodesLeft(nodeIndex + 1, whiteSpace);
      }
    }
  }

  private void createLeafGroups(int layerIndex) {
    ISimpleNode[] layerNodes = getGraph().getNodesByLayer(layerIndex + 1);
    for (ISimpleNode node : layerNodes) {
      if (!node.isLeafNode()) {
        continue;
      }
      List<ISimpleNode> ancestors = new ArrayList<>();
      ancestors.add(node);
      registerLeafNodeAncestor(node, node);
      for (int upperLayerIndex = layerIndex - 1; upperLayerIndex >= 0; upperLayerIndex--) {
        ISimpleNode[] ancestorLayerNodes = getGraph().getNodesByLayer(upperLayerIndex + 1);
        for (ISimpleNode possibleAncestor : ancestorLayerNodes) {
          if (ancestors.contains(possibleAncestor)) {
            continue;
          }
          List<ISimpleNode> childrenList = Arrays.asList(possibleAncestor.getChildren());
          if (!Collections.disjoint(childrenList, ancestors)) {
            ancestors.add(possibleAncestor);
            registerLeafNodeAncestor(possibleAncestor, node);
          }
        }
      }
    }
  }

  private void createVisualizableNodes(int layerIndex) {
    ISimpleNode[] layerNodes = getGraph().getNodesByLayer(layerIndex + 1);
    for (int nodeIndex = 0; nodeIndex < layerNodes.length; nodeIndex++) {
      ISimpleNode currentNode = layerNodes[nodeIndex];
      if (isVisualizableNodeRegistered(currentNode)) {
        continue;
      }
      boolean horizontalMetanodeFound = lookForHorizontalMetaNodes(layerNodes, nodeIndex, currentNode);
      if (horizontalMetanodeFound) {
        continue;
      }
      if (!isVisualizableNodeRegistered(currentNode)) {
        getNodeFactory().registerVisualizableNode(currentNode);
      }
    }
  }

  private boolean lookForHorizontalMetaNodes(ISimpleNode[] layerNodes, int nodeIndex, ISimpleNode currentNode) {
    Set<ISimpleNode> identicalChildrenNodes = new LinkedHashSet<>();
    identicalChildrenNodes.add(currentNode);
    for (int compareNodeIndex = nodeIndex + 1; compareNodeIndex < layerNodes.length; compareNodeIndex++) {
      ISimpleNode compareNode = layerNodes[compareNodeIndex];
      if (haveIdenticalChildren(currentNode, compareNode)) {
        for (int betweenNodeIndex = nodeIndex + 1; betweenNodeIndex < compareNodeIndex; betweenNodeIndex++) {
          identicalChildrenNodes.add(layerNodes[betweenNodeIndex]);
        }
        identicalChildrenNodes.add(compareNode);
      }
    }
    if (identicalChildrenNodes.size() > 1) {
      getNodeFactory().registerHorizontalMetaNode(identicalChildrenNodes);
      return true;
    }
    return false;
  }

  private boolean haveIdenticalChildren(ISimpleNode node1, ISimpleNode node2) {
    if (node1.isLeafNode() || node2.isLeafNode()) {
      return false;
    }
    List<IVisualizableNode> firstVisualizableChildren = new ArrayList<>();
    for (ISimpleNode node : node1.getChildren()) {
      firstVisualizableChildren.add(getVisualizableNode(node));
    }
    List<IVisualizableNode> secondVisualizableChildren = new ArrayList<>();
    for (ISimpleNode node : node2.getChildren()) {
      secondVisualizableChildren.add(getVisualizableNode(node));
    }
    return firstVisualizableChildren.containsAll(
            secondVisualizableChildren) && firstVisualizableChildren.size() == secondVisualizableChildren.size();
  }
}