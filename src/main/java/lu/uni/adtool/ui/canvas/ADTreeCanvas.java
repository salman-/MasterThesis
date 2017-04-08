/**
 * Author: Piotr Kordy (piotr.kordy@uni.lu <mailto:piotr.kordy@uni.lu>)
 * Date:   10/12/2015
 * Copyright (c) 2015,2013,2012 University of Luxembourg -- Faculty of Science,
 *     Technology and Communication FSTC
 * All rights reserved.
 * Licensed under GNU Affero General Public License 3.0;
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU Affero General Public License as
 *    published by the Free Software Foundation, either version 3 of the
 *    License, or (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU Affero General Public License for more details.
 *
 *    You should have received a copy of the GNU Affero General Public License
 *    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package lu.uni.adtool.ui.canvas;

import ee.ut.smarttool.DB.AttackDBService;
import ee.ut.smarttool.DB.CountermeasureDBService;
import lu.uni.adtool.tools.Options;
import lu.uni.adtool.tools.undo.AddChild;
import lu.uni.adtool.tools.undo.AddCounter;
import lu.uni.adtool.tools.undo.AddSibling;
import lu.uni.adtool.tools.undo.FromTermsTree;
import lu.uni.adtool.tools.undo.PasteNode;
import lu.uni.adtool.tools.undo.SetLabel;
import lu.uni.adtool.tools.undo.SwitchAttacker;
import lu.uni.adtool.tools.undo.SwitchSibling;
import lu.uni.adtool.tools.undo.ToggleOpAction;
import lu.uni.adtool.tree.ADTNode;
import lu.uni.adtool.tree.ADTParser;
import lu.uni.adtool.tree.GuiNode;
import lu.uni.adtool.tree.Node;
import lu.uni.adtool.tree.NodeTree;
import lu.uni.adtool.ui.MainController;
import lu.uni.adtool.ui.TermView;
import lu.uni.adtool.ui.TreeDockable;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Stack;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JScrollPane;
import lu.uni.adtool.tree.SimpleNode;
import lu.uni.adtool.tree.TreeSchema;
import org.abego.treelayout.util.DefaultConfiguration;

// if Type is null then it is the canvas with the original tree
public class ADTreeCanvas<Type> extends AbstractTreeCanvas {
  public ADTreeCanvas(NodeTree tree, MainController mc) {
      
    super(tree, mc);
    if (null == treeSchema)
        treeSchema = new TreeMap<String,ArrayList<SimpleNode>>();
    this.labelCounter = tree.getLayout().getLabelCounter(LABEL_PREFIX);
    this.listener = new ADTCanvasHandler(this);
    this.addMouseListener(listener);
    this.addMouseMotionListener(listener);
    this.addKeyListener(listener);
    this.configuration = new DefaultConfiguration<Node>(Options.canv_gapBetweenLevels, Options.canv_gapBetweenNodes);
    if (tree != null) {
      this.setFocus(null);
      this.lastFocused = (GuiNode) tree.getRoot(false);
      // create the layout
      this.getSharedExtentProvider().updateTreeSize(tree.getRoot(true));
      this.recalculateLayout();
    }
  }

  /**
   * Constructor used to export tree without showing it in a dockable
   */
  public ADTreeCanvas(NodeTree tree) {
    super(tree);
    treeSchema = new TreeMap<String,ArrayList<SimpleNode>>();
    this.labelCounter = tree.getLayout().getLabelCounter(LABEL_PREFIX);
    this.configuration =
        new DefaultConfiguration<Node>(Options.canv_gapBetweenLevels, Options.canv_gapBetweenNodes);
    if (tree != null) {
      this.setFocus(null);
      this.lastFocused = (GuiNode) tree.getRoot(false);
      // create the layout
      this.getSharedExtentProvider().updateTreeSize(tree.getRoot(true));
      this.recalculateLayout();
    }
  }

  public void paste(Node node) {
    if (this.focused != null && node instanceof ADTNode) {
      ADTNode n = (ADTNode) node;
      if (tree.getLayout().getSwitchRole()) {
        n.toggleRoleRecursive();
      }
      if(tree.addSubtree(this.focused, n)) {
        this.addEditAction(new PasteNode(this.focused, n));
        this.notifyAllTreeChanged();
        terms.updateTerms();
      }
    }
  }

  /**
   * Adds a child or a counter to the node.
   *
   * @param node
 * @param selectedNodeId 
   */
  public void addChild(Node node, String selectedNodeId,String selectedNodeType) {
    Node child =null;
    addEditAction(new AddChild(node));
  //  Node child = new ADTNode(((ADTNode) node).getType());
    if(((ADTNode) node).getType().toString().contains("PRO"))                   //Parent attack child attack
        child = new ADTNode(selectedNodeId,((ADTNode) node).getType(),1);
    else                                                                        //Parent Counter child Counter
        child = new ADTNode(selectedNodeId,((ADTNode) node).getType(),3);
    child.setName(this.getNewLabel());
    child.setParent(selectedNodeId);
    tree.addChild(node, child);

 //   addChild(TreeSchema.keyMaker(node.getId(), selectedNodeType),child.getId(),selectedNodeType);
     ArrayList<SimpleNode> childrenList= new ArrayList<SimpleNode>();
     treeSchema.put(keyMaker(child.getId(),selectedNodeType),childrenList);
     treeSchema.get(keyMaker(node.getId(), selectedNodeType)).add(new SimpleNode(child.getId(),selectedNodeType));
    this.notifyAllTreeChanged();
    terms.updateTerms();
  }
  
  public void addChild(Node node) {
	    addEditAction(new AddChild(node));
	    Node child = new ADTNode(((ADTNode) node).getType());
	    child.setName(this.getNewLabel());
	    tree.addChild(node, child);
	    this.notifyAllTreeChanged();
	    terms.updateTerms();
	  }

  public void addCounter(Node parent,String parentId,String selectedNodeType) {
      
    if (((ADTNode) parent).isCountered()) {
      return;
    }
    addEditAction(new AddCounter(parent));
    Node child=null;
    if(((ADTNode) parent).getType().toString().contains("PRO")){      // Parent is attack
         child = new ADTNode(parentId,((ADTNode) parent).getType(),2);
    }else{                                                           // Parent is countermeasure
        child = new ADTNode(parentId,((ADTNode) parent).getType(),4);
    }    
    tree.addCounter((ADTNode) parent, (ADTNode) child);
    
    ((ADTNode) child).toggleRole();
    child.setName(this.getNewLabel());   
    String type= (selectedNodeType.contains("PRO")) ? "OPP" : "PRO"; 
 //   addChild(TreeSchema.keyMaker(parent.getId(), selectedNodeType),child.getId(),type);
    
     ArrayList<SimpleNode> childrenList= new ArrayList<SimpleNode>();
    treeSchema.put(keyMaker(child.getId(),type),childrenList);
    treeSchema.get(keyMaker(parent.getId(), selectedNodeType)).add(new SimpleNode(child.getId(),selectedNodeType));
    
    this.notifyAllTreeChanged();
    terms.updateTerms();
  }
  public void addCounter(Node parent) {
      
    if (((ADTNode) parent).isCountered()) {
      return;
    }
    addEditAction(new AddCounter(parent));
    Node child = new ADTNode(((ADTNode) parent).getType());
    ((ADTNode) child).toggleRole();
    child.setName(this.getNewLabel());
    tree.addCounter((ADTNode) parent, (ADTNode) child);
    this.notifyAllTreeChanged();
    terms.updateTerms();
  }
  
  public void setTerms(TermView terms) {
    this.terms = terms;
  }

  /**
   * Adds a sibling to a node on a left or right side.
   *
   * @param node
   * @param onLeft
   *          if true we add sibling to the left.
   */
  public void addSibling(Node node, boolean onLeft) {
    addEditAction(new AddSibling(node, onLeft));
    if (node.getParent() != null && ((ADTNode)node.getParent()).getType() == ((ADTNode) node).getType()) {
      ADTNode sibling = new ADTNode(((ADTNode) node).getType());
      sibling.setName(this.getNewLabel());
      tree.addSibling(node, sibling, onLeft);
      this.notifyAllTreeChanged();
      terms.updateTerms();
    }
  }

  /**
   * Gets the label separated into lines
   *
   * @return The array of labels with each line as a separate entry
   */
  public String[] getLabelLines(Node node) {
    return getLabel(node).split("\n");
  }

  /**
   * Returns a text label that is painted for a given node.
   *
   * @param node
   *          label as a text.
   * @return
   */
  public String getLabel(Node node) {
    return node.getName();
  }

  /**
   * Changes the node label.
   *
   * @param node
   *          node for which we change the label.
   * @param label
   *          new label for the node.
   */
  public void setLabel(Node node, String label, String comment) {
    if (node.getName().equals(label) && node.getComment().equals(comment)) {
      return;
    }
    addEditAction(new SetLabel(node, node.getName(), node.getComment(), label, comment));
    tree.setName(node, label);
    node.setComment(comment);
    if (node == tree.getLayout().getRoot()) {
      TreeDockable currentTree = (TreeDockable) this.controller.getControl()
        .getMultipleDockable(TreeDockable.TREE_ID + Integer.toString(this.getTreeId()));
       if (currentTree != null) {
         currentTree.setTitleText("ADTree - " + tree.getLayout().getRoot().getName());
       }
    }

    this.notifyAllTreeChanged();
    if (node.hasDefault()) {
      this.terms.updateTerms();
    }
  }

  public void repaintAll() {
    controller.getFrame().getDomainFactory().repaintAllDomains(this.getTreeId());
    this.repaint();
  }

  public boolean validLabel(String s) {
    if (s == null) {
      return false;
    }
    if (s.length() == 0) {
      return false;
    }
    ADTParser parser = new ADTParser();
    ADTNode root = parser.parseString(s);
    if (root == null) return false;
    return root.isLeaf();
  }

  public void toggleOp(Node node) {
    addEditAction(new ToggleOpAction(node));
    ((ADTNode) node).toggleOp();
    tree.getLayout().recalculateValues();
    this.notifyAllTreeChanged();
    this.terms.updateTerms();
  }


  public void switchSibling(Node node, boolean onLeft) {
    addEditAction(new SwitchSibling(node, onLeft));
    if (node.getParent() != null) {
      GuiNode newPos = null;
      if (onLeft) {
        newPos = ((GuiNode) node).getLeftSibling();
      }
      else {
        newPos = ((GuiNode) node).getRightSibling();
      }
      if (newPos != null && newPos.getParent() != null) {
        tree.switchSibling(node, newPos);
        this.notifyAllTreeChanged();
        this.terms.updateTerms();
      }
    }
  }

  /**
   * Returns the root of a tree associated with this canvas
   */
  public void setRoot(ADTNode root) { // TODO - update all listeners etc
    this.addEditAction(new FromTermsTree(this.tree.getRoot(true)));
    this.tree.setRoot(root);
    this.notifyAllTreeChanged();
    this.terms.updateTerms();
    // this.
    // tree.notifyTreeChanged(TreeChangeListener.Change.NODES_CHANGE);
  }

  /**
   * Returns the root of a tree associated with this canvas
   */
  public ADTNode getRoot() {
    return (ADTNode) tree.getRoot(true);
  }

  public void switchAttacker() {
    addEditAction(new SwitchAttacker());
    tree.getLayout().toggleRole();
    notifyAllTreeChanged();
  }

  public void setScrollPane(JScrollPane pane) {
    this.scrollPane = pane;
    this.scrollPane.addMouseWheelListener(listener);
    this.scrollPane.addComponentListener(listener);
    this.viewPortSize = this.scrollPane.getViewport().getExtentSize();
    this.setScale(1);
  }

  public void updateTerms() {
    terms.updateTerms();
  }

  /**
   * Just return this canvas - placeholder for canvases that want to share edit
   * history - they need to override this method.
   */
  public AbstractTreeCanvas getTreeCanvas() {
    return this;
  }

  protected Color getFillColor(Node node) {
    return Options.canv_FillColorAtt;
  }
  
   public static Stack findChildrenToDelete(SimpleNode parentNode){
        Stack nodesToDelete = new Stack();
        nodesToDelete.add(parentNode);
        String key=keyMaker(parentNode.getId(), parentNode.getType());
        ArrayList<SimpleNode> children=treeSchema.get(key);
        if(children!=null)
            for(int i=0;i<children.size();i++){
                // add recursive children to the result
                nodesToDelete.addAll(findChildrenToDelete( children.get(i)));
            }
        return nodesToDelete;  
    }
    
    public static TreeMap<String, ArrayList<SimpleNode>> deleteNode(TreeMap<String,ArrayList<SimpleNode>> tree,SimpleNode parentNode){
      
      AttackDBService attack=new AttackDBService();
      CountermeasureDBService counter=new CountermeasureDBService();
      Stack nodes = findChildrenToDelete(parentNode);
      while(nodes.size()>0){
          SimpleNode node =(SimpleNode) nodes.peek();
          
          nodes.pop();
        
          System.out.println("ID is: "+node.getId()+" Type: "+node.getType());
         
          try {
             //  int res1=0;
             //   int res2=0;
                if(node.getType().contains("PRO")){
                    
                    tree.remove(keyMaker(node.getId(), node.getType()));
               /*   res1= gdb.deleteParentId("'"+"attack-counterTree"+"'",node.getId());
                  res2= gdb.deleteParentId("'"+"attackTree"+"'",node.getId());
                }else{  
                   res1=gdb.deleteParentId("'"+"counter-attackTree"+"'",node.getId());
                   res2=gdb.deleteParentId("'"+"countermeaureTree"+"'",node.getId());
               */ }
           } catch (Exception ex) {
                  Logger.getLogger(TreeSchema.class.getName()).log(Level.SEVERE, null, ex);
            }

          
      }
      return tree;
    }
    
    public static boolean hasChildren(SimpleNode node){
        try{
            String id= keyMaker(node.getId(),node.getType());
            ArrayList<SimpleNode> children = treeSchema.get(id);
            boolean res = (children==null) ? false :true;
            return res;
        }catch(Exception e){
            return false;
        }
    }
    
    public static String keyMaker(String id,String type){
        return id+"|"+type;
    }
    
    public static void addChild(String parent,String childId,String nodeType){
        ArrayList<SimpleNode> childrenList= new ArrayList<SimpleNode>();
        treeSchema.put(keyMaker(childId,nodeType),childrenList);
        
        treeSchema.get(parent).add(new SimpleNode(childId, nodeType));
       // children.add(new SimpleNode(childId, nodeType)); 
     //   treeSchema.put(parent,children);
      
    }
    public static void addRoot(SimpleNode parent){
       // clearTree();
        String key=keyMaker(parent.getId(),parent.getType());
        ArrayList<SimpleNode> childrenList= new ArrayList<SimpleNode>();
        if (null == treeSchema.get(key))
            treeSchema.put(key, childrenList);  
        
    }

    public static void clearTree(){
        treeSchema.clear();
    }

  public static TreeMap<String,ArrayList<SimpleNode>> treeSchema;//=new TreeMap<String,ArrayList<SimpleNode>>();
  protected ADTCanvasHandler  listener;
  protected TermView          terms;
  private static final long   serialVersionUID = 6626362203605041529L;
}
