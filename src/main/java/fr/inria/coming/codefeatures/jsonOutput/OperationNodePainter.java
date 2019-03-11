package fr.inria.coming.codefeatures.jsonOutput;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.github.gumtreediff.actions.model.Action;
import com.github.gumtreediff.tree.ITree;
import com.google.gson.JsonObject;

import fr.inria.coming.codefeatures.jsonOutput.Json4SpoonGenerator.JSON_PROPERTIES;
import gumtree.spoon.diff.operations.Operation;

/**
 * 
 * @author Matias Martinez
 *
 */
public class OperationNodePainter implements NodePainter {

	private Map<ITree, Operation> nodesAffectedByOps = new HashMap<>();

	public OperationNodePainter(List<Operation> operations) {
		// Collect all nodes and get the operator
		for (Operation operation : operations) {
			nodesAffectedByOps.put(operation.getAction().getNode(), operation);
		}
	}

	@Override
	public void paint(ITree tree, JsonObject jsontree) {

		if (nodesAffectedByOps.containsKey(tree)) {

			Operation<Action> operationOverTree = nodesAffectedByOps.get(tree);
			String actionName = operationOverTree.getAction().getName();
			jsontree.addProperty(JSON_PROPERTIES.op.toString(), actionName);

		}
	}

}
