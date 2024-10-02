package org.palladiosimulator.analyzer.slingshot.cost.events;

import org.palladiosimulator.analyzer.slingshot.common.events.AbstractSimulationEvent;
import org.palladiosimulator.pcm.resourceenvironment.ResourceContainer;

public class TakeCostMeasurement extends AbstractSimulationEvent {

	private final ResourceContainer target;
	private final double interval;

	public double getInterval() {
		return interval;
	}

	public TakeCostMeasurement(final double delay, final ResourceContainer target, final double interval) {
		super(delay);
		this.target = target;
		this.interval = interval;
	}

	public TakeCostMeasurement(final TakeCostMeasurement intervalPassed) {
		this(intervalPassed.getInterval(), intervalPassed.getTargetResourceContainer(), intervalPassed.getInterval());
	}

	public ResourceContainer getTargetResourceContainer() {
		return this.target;
	}
}
