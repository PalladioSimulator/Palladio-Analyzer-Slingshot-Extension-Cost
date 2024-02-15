package org.palladiosimulator.analyzer.slingshot.cost.events;

import org.palladiosimulator.analyzer.slingshot.common.events.AbstractSimulationEvent;
import org.palladiosimulator.pcm.resourceenvironment.ResourceContainer;

public class IntervalPassed extends AbstractSimulationEvent {

	private final ResourceContainer target;

	public IntervalPassed(final ResourceContainer target, final double delay) {
		super(delay);
		this.target = target;
	}

	public IntervalPassed(final IntervalPassed intervalPassed) {
		this(intervalPassed.getTargetResourceContainer(), intervalPassed.delay());
	}

	public ResourceContainer getTargetResourceContainer() {
		return this.target;
	}
}
