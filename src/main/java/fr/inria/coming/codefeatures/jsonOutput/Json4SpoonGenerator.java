package fr.inria.coming.codefeatures.jsonOutput;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.github.gumtreediff.tree.ITree;
import com.github.gumtreediff.tree.TreeContext;
import com.github.gumtreediff.utils.Pair;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import gumtree.spoon.builder.SpoonGumTreeBuilder;
//import fr.inria.coming.codefeatures.jsonOutput.NodePainter;
//import fr.inria.coming.codefeatures.jsonOutput.OperationNodePainter;
import gumtree.spoon.diff.operations.Operation;
import spoon.reflect.declaration.CtElement;

/**
 *
 * Creates a JSON representation of a Spoon Java abstract syntaxt tree.
 *
 * @author Matias Martinez
 *
 */
public class Json4SpoonGenerator {

	public enum JSON_PROPERTIES {
		label, type, op, children, parents_src, parents_dst, location_src, location_dst, upd_to, parent_ids, parent_names
	};

	@SuppressWarnings("rawtypes")
	public JsonObject getJSONasJsonObject(CtElement element) {
		SpoonGumTreeBuilder builder = new SpoonGumTreeBuilder();
		ITree generatedTree = builder.getTree(element);

		TreeContext tcontext = builder.getTreeContext();
		return this.getJSONasJsonObject(tcontext, generatedTree);
	}

	public String getJSONasString(CtElement element) {
		SpoonGumTreeBuilder builder = new SpoonGumTreeBuilder();
		ITree generatedTree = builder.getTree(element);

		TreeContext tcontext = builder.getTreeContext();

		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		return gson.toJson(this.getJSONasJsonObject(tcontext, generatedTree)) + "\n";
	}

	public JsonObject getJSONasJsonObject(TreeContext context, ITree tree) {
		JsonObject o = new JsonObject();
		o.addProperty(JSON_PROPERTIES.label.toString(), tree.getLabel());
		o.addProperty(JSON_PROPERTIES.type.toString(), context.getTypeLabel(tree));

		JsonArray nodeChildens = new JsonArray();
		o.add(JSON_PROPERTIES.children.toString(), nodeChildens);

		for (ITree tch : tree.getChildren()) {
			JsonObject childJSon = getJSONasJsonObject(context, tch);
			if (childJSon != null)
				nodeChildens.add(childJSon);
		}
		return o;

	}

