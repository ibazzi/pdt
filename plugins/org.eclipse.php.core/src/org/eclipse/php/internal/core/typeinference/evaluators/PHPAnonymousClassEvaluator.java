package org.eclipse.php.internal.core.typeinference.evaluators;

import org.eclipse.dltk.ti.GoalState;
import org.eclipse.dltk.ti.ISourceModuleContext;
import org.eclipse.dltk.ti.goals.GoalEvaluator;
import org.eclipse.dltk.ti.goals.IGoal;
import org.eclipse.php.core.compiler.ast.nodes.AnonymousClassDeclaration;
import org.eclipse.php.internal.core.typeinference.AnonymousClassInstanceType;

public class PHPAnonymousClassEvaluator extends GoalEvaluator {

	private AnonymousClassInstanceType result;

	public PHPAnonymousClassEvaluator(IGoal goal, AnonymousClassDeclaration declare) {
		super(goal);
		result = new AnonymousClassInstanceType(((ISourceModuleContext) goal.getContext()).getSourceModule(), declare);

	}

	@Override
	public IGoal[] init() {
		return null;
	}

	@Override
	public Object produceResult() {
		return result;
	}

	@Override
	public IGoal[] subGoalDone(IGoal subgoal, Object result, GoalState state) {
		return null;
	}

}