	/**
	 * Decorates a node with the affected operator, if any.
	 * 
	 * @param context
	 * @param tree
	 * @param operations
	 * @return
	 */
	public JsonObject getJSONwithOperations(TreeContext context, ITree tree, List<Operation> operations) {

		OperationNodePainter opNodePainter = new OperationNodePainter(operations);
		Collection<NodePainter> painters = new ArrayList<NodePainter>();
		painters.add(opNodePainter);
		JsonObject o = getJSONwithCustorLabels(context, tree, painters);
		String actionName = o.get(JSON_PROPERTIES.op.toString()).getAsString();

		CtElement srcTree = (CtElement) tree.getMetadata("spoon_object");
		JsonObject parentsNamesAndIdsSrc = analyzeParentTypes(srcTree);
		JsonArray srcLoc = new JsonArray();
		if ("(unknown file)".equals(srcTree.getPosition().toString())){
			srcLoc.add("None");
			srcLoc.add("None");
		}
		else {
			srcLoc.add(srcTree.getPosition().getSourceStart());
			srcLoc.add(srcTree.getPosition().getSourceEnd());
		}
		if (actionName == "DEL"){
			o.add(JSON_PROPERTIES.location_src.toString(), srcLoc);
			o.add(JSON_PROPERTIES.parents_src.toString(), parentsNamesAndIdsSrc);
		}
		else if (actionName == "MOV"){
			CtElement dstTree = (CtElement) tree.getMetadata("spoon_object_dest");
			JsonObject parentsNamesAndIdsDst = analyzeParentTypes(dstTree);
			JsonArray dstLoc = new JsonArray();
			if ("(unknown file)".equals(dstTree.getPosition().toString())){
				dstLoc.add("None");
				dstLoc.add("None");
			}
			else {
				dstLoc.add(dstTree.getPosition().getSourceStart());
				dstLoc.add(dstTree.getPosition().getSourceEnd());
			}
			o.add(JSON_PROPERTIES.location_src.toString(), srcLoc);
			o.add(JSON_PROPERTIES.location_dst.toString(), dstLoc);
			o.add(JSON_PROPERTIES.parents_src.toString(), parentsNamesAndIdsSrc);
			o.add(JSON_PROPERTIES.parents_dst.toString(), parentsNamesAndIdsDst);

		}
		else if (actionName == "INS"){
			o.add(JSON_PROPERTIES.location_dst.toString(), srcLoc);
			o.add(JSON_PROPERTIES.parents_dst.toString(), parentsNamesAndIdsSrc);
		}
		else if (actionName == "UPD"){
			CtElement dstTree = (CtElement) tree.getMetadata("spoon_object_dest");
			JsonArray dstLoc = new JsonArray();
			if ("(unknown file)".equals(dstTree.getPosition().toString())){
				dstLoc.add("None");
				dstLoc.add("None");
			}
			else {
				dstLoc.add(dstTree.getPosition().getSourceStart());
				dstLoc.add(dstTree.getPosition().getSourceEnd());
			}
			o.add(JSON_PROPERTIES.location_src.toString(), srcLoc);
			o.add(JSON_PROPERTIES.location_dst.toString(), dstLoc);
			o.add(JSON_PROPERTIES.parents_dst.toString(), parentsNamesAndIdsSrc);

			ITree dstNode = (ITree) dstTree.getMetadata("gtnode");
			if (dstNode != null){
				o.addProperty(JSON_PROPERTIES.upd_to.toString(), dstNode.getLabel());
			}
				else {
				o.addProperty(JSON_PROPERTIES.upd_to.toString(),dstTree.toString());
			}
		}
		else {
			throw new RuntimeException("invalid operation: " + actionName);
		}
		return o;
	}


	private JsonObject analyzeParentTypes(CtElement element) {
		CtElement parent = element.getParent();
		JsonArray parentNames = new JsonArray();
		JsonArray parentIds = new JsonArray();
		JsonObject parents = new JsonObject();
		try {
			do {
				int id;
				String name = parent.getClass().getSimpleName();
				if ("CtPackageImpl".equals(name)){
					break;
				}
				parentNames.add(name);
				if (parent.getMetadata("gtnode") != null) {
					id = ((ITree) parent.getMetadata("gtnode")).getId();
				}
				else {
					id = -1;
				}
				parentIds.add(id);
				parent = parent.getParent();
			} while (parent != null);
		} catch (Exception e) {
		}

		parents.add(JSON_PROPERTIES.parent_ids.toString(), parentIds);
		parents.add(JSON_PROPERTIES.parent_names.toString(), parentNames);
		return parents;

	}
	@SuppressWarnings("unused")
	public JsonObject getJSONwithCustorLabels(TreeContext context, ITree tree, Collection<NodePainter> nodePainters) {

		JsonObject o = new JsonObject();
		o.addProperty(JSON_PROPERTIES.label.toString(), tree.getLabel());
		o.addProperty(JSON_PROPERTIES.type.toString(), context.getTypeLabel(tree));
		for (NodePainter nodePainter : nodePainters) {
			nodePainter.paint(tree, o);
		}

		JsonArray nodeChildens = new JsonArray();
		o.add(JSON_PROPERTIES.children.toString(), nodeChildens);

		for (ITree tch : tree.getChildren()) {
			JsonObject childJSon = getJSONwithCustorLabels(context, tch, nodePainters);
			if (childJSon != null)
				nodeChildens.add(childJSon);
		}
		return o;

	}

}
